package dev.alsalman.agenticworkflowengine.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TemplateExecution(
    UUID id,
    UUID templateId,
    UUID goalId,
    Map<String, Object> parameters,
    String userId,
    Instant executedAt
) {
    public static TemplateExecution create(
        UUID templateId,
        UUID goalId,
        Map<String, Object> parameters,
        String userId
    ) {
        return new TemplateExecution(
            UUID.randomUUID(),
            templateId,
            goalId,
            Map.copyOf(parameters),
            userId,
            Instant.now()
        );
    }
}