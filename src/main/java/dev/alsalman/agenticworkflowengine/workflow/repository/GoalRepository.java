package dev.alsalman.agenticworkflowengine.workflow.repository;

import dev.alsalman.agenticworkflowengine.workflow.domain.GoalEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoalRepository extends CrudRepository<GoalEntity, UUID> {
    
}