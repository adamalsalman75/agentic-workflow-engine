package dev.alsalman.agenticworkflowengine.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkflowTemplate(
    UUID id,
    String name,
    String description,
    String category,
    List<TemplateParameter> parameters,
    String promptTemplate,
    Map<String, Object> metadata,
    List<String> tags,
    String author,
    int version,
    boolean isPublic,
    int usageCount,
    Double rating,
    Instant createdAt,
    Instant updatedAt
) {
    public WorkflowTemplate {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be blank");
        }
        if (promptTemplate == null || promptTemplate.isBlank()) {
            throw new IllegalArgumentException("Prompt template cannot be blank");
        }
        if (parameters == null) {
            parameters = List.of();
        }
        if (tags == null) {
            tags = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
    
    public static WorkflowTemplate create(
        String name,
        String description,
        String category,
        List<TemplateParameter> parameters,
        String promptTemplate,
        List<String> tags,
        String author
    ) {
        return new WorkflowTemplate(
            UUID.randomUUID(),
            name,
            description,
            category,
            parameters,
            promptTemplate,
            Map.of(),
            tags,
            author,
            1,
            true,
            0,
            null,
            Instant.now(),
            null
        );
    }
    
    public WorkflowTemplate withUsageIncrement() {
        return new WorkflowTemplate(
            id, name, description, category, parameters, promptTemplate,
            metadata, tags, author, version, isPublic, usageCount + 1,
            rating, createdAt, Instant.now()
        );
    }
}