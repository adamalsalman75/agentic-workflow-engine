package dev.alsalman.agenticworkflowengine.domain;

import java.time.Duration;
import java.time.Instant;

public record WorkflowResult(
    Goal goal,
    Instant startTime,
    Instant endTime,
    Duration duration,
    boolean success
) {
    public static WorkflowResult success(Goal goal, Instant startTime) {
        Instant endTime = Instant.now();
        return new WorkflowResult(
            goal,
            startTime,
            endTime,
            Duration.between(startTime, endTime),
            true
        );
    }
    
    public static WorkflowResult failure(Goal goal, Instant startTime) {
        Instant endTime = Instant.now();
        return new WorkflowResult(
            goal,
            startTime,
            endTime,
            Duration.between(startTime, endTime),
            false
        );
    }
}