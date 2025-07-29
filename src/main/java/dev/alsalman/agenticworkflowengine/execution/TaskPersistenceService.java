package dev.alsalman.agenticworkflowengine.execution;

import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for coordinating task persistence operations.
 * Handles saving tasks and managing task-to-goal relationships.
 */
@Service
public class TaskPersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPersistenceService.class);
    
    private final TaskDependencyResolver dependencyResolver;
    private final WorkflowPersistenceService persistenceService;
    
    public TaskPersistenceService(TaskDependencyResolver dependencyResolver, 
                                 WorkflowPersistenceService persistenceService) {
        this.dependencyResolver = dependencyResolver;
        this.persistenceService = persistenceService;
    }
    
    /**
     * Persists all tasks from a task plan and establishes their relationships.
     * Coordinates with TaskDependencyResolver to ensure proper ID mapping for dependencies.
     * 
     * @param taskPlan The task plan containing tasks to persist
     * @param goalId The goal ID to associate tasks with
     * @return List of persisted tasks with database-generated IDs
     */
    public List<Task> persistTaskPlan(TaskPlan taskPlan, UUID goalId) {
        log.info("Persisting task plan with {} tasks for goal: {}", 
                taskPlan.tasks().size(), goalId);
        
        // Delegate to dependency resolver for coordinated persistence
        List<Task> persistedTasks = dependencyResolver.coordinateTaskPersistence(taskPlan, goalId);
        
        log.info("Successfully persisted {} tasks with dependencies", persistedTasks.size());
        return persistedTasks;
    }
    
    /**
     * Saves a single task update, typically after task completion.
     * 
     * @param task The task to save
     * @param goalId The associated goal ID
     * @return The saved task
     */
    public Task saveTaskUpdate(Task task, UUID goalId) {
        return persistenceService.saveTask(task, goalId);
    }
    
    /**
     * Loads all tasks associated with a goal.
     * 
     * @param goalId The goal ID
     * @return List of tasks for the goal
     */
    public List<Task> loadTasksForGoal(UUID goalId) {
        return persistenceService.findTasksByGoalId(goalId);
    }
}