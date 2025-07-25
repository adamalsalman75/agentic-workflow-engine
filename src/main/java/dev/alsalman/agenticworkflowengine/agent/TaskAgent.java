package dev.alsalman.agenticworkflowengine.agent;

import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.service.ResilientChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskAgent {
    
    private final ResilientChatClient resilientChatClient;
    
    public TaskAgent(ResilientChatClient resilientChatClient) {
        this.resilientChatClient = resilientChatClient;
    }
    
    public Task executeTask(Task task, String originalGoal, List<Task> completedTasks) {
        String completedTasksContext = completedTasks.stream()
            .limit(3)
            .map(completedTask -> "- " + limitText(completedTask.description(), 80) + 
                                " (" + limitText(completedTask.result(), 100) + ")")
            .reduce("", (acc, taskInfo) -> acc + taskInfo + "\n");
            
        String prompt = """
            Execute: %s
            
            Goal: %s
            
            Previous:
            %s
            
            Provide specific, actionable result:
            """.formatted(task.description(), 
                        limitText(originalGoal, 150), 
                        completedTasksContext);
            
        try {
            String result = resilientChatClient.call("task execution", prompt);
            return task.withResult(result);
        } catch (Exception e) {
            return task.withStatus(TaskStatus.FAILED)
                .withResult("Task execution failed: " + e.getMessage());
        }
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}