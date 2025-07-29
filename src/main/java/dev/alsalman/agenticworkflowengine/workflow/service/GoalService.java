package dev.alsalman.agenticworkflowengine.workflow.service;

import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for managing goal lifecycle and persistence.
 * Handles creation, loading, and state management of goals.
 */
@Service
public class GoalService {
    
    private static final Logger log = LoggerFactory.getLogger(GoalService.class);
    
    private final WorkflowPersistenceService persistenceService;
    
    public GoalService(WorkflowPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }
    
    /**
     * Initializes a goal for workflow execution.
     * If goalId is provided, attempts to load existing goal.
     * If goalId is null or goal not found, creates a new goal.
     * 
     * @param userQuery The user's query/request
     * @param goalId Optional existing goal ID
     * @return Initialized goal ready for workflow execution
     */
    public Goal initializeGoal(String userQuery, UUID goalId) {
        if (goalId != null) {
            Goal existingGoal = persistenceService.findGoalById(goalId);
            if (existingGoal != null) {
                log.info("Loading existing goal: {}", goalId);
                return existingGoal;
            }
            log.warn("Goal not found with ID: {}, creating new goal", goalId);
        }
        
        return createNewGoal(userQuery);
    }
    
    /**
     * Creates a new goal for the given user query.
     * 
     * @param userQuery The user's query/request
     * @return New goal with IN_PROGRESS status
     */
    private Goal createNewGoal(String userQuery) {
        Goal newGoal = new Goal(
            null, // Let database generate ID
            userQuery,
            List.of(),
            null,
            GoalStatus.IN_PROGRESS,
            Instant.now(),
            null
        );
        
        Goal savedGoal = persistenceService.saveGoal(newGoal);
        log.info("Created new goal with ID: {}", savedGoal.id());
        return savedGoal;
    }
    
    /**
     * Updates goal status to FAILED with error message.
     * 
     * @param goal The goal to mark as failed
     * @param errorMessage The error message to include
     * @return Updated goal with FAILED status
     */
    public Goal markGoalAsFailed(Goal goal, String errorMessage) {
        Goal failedGoal = new Goal(
            goal.id(),
            goal.query(),
            goal.tasks(),
            "Workflow failed: " + errorMessage,
            GoalStatus.FAILED,
            goal.createdAt(),
            Instant.now()
        );
            
        return persistenceService.saveGoal(failedGoal);
    }
    
    /**
     * Updates goal with completed status and summary.
     * 
     * @param goal The goal to mark as completed
     * @param summary The completion summary
     * @return Updated goal with COMPLETED status
     */
    public Goal markGoalAsCompleted(Goal goal, String summary) {
        Goal completedGoal = new Goal(
            goal.id(),
            goal.query(),
            goal.tasks(),
            summary,
            GoalStatus.COMPLETED,
            goal.createdAt(),
            Instant.now()
        );
            
        return persistenceService.saveGoal(completedGoal);
    }
}