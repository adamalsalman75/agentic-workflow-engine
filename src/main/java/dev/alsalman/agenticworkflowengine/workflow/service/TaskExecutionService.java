package dev.alsalman.agenticworkflowengine.workflow.service;

import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.execution.TaskAgent;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Service
public class TaskExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);
    
    private final TaskAgent taskAgent;
    private final DependencyResolver dependencyResolver;
    
    public TaskExecutionService(TaskAgent taskAgent, DependencyResolver dependencyResolver) {
        this.taskAgent = taskAgent;
        this.dependencyResolver = dependencyResolver;
    }
    
    /**
     * Determines which tasks can be executed based on their dependencies.
     * 
     * @param remainingTasks All tasks that haven't been completed yet
     * @return List of tasks that are ready to execute (no blocking dependencies)
     */
    public List<Task> getExecutableTasks(List<Task> remainingTasks) {
        return dependencyResolver.getExecutableTasks(remainingTasks);
    }
    
    /**
     * Executes a batch of tasks in parallel, handling both single and multiple task scenarios.
     * 
     * @param executableTasks The tasks that are ready to execute (dependencies satisfied)
     * @param userQuery The original user query for context
     * @param completedTasks All completed tasks (for dependency context)
     * @return List of completed tasks
     */
    public List<Task> executeTasksInParallel(List<Task> executableTasks, String userQuery, List<Task> allTasks) {
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
}