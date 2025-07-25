package dev.alsalman.agenticworkflowengine.agent;

import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import dev.alsalman.agenticworkflowengine.domain.DependencyType;
import org.springframework.stereotype.Component;
import dev.alsalman.agenticworkflowengine.service.ResilientChatClient;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TaskPlanAgent {
    
    private final ResilientChatClient resilientChatClient;
    
    public TaskPlanAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    public List<Task> createTaskPlan(String userGoal) {
        TaskPlan taskPlan = createTaskPlanWithDependencies(userGoal);
        return taskPlan.tasks();
    }
    
    public TaskPlan createTaskPlanWithDependencies(String userGoal) {
        String prompt = """
            Break down this goal into 3-6 specific, actionable tasks with dependencies.
            
            Goal: %s
            
            Format:
            TASKS:
            1. Task description
            2. Another task
            
            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - reason
            
            Types: blocking (must wait), informational (can start early)
            """.formatted(userGoal);
            
        String response = resilientChatClient.call("task planning", prompt);
        return parseTaskPlanResponse(response);
    }
    
    private TaskPlan parseTaskPlanResponse(String response) {
        String[] sections = response.split("DEPENDENCIES:");
        String tasksSection = sections[0].replace("TASKS:", "").trim();
        String dependenciesSection = sections.length > 1 ? sections[1].trim() : "";
        
        // Parse tasks
        List<Task> tasks = Arrays.stream(tasksSection.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("TASKS"))
            .map(line -> line.replaceAll("^\\d+\\.\\s*", "")) // Remove numbering
            .filter(line -> !line.isEmpty())
            .map(Task::create)
            .toList();
            
        // Create task index map for dependency resolution
        Map<Integer, UUID> taskIndexMap = tasks.stream()
            .collect(Collectors.toMap(
                task -> tasks.indexOf(task) + 1, // 1-based indexing
                Task::id
            ));
            
        // Parse dependencies
        List<TaskDependency> dependencies = new ArrayList<>();
        if (!dependenciesSection.isEmpty()) {
            Arrays.stream(dependenciesSection.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .forEach(line -> {
                    try {
                        TaskDependency dependency = parseDependencyLine(line, taskIndexMap);
                        if (dependency != null) {
                            dependencies.add(dependency);
                        }
                    } catch (Exception e) {
                        // Log but don't fail - continue with other dependencies
                        System.err.println("Failed to parse dependency: " + line);
                    }
                });
        }
        
        // Update tasks with their dependencies
        List<Task> updatedTasks = tasks.stream()
            .map(task -> updateTaskWithDependencies(task, dependencies))
            .toList();
            
        return TaskPlan.of(updatedTasks, dependencies);
    }
    
    private TaskDependency parseDependencyLine(String line, Map<Integer, UUID> taskIndexMap) {
        // Parse format: "Task X depends on Task Y (type) - reason"
        if (!line.contains("depends on")) {
            return null;
        }
        
        String[] parts = line.split("depends on");
        if (parts.length != 2) {
            return null;
        }
        
        // Extract task numbers
        int taskNum = extractTaskNumber(parts[0]);
        int dependsOnNum = extractTaskNumber(parts[1]);
        
        if (taskNum == -1 || dependsOnNum == -1) {
            return null;
        }
        
        UUID taskId = taskIndexMap.get(taskNum);
        UUID dependsOnId = taskIndexMap.get(dependsOnNum);
        
        if (taskId == null || dependsOnId == null) {
            return null;
        }
        
        // Extract type and reason
        String typeAndReason = parts[1].substring(parts[1].indexOf('('));
        boolean isBlocking = typeAndReason.contains("blocking");
        String reason = typeAndReason.substring(typeAndReason.indexOf('-') + 1).trim();
        
        return isBlocking ? 
            TaskDependency.blocking(taskId, dependsOnId, reason) :
            TaskDependency.informational(taskId, dependsOnId, reason);
    }
    
    private int extractTaskNumber(String text) {
        try {
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.matches("\\d+")) {
                    return Integer.parseInt(word);
                }
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        return -1;
    }
    
    private Task updateTaskWithDependencies(Task task, List<TaskDependency> dependencies) {
        List<UUID> blockingDeps = dependencies.stream()
            .filter(dep -> dep.taskId().equals(task.id()) && dep.type() == DependencyType.BLOCKING)
            .map(TaskDependency::dependsOnTaskId)
            .toList();
            
        List<UUID> informationalDeps = dependencies.stream()
            .filter(dep -> dep.taskId().equals(task.id()) && dep.type() == DependencyType.INFORMATIONAL)
            .map(TaskDependency::dependsOnTaskId)
            .toList();
            
        return Task.create(task.description(), blockingDeps, informationalDeps);
    }
    
    public List<Task> reviewAndUpdatePlan(List<Task> currentTasks, Task completedTask) {
        String completedCount = String.valueOf(currentTasks.stream()
            .filter(task -> task.status().name().equals("COMPLETED"))
            .count());
            
        String pendingTasksInfo = currentTasks.stream()
            .filter(task -> task.status().name().equals("PENDING"))
            .map(task -> "- " + task.description())
            .limit(5)
            .reduce("", (acc, task) -> acc + task + "\n");
            
        String prompt = """
            Task completed: %s
            Result: %s
            Completed: %s tasks
            
            Remaining tasks:
            %s
            
            Should remaining tasks change? Respond "NO_CHANGES" or list updated tasks (one per line).
            """.formatted(
                limitText(completedTask.description(), 100),
                limitText(completedTask.result(), 200),
                completedCount,
                pendingTasksInfo
            );
            
        String response = resilientChatClient.call("plan review", prompt);
            
        if ("NO_CHANGES".equals(response.trim())) {
            return currentTasks;
        }
        
        List<Task> completedTasks = currentTasks.stream()
            .filter(task -> !task.status().name().equals("PENDING"))
            .toList();
            
        List<Task> updatedPendingTasks = Arrays.stream(response.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .map(Task::create)
            .toList();
            
        return List.of(completedTasks, updatedPendingTasks)
            .stream()
            .flatMap(List::stream)
            .toList();
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}