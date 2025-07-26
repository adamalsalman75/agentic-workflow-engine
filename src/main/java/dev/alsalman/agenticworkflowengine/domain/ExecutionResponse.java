package dev.alsalman.agenticworkflowengine.domain;

import java.util.UUID;

public record ExecutionResponse(
    UUID goalId,
    String message
) {
    public static ExecutionResponse success(UUID goalId) {
        return new ExecutionResponse(goalId, "Workflow execution started");
    }
}