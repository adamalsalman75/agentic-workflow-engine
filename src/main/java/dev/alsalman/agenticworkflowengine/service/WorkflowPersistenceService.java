package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalEntity;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskEntity;
import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import dev.alsalman.agenticworkflowengine.domain.DependencyType;
import dev.alsalman.agenticworkflowengine.repository.GoalRepository;
import dev.alsalman.agenticworkflowengine.repository.TaskRepository;
import dev.alsalman.agenticworkflowengine.repository.TaskDependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class WorkflowPersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowPersistenceService.class);
    
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    public WorkflowPersistenceService(
            GoalRepository goalRepository, 
            TaskRepository taskRepository,
            TaskDependencyRepository taskDependencyRepository) {
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
    }
    
    @Transactional
    public Goal saveGoal(Goal goal) {
        log.debug("Saving goal with ID: {}", goal.id());
        
        GoalEntity goalEntity;
        GoalEntity saved;
        
        if (goal.id() != null && goalRepository.existsById(goal.id())) {
            // Update existing goal
            log.debug("Updating existing goal: {}", goal.id());
            goalEntity = GoalEntity.fromGoalWithId(goal);
            saved = goalRepository.save(goalEntity);
        } else {
            // Insert new goal
            log.debug("Inserting new goal");
            goalEntity = GoalEntity.fromGoal(goal);
            saved = goalRepository.save(goalEntity);
        }
        
        // Return goal with original tasks preserved but updated ID
        return new Goal(
            saved.id(),
            saved.query(),
            goal.tasks(), // Preserve original tasks
            saved.summary(),
            saved.status(),
            saved.createdAt(),
            saved.completedAt()
        );
    }
    
    
    @Transactional
    public Task saveTask(Task task, UUID goalId) {
        log.debug("Persisting task: '{}' [{}] with status: {}", 
                 task.description(), task.id(), task.status());
        
        TaskEntity taskEntity;
        TaskEntity saved;
        
        if (task.id() != null && taskRepository.existsById(task.id())) {
            // Update existing task
            log.debug("Updating existing task in database: {}", task.id());
            taskEntity = TaskEntity.fromTaskWithId(task, goalId);
            saved = taskRepository.save(taskEntity);
        } else {
            // Insert new task
            log.debug("Inserting completed task into database: {}", task.id());
            taskEntity = TaskEntity.fromTask(task, goalId);
            saved = taskRepository.save(taskEntity);
        }
        
        Task savedTask = saved.toTask();
        
        // Save task dependencies if this is a new task with dependencies
        if (task.id() == null && (!task.blockingDependencies().isEmpty() || !task.informationalDependencies().isEmpty())) {
            log.debug("Saving {} blocking and {} informational dependencies for task {}", 
                     task.blockingDependencies().size(), task.informationalDependencies().size(), savedTask.id());
            
            // Save blocking dependencies
            for (UUID dependsOnTaskId : task.blockingDependencies()) {
                TaskDependency dependency = TaskDependency.blocking(savedTask.id(), dependsOnTaskId, "blocking dependency");
                taskDependencyRepository.save(dependency);
                log.debug("Saved blocking dependency: {} -> {}", savedTask.id(), dependsOnTaskId);
            }
            
            // Save informational dependencies  
            for (UUID dependsOnTaskId : task.informationalDependencies()) {
                TaskDependency dependency = TaskDependency.informational(savedTask.id(), dependsOnTaskId, "informational dependency");
                taskDependencyRepository.save(dependency);
                log.debug("Saved informational dependency: {} -> {}", savedTask.id(), dependsOnTaskId);
            }
        }
        
        return savedTask;
    }
    
    
    @Transactional(readOnly = true)
    public Goal findGoalById(UUID goalId) {
        GoalEntity entity = goalRepository.findById(goalId).orElse(null);
        if (entity == null) {
            return null;
        }
        
        // Return goal without tasks - use /tasks endpoint for task details
        return new Goal(
            entity.id(),
            entity.query(),
            List.of(), // Empty task list - use dedicated /tasks endpoint
            entity.summary(),
            entity.status(),
            entity.createdAt(),
            entity.completedAt()
        );
    }
    
    @Transactional(readOnly = true)
    public List<Task> findTasksByGoalId(UUID goalId) {
        List<TaskEntity> taskEntities = taskRepository.findByGoalId(goalId);
        List<TaskDependency> allDependencies = taskDependencyRepository.findByGoalId(goalId);
        
        return taskEntities.stream()
            .map(taskEntity -> {
                Task task = taskEntity.toTask();
                
                // Find dependencies for this task
                List<UUID> blockingDeps = allDependencies.stream()
                    .filter(dep -> dep.taskId().equals(task.id()) && dep.type() == DependencyType.BLOCKING)
                    .map(TaskDependency::dependsOnTaskId)
                    .toList();
                    
                List<UUID> informationalDeps = allDependencies.stream()
                    .filter(dep -> dep.taskId().equals(task.id()) && dep.type() == DependencyType.INFORMATIONAL)
                    .map(TaskDependency::dependsOnTaskId)
                    .toList();
                
                // Return task with dependencies if any exist
                if (!blockingDeps.isEmpty() || !informationalDeps.isEmpty()) {
                    return new Task(
                        task.id(),
                        task.description(),
                        task.result(),
                        task.status(),
                        blockingDeps,
                        informationalDeps,
                        task.createdAt(),
                        task.completedAt()
                    );
                }
                
                return task;
            })
            .toList();
    }
    
    @Transactional
    public TaskDependency saveTaskDependency(TaskDependency dependency) {
        log.debug("Saving task dependency: {} -> {}", dependency.taskId(), dependency.dependsOnTaskId());
        return taskDependencyRepository.save(dependency);
    }
    
}