package dev.alsalman.agenticworkflowengine.template.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Database entity for template parameters
 */
@Table("template_parameters")
public record TemplateParameter(
    @Id
    UUID id,
    UUID templateId,
    String name,
    String description,
    String type, // Storing as String for ParameterType enum
    boolean required,
    String defaultValue,
    int displayOrder,
    Instant createdAt
) {
    
    public static TemplateParameter create(
        UUID templateId,
        String name,
        String description,
        ParameterType type,
        boolean required,
        String defaultValue,
        int displayOrder
    ) {
        return new TemplateParameter(
            null, // Let database generate ID
            templateId,
            name,
            description,
            type.name(),
            required,
            defaultValue,
            displayOrder,
            Instant.now()
        );
    }
    
    /**
     * Convert database entity to domain Parameter
     */
    public Parameter toDomainParameter() {
        return new Parameter(
            name,
            description,
            ParameterType.valueOf(type),
            required,
            defaultValue,
            null // Validation rules will be loaded separately
        );
    }
}