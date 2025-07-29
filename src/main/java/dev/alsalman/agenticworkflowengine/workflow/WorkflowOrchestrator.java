package dev.alsalman.agenticworkflowengine.workflow;

import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the workflow execution with three core responsibilities:
 * 1. Map user's goal into TaskPlan
 * 2. Execute and review the TaskPlan after every task
 * 3. Create a summary after everything is executed
 */
@Service
public class WorkflowOrchestrator {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowOrchestrator.class);
    
    private final dev.alsalman.agenticworkflowengine.workflow.service.GoalService goalService;
    private final dev.alsalman.agenticworkflowengine.planning.TaskPlanService taskPlanService;
    private final dev.alsalman.agenticworkflowengine.execution.TaskPersistenceService taskPersistenceService;
    private final dev.alsalman.agenticworkflowengine.execution.TaskPreparationService taskPreparationService;
    private final dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService taskExecutionService;
    private final dev.alsalman.agenticworkflowengine.planning.PlanReviewService planReviewService;
    private final dev.alsalman.agenticworkflowengine.workflow.service.WorkflowSummaryService summaryService;
    
    public WorkflowOrchestrator(dev.alsalman.agenticworkflowengine.workflow.service.GoalService goalService,
                              dev.alsalman.agenticworkflowengine.planning.TaskPlanService taskPlanService,
                              dev.alsalman.agenticworkflowengine.execution.TaskPersistenceService taskPersistenceService,
                              dev.alsalman.agenticworkflowengine.execution.TaskPreparationService taskPreparationService,
                              dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService taskExecutionService,
                              dev.alsalman.agenticworkflowengine.planning.PlanReviewService planReviewService,
                              dev.alsalman.agenticworkflowengine.workflow.service.WorkflowSummaryService summaryService) {
        this.goalService = goalService;
        this.taskPlanService = taskPlanService;
        this.taskPersistenceService = taskPersistenceService;
        this.taskPreparationService = taskPreparationService;
        this.taskExecutionService = taskExecutionService;
        this.planReviewService = planReviewService;
        this.summaryService = summaryService;
    }
    
    /**
     * Orchestrates the workflow execution for a user query.
     * 
     * Core responsibilities:
     * 1. Map user's goal into TaskPlan
     * 2. Execute and review the TaskPlan after every task
     * 3. Create a summary after everything is executed
     * 
     * @param userQuery The user's query/request
     * @param goalId Optional existing goal ID
     * @return WorkflowResult containing the completed goal
     */
    public WorkflowResult executeWorkflow(String userQuery, UUID goalId) {
        Instant startTime = Instant.now();
        log.info("Starting workflow execution for query: '{}' with goal ID: {}", userQuery, goalId);
        
        try {
            // 1. Initialize goal (load existing or create new)
            Goal goal = goalService.initializeGoal(userQuery, goalId);
            
            // 2. Map user's goal into TaskPlan
            TaskPlan taskPlan = createTaskPlan(userQuery);
            List<Task> tasks = persistTaskPlan(taskPlan, goal.id());
            
            // 3. Execute and review the TaskPlan
            List<Task> completedTasks = executeTasksWithReview(tasks, userQuery, goal.id());
            
            // 4. Create summary after everything is executed
            Goal completedGoal = summaryService.summarizeWorkflow(goal, completedTasks);
            
            Duration executionTime = Duration.between(startTime, Instant.now());
            log.info("Workflow execution completed successfully in {} ms", executionTime.toMillis());
            
            return WorkflowResult.success(completedGoal, startTime);
            
        } catch (Exception e) {
            log.error("Workflow execution failed for query: '{}' with goal ID: {}", userQuery, goalId, e);
            
            // Handle failure by updating goal status
            Goal failedGoal = handleWorkflowFailure(goalId, userQuery, e);
            return WorkflowResult.failure(failedGoal, startTime);
        }
    }
    
    /**
     * Step 1: Map user's goal into TaskPlan
     */
    private TaskPlan createTaskPlan(String userQuery) {
        log.info("Creating task plan for query: '{}'", userQuery);
        return taskPlanService.createTaskPlan(userQuery);
    }
    
    /**
     * Persist the task plan with proper dependency mapping
     */
    private List<Task> persistTaskPlan(TaskPlan taskPlan, UUID goalId) {
        log.info("Persisting task plan with {} tasks", taskPlan.tasks().size());
        return taskPersistenceService.persistTaskPlan(taskPlan, goalId);
    }
    
    /**
     * Step 2: Execute and review the TaskPlan after every task
     */
    private List<Task> executeTasksWithReview(List<Task> tasks, String userQuery, UUID goalId) {
        log.info("Starting task execution with review cycle");
        
        // Prepare tasks (validate dependencies)
        List<Task> preparedTasks = taskPreparationService.prepareTasks(tasks);
        List<Task> completedTasks = new ArrayList<>();
        List<Task> remainingTasks = new ArrayList<>(preparedTasks);
        
        // Execute tasks based on dependencies
        while (!remainingTasks.isEmpty()) {
            List<Task> executableTasks = taskExecutionService.getExecutableTasks(remainingTasks);
            
            if (executableTasks.isEmpty()) {
                log.error("No executable tasks found, but {} tasks remain", remainingTasks.size());
                break;
            }
            
            log.info("Found {} executable tasks for execution", executableTasks.size());
            
            // Execute tasks in parallel when possible
            List<Task> executedTasks = taskExecutionService.executeTasksInParallel(
                executableTasks, userQuery, completedTasks
            );
            
            // Update task lists and review plan after each execution
            for (Task executedTask : executedTasks) {
                remainingTasks = planReviewService.updateTaskInList(remainingTasks, executedTask, goalId);
                completedTasks.add(executedTask);
                
                // Review and potentially update remaining tasks
                remainingTasks = planReviewService.handlePlanReview(remainingTasks, executedTask, goalId);
            }
        }
        
        return completedTasks;
    }
    
    /**
     * Handle workflow failure by updating goal status
     */
    private Goal handleWorkflowFailure(UUID goalId, String userQuery, Exception e) {
        try {
            Goal goal = goalService.initializeGoal(userQuery, goalId);
            return goalService.markGoalAsFailed(goal, e.getMessage());
        } catch (Exception ex) {
            log.error("Failed to update goal status for failure handling", ex);
            // Return a minimal failed goal if we can't load/save
            return new Goal(goalId, userQuery, List.of(), 
                          "Workflow failed: " + e.getMessage(), 
                          dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus.FAILED, 
                          Instant.now(), Instant.now());
        }
    }
}