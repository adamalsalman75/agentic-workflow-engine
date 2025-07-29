package dev.alsalman.agenticworkflowengine.workflow.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Frontend-focused goal summary without task details.
 * Used for high-level goal status API responses.
 */
public record GoalSummary(
    UUID id,
    String query,
    String summary,
    GoalStatus status,
    Instant createdAt,
    Instant completedAt
) {
    
    /**
     * Creates a GoalSummary from a Goal domain object.
     * Excludes tasks to provide clean summary response.
     */
    public static GoalSummary from(Goal goal) {
        return new GoalSummary(
            goal.id(),
            goal.query(),
            goal.summary(),
            goal.status(),
            goal.createdAt(),
            goal.completedAt()
        );
    }
}