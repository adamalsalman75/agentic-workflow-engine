package dev.alsalman.agenticworkflowengine.repository;

import dev.alsalman.agenticworkflowengine.domain.TaskDependency;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskDependencyRepository extends CrudRepository<TaskDependency, UUID> {

}