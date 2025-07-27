package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.agent.GoalAgent;
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
    private final TaskDependencyResolver taskDependencyResolver;
    private final GoalAgent goalAgent;
    private final DependencyResolver dependencyResolver;
    private final WorkflowPersistenceService persistenceService;
    private final TaskPreparationService taskPreparationService;
    private final TaskExecutionService taskExecutionService;
    private final PlanReviewService planReviewService;
    
    public WorkflowOrchestrator(TaskPlanAgent taskPlanAgent, TaskDependencyResolver taskDependencyResolver,
                              GoalAgent goalAgent, DependencyResolver dependencyResolver, 
                              WorkflowPersistenceService persistenceService,
                              TaskPreparationService taskPreparationService,
                              TaskExecutionService taskExecutionService,
                              PlanReviewService planReviewService) {
        this.taskPlanAgent = taskPlanAgent;
        this.taskDependencyResolver = taskDependencyResolver;
        this.goalAgent = goalAgent;
        this.dependencyResolver = dependencyResolver;
        this.persistenceService = persistenceService;
        this.taskPreparationService = taskPreparationService;
        this.taskExecutionService = taskExecutionService;
        this.planReviewService = planReviewService;
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
            return handleWorkflowFailure(goalId, userQuery, e, startTime);
        }
    }
    
    private WorkflowResult executeWorkflowWithGoal(String userQuery, Goal goal, Instant startTime) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Create initial task plan with dependencies
            log.info("Creating initial task plan with dependencies asynchronously...");
            var planningTask = scope.fork(() -> taskPlanAgent.createTaskPlanWithDependencies(userQuery));
            
            scope.join();
            scope.throwIfFailed();
            
            var taskPlan = planningTask.get();
            log.info("Created task plan with {} tasks and {} dependencies", 
                     taskPlan.tasks().size(), taskPlan.dependencies().size());
            
            // Use TaskDependencyAgent to coordinate task persistence and UUID mapping
            log.info("Coordinating task persistence and dependency mapping...");
            List<Task> coordinatedTasks = taskDependencyResolver.coordinateTaskPersistence(taskPlan, goal.id());
            
            // Update goal with tasks that have correctly mapped dependencies
            goal = goal.withTasks(coordinatedTasks);
            log.info("Coordination complete: {} tasks ready for dependency-aware execution", coordinatedTasks.size());
            coordinatedTasks.forEach(task -> log.debug("Task '{}' with {} blocking deps, {} informational deps", 
                                                       task.description(), 
                                                       task.blockingDependencies().size(),
                                                       task.informationalDependencies().size()));
            
            // Execute tasks using dependency-aware parallel execution
            log.info("Starting dependency-aware task execution phase...");
            List<Task> currentTasks = taskPreparationService.prepareTasks(goal.tasks());
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
            return handleWorkflowFailure(goal.id(), userQuery, e, startTime);
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
            List<Task> completedBatch = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, allTasks);
            
            // Update the task list with completed tasks and save to database
            for (Task completedTask : completedBatch) {
                allTasks = planReviewService.updateTaskInList(allTasks, completedTask, goalId);
                allTasks = planReviewService.handlePlanReview(allTasks, completedTask, goalId);
            }
        }
        
        return allTasks;
    }
    
    
    private WorkflowResult handleWorkflowFailure(UUID goalId, String userQuery, Exception exception, Instant startTime) {
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
        
        // Fallback: create a new failed goal
        Goal failedGoal = Goal.create(userQuery).withStatus(dev.alsalman.agenticworkflowengine.domain.GoalStatus.FAILED);
        return WorkflowResult.failure(failedGoal, startTime);
    }
}