package dev.alsalman.agenticworkflowengine.execution;

import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.infrastructure.ResilientChatClient;
import org.springframework.stereotype.Component;

@Component
public class GoalAgent {
    
    private final ResilientChatClient resilientChatClient;
    
    public GoalAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    public Goal summarizeGoalCompletion(Goal goal) {
        String tasksInfo = goal.tasks().stream()
            .map(task -> "- " + task.description() + 
                        " [" + task.status() + "] " + 
                        (task.result() != null ? task.result() : "No result"))
            .reduce("", (acc, taskInfo) -> acc + taskInfo + "\n");
            
        long completedTasks = goal.tasks().stream()
            .filter(task -> task.status().name().equals("COMPLETED"))
            .count();
            
        long failedTasks = goal.tasks().stream()
            .filter(task -> task.status().name().equals("FAILED"))
            .count();
            
        String prompt = """
            Goal: %s
            
            Results: %d/%d completed, %d failed
            
            Tasks:
            %s
            
            Provide concise summary: goal achievement, key results, issues, overall assessment.
            """.formatted(
                goal.query(),
                completedTasks,
                goal.tasks().size(),
                failedTasks,
                tasksInfo
            );
            
        String summary = resilientChatClient.call("goal summarization", prompt);
        return goal.withSummary(summary);
    }
}