package dev.alsalman.agenticworkflowengine.planning;

import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DependencyResolver {
    
    /**
     * Get tasks that are ready to execute (no blocking dependencies or all dependencies completed)
     */
    public List<Task> getExecutableTasks(List<Task> allTasks) {
        Set<UUID> completedTaskIds = getCompletedTaskIds(allTasks);
        
        return allTasks.stream()
            .filter(task -> task.status() == TaskStatus.PENDING)
            .filter(task -> task.canExecute(completedTaskIds.stream().toList()))
            .toList();
    }
    
    
    
    /**
     * Check if all tasks can be completed (no circular dependencies)
     */
    public boolean hasCircularDependencies(List<Task> tasks) {
        // Simple cycle detection using DFS
        for (Task task : tasks) {
            if (hasCycleDFS(task, tasks, Set.of(), Set.of())) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Validate task dependencies are all valid (reference existing tasks)
     */
    public List<String> validateDependencies(List<Task> tasks) {
        Set<UUID> taskIds = tasks.stream().map(Task::id).collect(Collectors.toSet());
        return tasks.stream()
            .flatMap(task -> {
                List<String> errors = task.blockingDependencies().stream()
                    .filter(depId -> !taskIds.contains(depId))
                    .map(depId -> "Task '" + task.description() + "' has invalid blocking dependency: " + depId)
                    .toList();
                    
                List<String> infoErrors = task.informationalDependencies().stream()
                    .filter(depId -> !taskIds.contains(depId))
                    .map(depId -> "Task '" + task.description() + "' has invalid informational dependency: " + depId)
                    .toList();
                    
                return Stream.of(errors, infoErrors).flatMap(List::stream);
            })
            .toList();
    }
    
    private Set<UUID> getCompletedTaskIds(List<Task> tasks) {
        return tasks.stream()
            .filter(task -> task.status() == TaskStatus.COMPLETED)
            .map(Task::id)
            .collect(Collectors.toSet());
    }
    
    private boolean hasCycleDFS(Task task, List<Task> allTasks, Set<UUID> visiting, Set<UUID> visited) {
        if (visiting.contains(task.id())) {
            return true; // Cycle detected
        }
        if (visited.contains(task.id())) {
            return false; // Already processed
        }
        
        // Create mutable copies of the sets
        Set<UUID> newVisiting = new HashSet<>(visiting);
        Set<UUID> newVisited = new HashSet<>(visited);
        
        newVisiting.add(task.id());
        
        for (UUID depId : task.blockingDependencies()) {
            Task depTask = findTaskById(allTasks, depId);
            if (depTask != null && hasCycleDFS(depTask, allTasks, newVisiting, newVisited)) {
                return true;
            }
        }
        
        newVisiting.remove(task.id());
        newVisited.add(task.id());
        return false;
    }
    
    private Task findTaskById(List<Task> tasks, UUID id) {
        return tasks.stream()
            .filter(task -> task.id().equals(id))
            .findFirst()
            .orElse(null);
    }
}