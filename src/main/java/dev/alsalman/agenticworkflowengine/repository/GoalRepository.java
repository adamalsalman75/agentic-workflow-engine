package dev.alsalman.agenticworkflowengine.repository;

import dev.alsalman.agenticworkflowengine.domain.GoalEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoalRepository extends CrudRepository<GoalEntity, UUID> {
    
}