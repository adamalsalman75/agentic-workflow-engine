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
    
    public WorkflowResult executeWorkflow(String userQuery) {
        Instant startTime = Instant.now();
        log.info("Starting async workflow execution for query: '{}'", userQuery);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Create initial goal and save to database
            Goal goal = Goal.create(userQuery);
            goal = persistenceService.saveGoal(goal);
            log.debug("Created and saved goal with ID: {}", goal.id());
            
            // Create initial task plan
            log.info("Creating initial task plan asynchronously...");
            var planningTask = scope.fork(() -> taskPlanAgent.createTaskPlan(userQuery));
            
            scope.join();
            scope.throwIfFailed();
            
            List<Task> initialTasks = planningTask.get();
            goal = goal.withTasks(initialTasks);
            
            // Save tasks to database
            for (Task task : initialTasks) {
                persistenceService.saveTask(task, goal.id());
            }
            log.info("Created and saved {} initial tasks", initialTasks.size());
            initialTasks.forEach(task -> log.debug("Task: {}", task.description()));
            
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
                    .map(task -> Task.create(task.description()))
                    .toList();
            }
            
            currentTasks = executeTasksWithDependencies(currentTasks, userQuery, goal.id());
            
            // Update goal and generate summary
            goal = goal.withTasks(currentTasks);
            
            // Save updated tasks to database
            for (Task task : currentTasks) {
                persistenceService.saveTask(task, goal.id());
            }
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
            Goal failedGoal = Goal.create(userQuery).withStatus(dev.alsalman.agenticworkflowengine.domain.GoalStatus.FAILED);
            
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
                    List<Task> updatedTasks = reviewPlanAfterTask(allTasks, completedTask);
                    if (!updatedTasks.equals(allTasks)) {
                        log.info("Task plan updated after completing: '{}'", completedTask.description());
                        allTasks = new ArrayList<>(updatedTasks);
                        
                        // Save updated task plan to database
                        for (Task task : allTasks) {
                            persistenceService.saveTask(task, goalId);
                        }
                    }
                }
            }
        }
        
        return allTasks;
    }
    
    private List<Task> executeTasksInParallel(List<Task> executableTasks, String userQuery, List<Task> allTasks) {
        if (executableTasks.size() == 1) {
            // Single task - no need for parallel execution
            Task task = executableTasks.get(0);
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
            
            List<Task> results = subtasks.stream()
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
                
            return results;
            
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
        return Task.create(task.description());
    }
}