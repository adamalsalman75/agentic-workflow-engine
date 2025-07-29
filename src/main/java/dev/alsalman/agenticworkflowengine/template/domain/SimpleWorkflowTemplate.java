package dev.alsalman.agenticworkflowengine.template.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Phase 1: Simplified template without complex data types
 * No Map, List, or complex relationships - just basic fields
 */
@Table("simple_templates")
public record SimpleWorkflowTemplate(
    @Id
    UUID id,
    String name,
    String description,
    String category,
    String promptTemplate,
    String author,
    boolean isPublic,
    Instant createdAt
) {
    
    public static SimpleWorkflowTemplate create(
        String name,
        String description,
        String category,
        String promptTemplate,
        String author
    ) {
        return new SimpleWorkflowTemplate(
            null, // Let database generate ID
            name,
            description,
            category,
            promptTemplate,
            author,
            true,
            Instant.now()
        );
    }
}