package dev.alsalman.agenticworkflowengine.domain;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Goal(
    UUID id,
    String query,
    List<Task> tasks,
    String summary,
    GoalStatus status,
    Instant createdAt,
    Instant completedAt
) {
    public static Goal create(String query) {
        return new Goal(
            UUID.randomUUID(),
            query,
            List.of(),
            null,
            GoalStatus.PLANNING,
            Instant.now(),
            null
        );
    }
    
    public Goal withTasks(List<Task> tasks) {
        return new Goal(
            id,
            query,
            tasks,
            summary,
            GoalStatus.IN_PROGRESS,
            createdAt,
            completedAt
        );
    }
    
    public Goal withSummary(String summary) {
        return new Goal(
            id,
            query,
            tasks,
            summary,
            GoalStatus.COMPLETED,
            createdAt,
            Instant.now()
        );
    }
    
    public Goal withStatus(GoalStatus status) {
        return new Goal(
            id,
            query,
            tasks,
            summary,
            status,
            createdAt,
            status == GoalStatus.COMPLETED ? Instant.now() : completedAt
        );
    }
}