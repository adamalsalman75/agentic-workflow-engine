package dev.alsalman.agenticworkflowengine.agent;

import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import dev.alsalman.agenticworkflowengine.domain.DependencyType;
import org.springframework.stereotype.Component;
import dev.alsalman.agenticworkflowengine.service.ResilientChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Component
public class TaskPlanAgent {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPlanAgent.class);
    
    private final ResilientChatClient resilientChatClient;
    
    public TaskPlanAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    
    public TaskPlan createTaskPlanWithDependencies(String userGoal) {
        String prompt = """
            Break down the following goal into 3-6 specific, actionable tasks. For each task, identify any other tasks that it depends on.

            Goal: %s

            Key principles:
            - Efficiency: If tasks are independent and can be done in parallel, do not create a dependency.
            - Logical Flow: Only create a dependency if one task's output is strictly required to start another.
            - No Forced Dependencies: If a goal can be broken into completely independent tasks, it is acceptable to have no dependencies.

            Examples of dependencies:
            - "Research market" must complete before "Create business plan" (blocking)
            - "Design logo" must complete before "Create marketing materials" (blocking)
            - "Set up legal structure" can inform "Open business bank account" but doesn't block it (informational)

            Format EXACTLY as shown:
            TASKS:
            1. [First task description]
            2. [Second task description]
            ...

            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - needs market research data to create plan
            Task 3 depends on Task 1 (informational) - market insights help with branding
            ...
            """.formatted(userGoal);
            
        String response = resilientChatClient.call("task planning", prompt);
        return parseTaskPlanResponse(response);
    }
    
    private TaskPlan parseTaskPlanResponse(String response) {
        log.debug("Parsing task plan response:\n{}", response);
        
        String[] sections = response.split("DEPENDENCIES:");
        String tasksSection = sections[0].replace("TASKS:", "").trim();
        String dependenciesSection = sections.length > 1 ? sections[1].trim() : "";
        
        log.debug("Tasks section: {}", tasksSection);
        log.debug("Dependencies section: {}", dependenciesSection);
        
        // Parse tasks
        List<Task> tasks = Arrays.stream(tasksSection.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith("TASKS"))
            .map(line -> line.replaceAll("^\\d+\\.\\s*", "")) // Remove numbering
            .filter(line -> !line.isEmpty())
            .map(Task::create)
            .toList();
            
        log.info("Parsed {} tasks", tasks.size());
            
        // Create task index map for dependency resolution
        Map<Integer, UUID> taskIndexMap = new HashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            UUID taskId = tasks.get(i).id();
            taskIndexMap.put(i + 1, taskId); // 1-based indexing
            log.debug("Task index mapping: Task {} -> UUID {}", i + 1, taskId);
        }
            
        // Parse dependencies
        List<TaskDependency> dependencies = new ArrayList<>();
        if (!dependenciesSection.isEmpty()) {
            Arrays.stream(dependenciesSection.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .forEach(line -> {
                    try {
                        log.debug("Parsing dependency line: {}", line);
                        TaskDependency dependency = parseDependencyLine(line, taskIndexMap);
                        if (dependency != null) {
                            dependencies.add(dependency);
                            log.debug("Successfully parsed dependency: {} -> {}", 
                                     dependency.taskId(), dependency.dependsOnTaskId());
                        } else {
                            log.warn("Failed to parse dependency line (returned null): {}", line);
                        }
                    } catch (Exception e) {
                        log.error("Exception parsing dependency line '{}': {}", line, e.getMessage());
                    }
                });
        } else {
            log.warn("No dependencies section found in response");
        }
        
        log.info("Parsed {} dependencies", dependencies.size());
        
        // Update tasks with their dependencies
        List<Task> updatedTasks = tasks.stream()
            .map(task -> updateTaskWithDependencies(task, dependencies))
            .toList();
            
        return TaskPlan.of(updatedTasks, dependencies);
    }
    
    private TaskDependency parseDependencyLine(String line, Map<Integer, UUID> taskIndexMap) {
        log.debug("Parsing dependency line: '{}'", line);
        
        // Parse format: "Task X depends on Task Y (type) - reason"
        if (!line.contains("depends on")) {
            log.debug("Line does not contain 'depends on': {}", line);
            return null;
        }
        
        String[] parts = line.split("depends on");
        if (parts.length != 2) {
            log.warn("Invalid dependency format - expected 2 parts separated by 'depends on', got {}: {}", 
                     parts.length, line);
            return null;
        }
        
        // Extract task numbers
        int taskNum = extractTaskNumber(parts[0]);
        int dependsOnNum = extractTaskNumber(parts[1]);
        
        if (taskNum == -1) {
            log.warn("Could not extract task number from first part '{}' in line: {}", parts[0], line);
            return null;
        }
        if (dependsOnNum == -1) {
            log.warn("Could not extract dependency task number from second part '{}' in line: {}", parts[1], line);
            return null;
        }
        
        UUID taskId = taskIndexMap.get(taskNum);
        UUID dependsOnId = taskIndexMap.get(dependsOnNum);
        
        if (taskId == null) {
            log.warn("Task number {} not found in task index map for line: {}", taskNum, line);
            return null;
        }
        if (dependsOnId == null) {
            log.warn("Dependency task number {} not found in task index map for line: {}", dependsOnNum, line);
            return null;
        }
        
        // Extract type and reason with better error handling
        String secondPart = parts[1];
        boolean isBlocking = false;
        String reason = "dependency relationship";
        
        try {
            // Look for (blocking) or (informational) pattern
            if (secondPart.contains("(")) {
                int parenStart = secondPart.indexOf('(');
                int parenEnd = secondPart.indexOf(')', parenStart);
                
                if (parenEnd > parenStart) {
                    String typeSection = secondPart.substring(parenStart + 1, parenEnd).toLowerCase().trim();
                    isBlocking = typeSection.contains("blocking");
                    
                    // Extract reason after the dash
                    int dashIndex = secondPart.indexOf('-', parenEnd);
                    if (dashIndex != -1 && dashIndex + 1 < secondPart.length()) {
                        reason = secondPart.substring(dashIndex + 1).trim();
                    }
                } else {
                    log.warn("Malformed parentheses in dependency line: {}", line);
                    // Default to blocking if we can't parse type
                    isBlocking = true;
                }
            } else {
                log.warn("No dependency type found in parentheses, defaulting to blocking: {}", line);
                isBlocking = true;
            }
        } catch (Exception e) {
            log.warn("Error parsing dependency type and reason from '{}': {}", secondPart, e.getMessage());
            // Default to blocking with generic reason
            isBlocking = true;
        }
        
        TaskDependency dependency = isBlocking ? 
            TaskDependency.blocking(taskId, dependsOnId, reason) :
            TaskDependency.informational(taskId, dependsOnId, reason);
            
        log.debug("Successfully parsed dependency: Task {} {} depends on Task {} ({})", 
                 taskNum, isBlocking ? "BLOCKING" : "INFORMATIONAL", dependsOnNum, reason);
        
        return dependency;
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
            
        // CRITICAL: Preserve the original task UUID, don't create a new one
        return new Task(
            task.id(), // Preserve original UUID
            task.description(),
            task.result(),
            task.status(),
            blockingDeps,
            informationalDeps,
            task.createdAt(),
            task.completedAt()
        );
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
            A task has been completed. Review the results and determine if the remaining plan needs to be updated.

            Completed Task: %s
            Result: %s
            Number of Tasks Completed: %s

            Remaining Tasks:
            %s

            Guiding Principles:
            - Maintain Stability: The original plan should be followed unless new information makes a change necessary. Do not add, remove, or alter tasks unless the completed task's result makes the existing plan inefficient or obsolete.
            - High-Impact Changes Only: Only modify the plan if the completed task reveals a critical new piece of information. For example, if a research task reveals that a key assumption in the plan is wrong.

            Based on this, should the remaining tasks be changed?
            Respond with "NO_CHANGES" if the plan is still valid. Otherwise, provide a new, complete list of the remaining tasks.
            """.formatted(
                limitText(completedTask.description(), 100),
                limitText(completedTask.result(), 200),
                completedCount,
                pendingTasksInfo
            );
            
        String response = resilientChatClient.call("plan review", prompt);
        
        log.debug("Plan review response: {}", response);
            
        if ("NO_CHANGES".equals(response.trim()) || response.trim().toLowerCase().contains("no changes")) {
            log.debug("Plan review: NO_CHANGES - preserving existing tasks with dependencies");
            return currentTasks;
        }
        
        // If changes are needed, preserve existing tasks to maintain dependencies
        // Only update descriptions, don't create completely new tasks
        log.warn("Plan review requested changes - this will remove task dependencies. Consider preserving existing plan.");
        
        // For now, return unchanged tasks to preserve dependencies
        // TODO: Implement proper task update that preserves dependencies
        return currentTasks;
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}