package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskPreparationService {
    
    private static final Logger log = LoggerFactory.getLogger(TaskPreparationService.class);
    
    private final DependencyResolver dependencyResolver;
    
    public TaskPreparationService(DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }
    
    /**
     * Prepares tasks for execution by validating dependencies and cleaning up any issues.
     * This includes dependency validation and circular dependency detection/cleanup.
     * 
     * @param tasks The tasks to prepare
     * @return A list of tasks ready for execution with validated dependencies
     */
    public List<Task> prepareTasks(List<Task> tasks) {
        List<Task> currentTasks = new ArrayList<>(tasks);
        
        // Validate dependencies and clean up invalid ones
        List<String> validationErrors = dependencyResolver.validateDependencies(currentTasks);
        if (!validationErrors.isEmpty()) {
            log.warn("Dependency validation errors found: {}", validationErrors.size());
            log.warn("Removing invalid dependencies and continuing with independent task execution");
            
            // Remove invalid dependencies from all tasks
            currentTasks = currentTasks.stream()
                .map(this::cleanInvalidDependencies)
                .toList();
        }
        
        // Check for circular dependencies after cleanup
        if (dependencyResolver.hasCircularDependencies(currentTasks)) {
            log.warn("Circular dependencies detected, removing all dependencies and executing sequentially");
            currentTasks = currentTasks.stream()
                .map(this::removeAllDependencies)
                .toList();
        }
        
        return currentTasks;
    }
    
    /**
     * Removes all dependencies from a task when validation issues are detected.
     * 
     * @param task The task to clean
     * @return A new task with all dependencies removed
     */
    private Task cleanInvalidDependencies(Task task) {
        return new Task(
            task.id(), // Preserve the existing ID
            task.description(),
            task.result(),
            task.status(),
            List.of(), // Remove all blocking dependencies
            List.of(), // Remove all informational dependencies
            task.createdAt(),
            task.completedAt()
        );
    }
    
    /**
     * Removes all dependencies from a task when circular dependencies are detected.
     * 
     * @param task The task to clean
     * @return A new task with all dependencies removed
     */
    private Task removeAllDependencies(Task task) {
        return new Task(
            task.id(), // Preserve the existing ID
            task.description(),
            task.result(),
            task.status(),
            List.of(), // Remove all blocking dependencies
            List.of(), // Remove all informational dependencies
            task.createdAt(),
            task.completedAt()
        );
    }
}