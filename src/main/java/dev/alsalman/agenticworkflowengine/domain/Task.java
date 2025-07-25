package dev.alsalman.agenticworkflowengine.domain;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Task(
    UUID id,
    String description,
    String result,
    TaskStatus status,
    List<UUID> blockingDependencies,
    List<UUID> informationalDependencies,
    Instant createdAt,
    Instant completedAt
) {
    public static Task create(String description) {
        return new Task(
            UUID.randomUUID(),
            description,
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
    }
    
    public static Task create(String description, List<UUID> blockingDependencies, List<UUID> informationalDependencies) {
        return new Task(
            UUID.randomUUID(),
            description,
            null,
            TaskStatus.PENDING,
            blockingDependencies != null ? blockingDependencies : List.of(),
            informationalDependencies != null ? informationalDependencies : List.of(),
            Instant.now(),
            null
        );
    }
    
    public Task withResult(String result) {
        return new Task(
            id,
            description,
            result,
            TaskStatus.COMPLETED,
            blockingDependencies,
            informationalDependencies,
            createdAt,
            Instant.now()
        );
    }
    
    public Task withStatus(TaskStatus status) {
        return new Task(
            id,
            description,
            result,
            status,
            blockingDependencies,
            informationalDependencies,
            createdAt,
            status == TaskStatus.COMPLETED ? Instant.now() : completedAt
        );
    }
    
    public boolean hasBlockingDependencies() {
        return !blockingDependencies.isEmpty();
    }
    
    public boolean hasInformationalDependencies() {
        return !informationalDependencies.isEmpty();
    }
    
    public boolean canExecute(List<UUID> completedTaskIds) {
        return blockingDependencies.stream().allMatch(completedTaskIds::contains);
    }
}