package dev.alsalman.agenticworkflowengine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("tasks")
public record TaskEntity(
    @Id UUID id,
    UUID goalId,
    String description,
    String result,
    TaskStatus status,
    List<UUID> blockingDependencies,
    List<UUID> informationalDependencies,
    Instant createdAt,
    Instant completedAt
) {
    public static TaskEntity fromTask(Task task, UUID goalId) {
        return new TaskEntity(
            null, // Let Spring Data generate the ID
            goalId,
            task.description(),
            task.result(),
            task.status(),
            task.blockingDependencies(),
            task.informationalDependencies(),
            task.createdAt(),
            task.completedAt()
        );
    }
    
    public static TaskEntity fromTaskWithId(Task task, UUID goalId) {
        return new TaskEntity(
            task.id(),
            goalId,
            task.description(),
            task.result(),
            task.status(),
            task.blockingDependencies(),
            task.informationalDependencies(),
            task.createdAt(),
            task.completedAt()
        );
    }
    
    public Task toTask() {
        return new Task(
            id,
            description,
            result,
            status,
            blockingDependencies,
            informationalDependencies,
            createdAt,
            completedAt
        );
    }
}