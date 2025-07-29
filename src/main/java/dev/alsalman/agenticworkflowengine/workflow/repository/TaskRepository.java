package dev.alsalman.agenticworkflowengine.workflow.repository;

import dev.alsalman.agenticworkflowengine.workflow.domain.TaskEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends CrudRepository<TaskEntity, UUID> {
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId ORDER BY created_at ASC")
    List<TaskEntity> findByGoalId(UUID goalId);

}