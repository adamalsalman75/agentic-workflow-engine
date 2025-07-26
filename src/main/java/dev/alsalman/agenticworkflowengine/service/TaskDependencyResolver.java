package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;

@Component
public class TaskDependencyResolver {
    
    private static final Logger log = LoggerFactory.getLogger(TaskDependencyResolver.class);
    
    private final WorkflowPersistenceService persistenceService;
    
    public TaskDependencyResolver(WorkflowPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }
    
    /**
     * Coordinates task persistence and ensures dependency UUIDs are correctly mapped
     * from planning UUIDs to database UUIDs after persistence.
     * 
     * @param taskPlan The initial task plan from TaskPlanAgent
     * @param goalId The goal ID to associate tasks with
     * @return List of tasks with correctly mapped dependencies
     */
    public List<Task> coordinateTaskPersistence(TaskPlan taskPlan, UUID goalId) {
        log.info("Coordinating task persistence for {} tasks with {} dependencies", 
                 taskPlan.tasks().size(), taskPlan.dependencies().size());
        
        List<Task> planningTasks = taskPlan.tasks();
        List<TaskDependency> planningDependencies = taskPlan.dependencies();
        
        // Step 1: Persist tasks and capture UUID mapping (planning UUID -> database UUID)
        Map<UUID, UUID> uuidMapping = new HashMap<>();
        List<Task> persistedTasks = new ArrayList<>();
        
        for (Task planningTask : planningTasks) {
            UUID planningUuid = planningTask.id();
            
            // Create task without dependencies for initial persistence
            Task taskForPersistence = new Task(
                null, // Let database generate new UUID
                planningTask.description(),
                planningTask.result(),
                planningTask.status(),
                List.of(), // Empty dependencies for now
                List.of(), // Empty dependencies for now
                planningTask.createdAt(),
                planningTask.completedAt()
            );
            
            Task persistedTask = persistenceService.saveTask(taskForPersistence, goalId);
            UUID databaseUuid = persistedTask.id();
            
            // Record the mapping
            uuidMapping.put(planningUuid, databaseUuid);
            persistedTasks.add(persistedTask);
            
            log.debug("UUID mapping: {} -> {}", planningUuid, databaseUuid);
        }
        
        // Step 2: Remap dependencies from planning UUIDs to database UUIDs
        List<TaskDependency> remappedDependencies = planningDependencies.stream()
            .map(dep -> remapDependency(dep, uuidMapping))
            .filter(dep -> dep != null) // Filter out any dependencies that couldn't be mapped
            .toList();
            
        log.info("Remapped {} dependencies successfully", remappedDependencies.size());
        
        // Step 3: Update tasks with correctly mapped dependencies
        List<Task> finalTasks = persistedTasks.stream()
            .map(task -> updateTaskWithRemappedDependencies(task, remappedDependencies))
            .toList();
            
        // Step 4: Persist the remapped dependencies to database
        for (TaskDependency dependency : remappedDependencies) {
            try {
                persistenceService.saveTaskDependency(dependency);
                log.debug("Persisted dependency: {} -> {}", dependency.taskId(), dependency.dependsOnTaskId());
            } catch (Exception e) {
                log.warn("Failed to persist dependency: {} -> {}: {}", 
                        dependency.taskId(), dependency.dependsOnTaskId(), e.getMessage());
            }
        }
        
        log.info("Task coordination completed successfully. {} tasks with mapped dependencies ready for execution", 
                 finalTasks.size());
        
        return finalTasks;
    }
    
    private TaskDependency remapDependency(TaskDependency originalDep, Map<UUID, UUID> uuidMapping) {
        UUID originalTaskId = originalDep.taskId();
        UUID originalDependsOnId = originalDep.dependsOnTaskId();
        
        UUID newTaskId = uuidMapping.get(originalTaskId);
        UUID newDependsOnId = uuidMapping.get(originalDependsOnId);
        
        if (newTaskId == null) {
            log.warn("Could not find mapping for task ID: {}", originalTaskId);
            return null;
        }
        
        if (newDependsOnId == null) {
            log.warn("Could not find mapping for dependency task ID: {}", originalDependsOnId);
            return null;
        }
        
        // Create new dependency with mapped UUIDs
        return new TaskDependency(
            null, // Let database generate ID
            newTaskId,
            newDependsOnId,
            originalDep.type(),
            originalDep.reason(),
            originalDep.createdAt()
        );
    }
    
    private Task updateTaskWithRemappedDependencies(Task task, List<TaskDependency> remappedDependencies) {
        // Find all dependencies for this task
        List<UUID> blockingDeps = remappedDependencies.stream()
            .filter(dep -> dep.taskId().equals(task.id()) && 
                          dep.type() == dev.alsalman.agenticworkflowengine.domain.DependencyType.BLOCKING)
            .map(TaskDependency::dependsOnTaskId)
            .toList();
            
        List<UUID> informationalDeps = remappedDependencies.stream()
            .filter(dep -> dep.taskId().equals(task.id()) && 
                          dep.type() == dev.alsalman.agenticworkflowengine.domain.DependencyType.INFORMATIONAL)
            .map(TaskDependency::dependsOnTaskId)
            .toList();
        
        // Return task with correctly mapped dependencies
        return new Task(
            task.id(), // Keep the database UUID
            task.description(),
            task.result(),
            task.status(),
            blockingDeps,
            informationalDeps,
            task.createdAt(),
            task.completedAt()
        );
    }
}