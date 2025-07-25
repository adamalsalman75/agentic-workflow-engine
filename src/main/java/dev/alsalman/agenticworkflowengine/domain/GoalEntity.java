package dev.alsalman.agenticworkflowengine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("goals")
public record GoalEntity(
    @Id UUID id,
    String query,
    String summary,
    GoalStatus status,
    Instant createdAt,
    Instant completedAt
) {
    public static GoalEntity fromGoal(Goal goal) {
        return new GoalEntity(
            null, // Let Spring Data generate the ID
            goal.query(),
            goal.summary(),
            goal.status(),
            goal.createdAt(),
            goal.completedAt()
        );
    }
    
    public static GoalEntity fromGoalWithId(Goal goal) {
        return new GoalEntity(
            goal.id(),
            goal.query(),
            goal.summary(),
            goal.status(),
            goal.createdAt(),
            goal.completedAt()
        );
    }
    
    public Goal toGoal() {
        return new Goal(
            id,
            query,
            List.of(), // Tasks will be loaded separately
            summary,
            status,
            createdAt,
            completedAt
        );
    }
}