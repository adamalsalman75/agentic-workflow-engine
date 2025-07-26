package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalEntity;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskEntity;
import dev.alsalman.agenticworkflowengine.repository.GoalRepository;
import dev.alsalman.agenticworkflowengine.repository.TaskRepository;
import dev.alsalman.agenticworkflowengine.repository.TaskDependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowPersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowPersistenceService.class);
    
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;

    public WorkflowPersistenceService(
            GoalRepository goalRepository, 
            TaskRepository taskRepository,
            TaskDependencyRepository taskDependencyRepository) {
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
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
                 limitText(task.description(), 60), task.id(), task.status());
        
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
        
        return saved.toTask();
    }
    
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
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
        return taskRepository.findByGoalId(goalId)
            .stream()
            .map(TaskEntity::toTask)
            .toList();
    }
    
    
}