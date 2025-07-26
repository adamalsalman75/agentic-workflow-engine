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
        // Filter completed tasks to only include actual dependencies
        List<Task> dependencyTasks = completedTasks.stream()
            .filter(completed -> {
                var blockingDeps = task.blockingDependencies();
                var informationalDeps = task.informationalDependencies();
                return (blockingDeps != null && blockingDeps.contains(completed.id())) || 
                       (informationalDeps != null && informationalDeps.contains(completed.id()));
            })
            .toList();
            
        String dependencyContext;
        String prompt;
        
        if (!dependencyTasks.isEmpty()) {
            // Build dependency-specific context
            dependencyContext = dependencyTasks.stream()
                .map(dep -> {
                    var blockingDeps = task.blockingDependencies();
                    String depType = (blockingDeps != null && blockingDeps.contains(dep.id())) ? "REQUIRED" : "REFERENCE";
                    return String.format("%s DEPENDENCY: %s\nResult: %s", 
                                       depType,
                                       limitText(dep.description(), 100),
                                       limitText(dep.result(), 300));
                })
                .reduce("", (acc, depInfo) -> acc + depInfo + "\n\n");
                
            prompt = """
                Execute: %s
                
                Overall Goal: %s
                
                DEPENDENCY OUTPUTS (use these results to complete your task):
                %s
                
                IMPORTANT: Your task should build upon and reference the dependency outputs above.
                Use specific information from the completed dependencies to inform your work.
                
                Provide specific, actionable result:
                """.formatted(task.description(), 
                            limitText(originalGoal, 150), 
                            dependencyContext);
        } else {
            // No dependencies - provide general context from recent completed tasks
            String generalContext = completedTasks.stream()
                .limit(3)
                .map(completed -> "- " + limitText(completed.description(), 80) + 
                                " (" + limitText(completed.result(), 100) + ")")
                .reduce("", (acc, taskInfo) -> acc + taskInfo + "\n");
                
            prompt = """
                Execute: %s
                
                Goal: %s
                
                Previous completed tasks (for context):
                %s
                
                Provide specific, actionable result:
                """.formatted(task.description(), 
                            limitText(originalGoal, 150), 
                            generalContext);
        }
            
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