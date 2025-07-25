package dev.alsalman.agenticworkflowengine.repository;

import dev.alsalman.agenticworkflowengine.domain.GoalEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends CrudRepository<GoalEntity, UUID> {
    
    @Query("SELECT * FROM goals ORDER BY created_at DESC LIMIT :limit")
    List<GoalEntity> findRecentGoals(int limit);
    
    @Query("SELECT * FROM goals WHERE status = :status ORDER BY created_at DESC")
    List<GoalEntity> findByStatus(String status);
}