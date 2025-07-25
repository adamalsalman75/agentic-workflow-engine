package dev.alsalman.agenticworkflowengine.agent;

import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.service.ResilientChatClient;
import org.springframework.stereotype.Component;

@Component
public class GoalAgent {
    
    private final ResilientChatClient resilientChatClient;
    
    public GoalAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    public Goal summarizeGoalCompletion(Goal goal) {
        String tasksInfo = goal.tasks().stream()
            .map(task -> "- " + limitText(task.description(), 60) + 
                        " [" + task.status() + "] " + 
                        limitText(task.result() != null ? task.result() : "No result", 80))
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
                limitText(goal.query(), 150),
                completedTasks,
                goal.tasks().size(),
                failedTasks,
                tasksInfo
            );
            
        String summary = resilientChatClient.call("goal summarization", prompt);
        return goal.withSummary(summary);
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}