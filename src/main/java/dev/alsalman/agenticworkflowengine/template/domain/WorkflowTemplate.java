package dev.alsalman.agenticworkflowengine.template.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Workflow template with advanced validation and parameter support
 */
@Table("templates")
public record WorkflowTemplate(
    @Id
    UUID id,
    String name,
    String description,
    String category,
    String promptTemplate,
    String author,
    boolean isPublic,
    Integer version,
    Instant createdAt,
    Instant updatedAt
) {
    
    public static WorkflowTemplate create(
        String name,
        String description,
        String category,
        String promptTemplate,
        String author
    ) {
        return new WorkflowTemplate(
            null, // Let database generate ID
            name,
            description,
            category,
            promptTemplate,
            author,
            true,
            1, // Initial version
            Instant.now(),
            Instant.now()
        );
    }
}