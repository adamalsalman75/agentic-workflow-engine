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
import java.util.stream.Collectors;

@Component
public class TaskPlanAgent {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPlanAgent.class);
    
    private final ResilientChatClient resilientChatClient;
    
    public TaskPlanAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    
    public TaskPlan createTaskPlanWithDependencies(String userGoal) {
        String prompt = """
            Break down this goal into 3-6 specific, actionable tasks with clear dependencies.
            
            Goal: %s
            
            CRITICAL: Tasks should have logical dependencies where one task's output is needed by another.
            
            Examples of dependencies:
            - "Research market" must complete before "Create business plan" (blocking)
            - "Design logo" must complete before "Create marketing materials" (blocking)
            - "Set up legal structure" can inform "Open business bank account" but doesn't block it (informational)
            
            Format EXACTLY as shown:
            TASKS:
            1. [First task description]
            2. [Second task description]
            3. [Third task description]
            4. [Fourth task description]
            
            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - needs market research data to create plan
            Task 3 depends on Task 1 (informational) - market insights help with branding
            Task 4 depends on Task 2 (blocking) - business plan needed for permits
            
            Types: 
            - blocking (must complete first task before starting second)
            - informational (first task output helps but doesn't block second task)
            
            Think carefully about what each task produces and what other tasks need as input.
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
            
            IMPORTANT: Only modify tasks if the completed task reveals new information that fundamentally changes what needs to be done.
            Most of the time, the original plan should remain unchanged.
            
            Should remaining tasks change? Respond "NO_CHANGES" or list updated tasks (one per line).
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