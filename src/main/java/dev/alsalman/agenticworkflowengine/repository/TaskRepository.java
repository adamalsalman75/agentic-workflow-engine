package dev.alsalman.agenticworkflowengine.repository;

import dev.alsalman.agenticworkflowengine.domain.TaskEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends CrudRepository<TaskEntity, UUID> {
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId ORDER BY created_at ASC")
    List<TaskEntity> findByGoalId(UUID goalId);
    
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId AND status = :status ORDER BY created_at ASC")
    List<TaskEntity> findByGoalIdAndStatus(UUID goalId, String status);
    
    @Query("DELETE FROM tasks WHERE goal_id = :goalId")
    void deleteByGoalId(UUID goalId);
}