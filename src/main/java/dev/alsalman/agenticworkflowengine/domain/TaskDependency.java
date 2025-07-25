package dev.alsalman.agenticworkflowengine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("task_dependencies")
public record TaskDependency(
    @Id UUID id,
    UUID taskId,
    UUID dependsOnTaskId,
    DependencyType type,
    String reason,
    Instant createdAt
) {
    public static TaskDependency blocking(UUID taskId, UUID dependsOnTaskId, String reason) {
        return new TaskDependency(UUID.randomUUID(), taskId, dependsOnTaskId, DependencyType.BLOCKING, reason, Instant.now());
    }
    
    public static TaskDependency informational(UUID taskId, UUID dependsOnTaskId, String reason) {
        return new TaskDependency(UUID.randomUUID(), taskId, dependsOnTaskId, DependencyType.INFORMATIONAL, reason, Instant.now());
    }
}