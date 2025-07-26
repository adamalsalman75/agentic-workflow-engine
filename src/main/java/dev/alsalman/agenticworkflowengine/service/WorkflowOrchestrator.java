package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.agent.GoalAgent;
import dev.alsalman.agenticworkflowengine.agent.TaskAgent;
import dev.alsalman.agenticworkflowengine.agent.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;
import java.util.ArrayList;

@Service
public class WorkflowOrchestrator {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrator.class);
    
    private final TaskPlanAgent taskPlanAgent;
    private final TaskAgent taskAgent;
    private final GoalAgent goalAgent;
    private final DependencyResolver dependencyResolver;
    private final WorkflowPersistenceService persistenceService;
    
    public WorkflowOrchestrator(TaskPlanAgent taskPlanAgent, TaskAgent taskAgent, GoalAgent goalAgent, 
                              DependencyResolver dependencyResolver, WorkflowPersistenceService persistenceService) {
        this.taskPlanAgent = taskPlanAgent;
        this.taskAgent = taskAgent;
        this.goalAgent = goalAgent;
        this.dependencyResolver = dependencyResolver;
        this.persistenceService = persistenceService;
    }
    
    
    public WorkflowResult executeWorkflow(String userQuery, UUID goalId) {
        Instant startTime = Instant.now();
        log.info("Starting async workflow execution for query: '{}' with goal ID: {}", userQuery, goalId);
        
        try {
            // Load existing goal from database
            Goal goal = persistenceService.findGoalById(goalId);
            if (goal == null) {
                throw new IllegalArgumentException("Goal not found with ID: " + goalId);
            }
            
            return executeWorkflowWithGoal(userQuery, goal, startTime);
        } catch (Exception e) {
            log.error("Async workflow execution failed for query: '{}' with goal ID: {}", userQuery, goalId, e);
            // Try to update goal status to failed
            try {
                Goal goal = persistenceService.findGoalById(goalId);
                if (goal != null) {
                    goal = goal.withStatus(dev.alsalman.agenticworkflowengine.domain.GoalStatus.FAILED);
                    persistenceService.saveGoal(goal);
                    return WorkflowResult.failure(goal, startTime);
                }
            } catch (Exception ex) {
                log.error("Failed to update goal status to failed", ex);
            }
            
            Goal failedGoal = Goal.create(userQuery).withStatus(dev.alsalman.agenticworkflowengine.domain.GoalStatus.FAILED);
            return WorkflowResult.failure(failedGoal, startTime);
        }
    }
    
    private WorkflowResult executeWorkflowWithGoal(String userQuery, Goal goal, Instant startTime) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Create initial task plan
            log.info("Creating initial task plan asynchronously...");
            var planningTask = scope.fork(() -> taskPlanAgent.createTaskPlan(userQuery));
            
            scope.join();
            scope.throwIfFailed();
            
            List<Task> initialTasks = planningTask.get();
            goal = goal.withTasks(initialTasks);
            log.info("Created {} initial tasks", initialTasks.size());
            initialTasks.forEach(task -> log.debug("Task: {}", task.description()));
            
            // Persist tasks immediately after planning with PENDING status and update with database IDs
            log.debug("Persisting {} tasks with PENDING status after planning", initialTasks.size());
            List<Task> persistedTasks = new ArrayList<>();
            for (Task task : initialTasks) {
                Task savedTask = persistenceService.saveTask(task, goal.id());
                persistedTasks.add(savedTask);
            }
            
            // Update goal with tasks that have database IDs
            goal = goal.withTasks(persistedTasks);
            
            // Execute tasks using dependency-aware parallel execution
            List<Task> currentTasks = new ArrayList<>(goal.tasks());
            log.info("Starting dependency-aware task execution phase...");
            
            // Validate dependencies and clean up invalid ones
            List<String> validationErrors = dependencyResolver.validateDependencies(currentTasks);
            if (!validationErrors.isEmpty()) {
                log.warn("Dependency validation errors found: {}", validationErrors.size());
                log.warn("Removing invalid dependencies and continuing with independent task execution");
                
                // Remove invalid dependencies from all tasks
                currentTasks = currentTasks.stream()
                    .map(this::cleanInvalidDependencies)
                    .toList();
            }
            
            // Check for circular dependencies after cleanup
            if (dependencyResolver.hasCircularDependencies(currentTasks)) {
                log.warn("Circular dependencies detected, removing all dependencies and executing sequentially");
                currentTasks = currentTasks.stream()
                    .map(task -> new Task(
                        task.id(), // Preserve the existing ID
                        task.description(),
                        task.result(),
                        task.status(),
                        List.of(), // Remove all blocking dependencies
                        List.of(), // Remove all informational dependencies
                        task.createdAt(),
                        task.completedAt()
                    ))
                    .toList();
            }
            
            currentTasks = executeTasksWithDependencies(currentTasks, userQuery, goal.id());
            
            // Update goal and generate summary
            goal = goal.withTasks(currentTasks);
            log.info("Async task execution completed. Generating final summary...");
            
            try (var summaryScope = new StructuredTaskScope.ShutdownOnFailure()) {
                final Goal finalGoal = goal;
                var summaryTask = summaryScope.fork(() -> goalAgent.summarizeGoalCompletion(finalGoal));
                
                summaryScope.join();
                summaryScope.throwIfFailed();
                
                goal = summaryTask.get();
                
                // Save final goal with summary to database
                persistenceService.saveGoal(goal);
            }
            
            log.info("Async workflow execution completed successfully in {} ms", 
                java.time.Duration.between(startTime, Instant.now()).toMillis());
            return WorkflowResult.success(goal, startTime);
            
        } catch (Exception e) {
            log.error("Async workflow execution failed for query: '{}'", userQuery, e);
            Goal failedGoal = goal.withStatus(dev.alsalman.agenticworkflowengine.domain.GoalStatus.FAILED);
            
            // Save failed goal to database
            persistenceService.saveGoal(failedGoal);
            return WorkflowResult.failure(failedGoal, startTime);
        }
    }
    
    private List<Task> executeTasksWithDependencies(List<Task> tasks, String userQuery, UUID goalId) {
        List<Task> allTasks = new ArrayList<>(tasks);
        
        while (true) {
            // Get tasks that are ready to execute (dependencies satisfied)
            List<Task> executableTasks = dependencyResolver.getExecutableTasks(allTasks);
            
            if (executableTasks.isEmpty()) {
                // No more executable tasks - we're done or stuck
                boolean hasCompletedTasks = allTasks.stream().anyMatch(task -> task.status() == TaskStatus.COMPLETED);
                boolean hasPendingTasks = allTasks.stream().anyMatch(task -> task.status() == TaskStatus.PENDING);
                
                if (hasPendingTasks && hasCompletedTasks) {
                    log.warn("Workflow stuck - pending tasks exist but none are executable");
                }
                break;
            }
            
            log.info("Found {} executable tasks for parallel execution", executableTasks.size());
            
            // Execute all executable tasks in parallel
            List<Task> completedBatch = executeTasksInParallel(executableTasks, userQuery, allTasks);
            
            // Update the task list with completed tasks and save to database
            for (Task completedTask : completedBatch) {
                // Save completed task to database
                persistenceService.saveTask(completedTask, goalId);
                
                for (int i = 0; i < allTasks.size(); i++) {
                    if (allTasks.get(i).id().equals(completedTask.id())) {
                        allTasks.set(i, completedTask);
                        break;
                    }
                }
                
                // Review and potentially update plan after each completed task
                if (completedTask.status() == TaskStatus.COMPLETED) {
                    log.debug("Starting plan review after completing task: '{}'", 
                             limitText(completedTask.description(), 80));
                    
                    List<Task> updatedTasks = reviewPlanAfterTask(allTasks, completedTask);
                    
                    if (!updatedTasks.equals(allTasks)) {
                        int originalCount = allTasks.size();
                        int newCount = updatedTasks.size();
                        int addedTasks = newCount - originalCount;
                        
                        log.info("📋 PLAN REVIEW UPDATED TASKS: {} original → {} new ({} tasks {})", 
                                originalCount, newCount, 
                                Math.abs(addedTasks), 
                                addedTasks > 0 ? "ADDED" : "REMOVED");
                        
                        // Update allTasks with persisted versions that have database IDs
                        List<Task> persistedUpdatedTasks = new ArrayList<>();
                        for (Task task : updatedTasks) {
                            if (task.status() == TaskStatus.PENDING && task.id() == null) {
                                log.info("💡 NEW TASK FROM PLAN REVIEW: '{}'", 
                                        limitText(task.description(), 100));
                                Task savedTask = persistenceService.saveTask(task, goalId);
                                persistedUpdatedTasks.add(savedTask);
                            } else {
                                persistedUpdatedTasks.add(task);
                            }
                        }
                        allTasks = persistedUpdatedTasks;
                    } else {
                        log.debug("Plan review completed - no changes needed");
                    }
                }
            }
        }
        
        return allTasks;
    }
    
    private List<Task> executeTasksInParallel(List<Task> executableTasks, String userQuery, List<Task> allTasks) {
        if (executableTasks.size() == 1) {
            // Single task - no need for parallel execution
            Task task = executableTasks.getFirst();
            log.info("Executing single task: '{}'", task.description());
            
            List<Task> completedTasks = allTasks.stream()
                .filter(t -> t.status() == TaskStatus.COMPLETED)
                .toList();
                
            return List.of(taskAgent.executeTask(task, userQuery, completedTasks));
        }
        
        // Multiple tasks - execute in parallel
        log.info("Executing {} tasks in parallel", executableTasks.size());
        
        try (var parallelScope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<Task>> subtasks = executableTasks.stream()
                .map(task -> {
                    log.debug("Forking parallel execution for: '{}'", task.description());
                    
                    final List<Task> completedTasks = allTasks.stream()
                        .filter(t -> t.status() == TaskStatus.COMPLETED)
                        .toList();
                    final Task finalTask = task;
                    
                    return parallelScope.fork(() -> taskAgent.executeTask(finalTask, userQuery, completedTasks));
                })
                .toList();
                
            parallelScope.join();
            parallelScope.throwIfFailed();

            return subtasks.stream()
                .map(subtask -> {
                    try {
                        Task result = subtask.get();
                        log.info("Parallel task completed: '{}' with status: {}", result.description(), result.status());
                        return result;
                    } catch (Exception e) {
                        log.error("Failed to get result from parallel task", e);
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            
        } catch (Exception e) {
            log.error("Parallel task execution failed", e);
            throw new RuntimeException("Parallel execution failed", e);
        }
    }
    
    private List<Task> reviewPlanAfterTask(List<Task> currentTasks, Task completedTask) {
        try {
            return taskPlanAgent.reviewAndUpdatePlan(currentTasks, completedTask);
        } catch (Exception e) {
            log.warn("Failed to review plan after task completion: {}", e.getMessage());
            return currentTasks; // Return unchanged if review fails
        }
    }
    
    private Task cleanInvalidDependencies(Task task) {
        // For now, just remove all dependencies if there are validation issues
        // In a more sophisticated version, we could selectively remove only invalid ones
        return new Task(
            task.id(), // Preserve the existing ID
            task.description(),
            task.result(),
            task.status(),
            List.of(), // Remove all blocking dependencies
            List.of(), // Remove all informational dependencies
            task.createdAt(),
            task.completedAt()
        );
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}