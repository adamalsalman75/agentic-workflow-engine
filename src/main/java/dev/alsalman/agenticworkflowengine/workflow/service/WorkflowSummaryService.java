package dev.alsalman.agenticworkflowengine.workflow.service;

import dev.alsalman.agenticworkflowengine.execution.GoalAgent;
import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for generating workflow summaries.
 * Coordinates with GoalAgent to create meaningful summaries of completed workflows.
 */
@Service
public class WorkflowSummaryService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowSummaryService.class);
    
    private final GoalAgent goalAgent;
    private final GoalService goalService;
    
    public WorkflowSummaryService(GoalAgent goalAgent, GoalService goalService) {
        this.goalAgent = goalAgent;
        this.goalService = goalService;
    }
    
    /**
     * Generates a summary for a completed workflow and updates the goal.
     * 
     * @param goal The goal to summarize
     * @param completedTasks All tasks that were completed
     * @return Goal updated with summary and completed status
     */
    public Goal summarizeWorkflow(Goal goal, List<Task> completedTasks) {
        log.info("Generating summary for goal: {}", goal.id());
        
        // Update goal with completed tasks
        Goal goalWithTasks = goal.withTasks(completedTasks);
        
        // Use GoalAgent to generate intelligent summary
        Goal summarizedGoal = goalAgent.summarizeGoalCompletion(goalWithTasks);
        
        // Mark goal as completed with the generated summary
        return goalService.markGoalAsCompleted(
            goalWithTasks, 
            summarizedGoal.summary()
        );
    }
}