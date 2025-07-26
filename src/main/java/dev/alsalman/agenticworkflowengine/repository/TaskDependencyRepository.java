package dev.alsalman.agenticworkflowengine.repository;

import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskDependencyRepository extends CrudRepository<TaskDependency, UUID> {
    
    @Query("SELECT * FROM task_dependencies WHERE task_id = :taskId")
    List<TaskDependency> findByTaskId(UUID taskId);
    
    @Query("SELECT * FROM task_dependencies WHERE depends_on_task_id = :dependsOnTaskId")
    List<TaskDependency> findByDependsOnTaskId(UUID dependsOnTaskId);
    
    @Modifying
    @Query("DELETE FROM task_dependencies WHERE task_id IN (SELECT id FROM tasks WHERE goal_id = :goalId)")
    void deleteByGoalId(UUID goalId);
}