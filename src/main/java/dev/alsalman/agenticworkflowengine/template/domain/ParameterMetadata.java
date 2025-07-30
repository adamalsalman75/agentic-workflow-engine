package dev.alsalman.agenticworkflowengine.template.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Database entity for parameter metadata
 */
@Table("parameter_metadata")
public record ParameterMetadata(
    @Id
    UUID id,
    UUID parameterId,
    String placeholder,
    String helpText,
    String displayGroup,
    String uiComponent,
    String additionalProperties, // JSON as TEXT for Spring Data JDBC compatibility
    Instant createdAt
) {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static ParameterMetadata create(
        UUID parameterId,
        String placeholder,
        String helpText,
        String displayGroup,
        String uiComponent,
        Map<String, Object> additionalProperties
    ) {
        String additionalPropertiesJson = null;
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            try {
                additionalPropertiesJson = objectMapper.writeValueAsString(additionalProperties);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize additional properties to JSON", e);
            }
        }
        
        return new ParameterMetadata(
            null, // Let database generate ID
            parameterId,
            placeholder,
            helpText,
            displayGroup,
            uiComponent,
            additionalPropertiesJson,
            Instant.now()
        );
    }
    
    /**
     * Get additional properties as Map for business logic
     */
    public Map<String, Object> getAdditionalPropertiesAsMap() {
        if (additionalProperties == null || additionalProperties.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(additionalProperties, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse additional properties JSON", e);
        }
    }
    
    /**
     * Convert to DTO for API response
     */
    public dev.alsalman.agenticworkflowengine.template.dto.ParameterMetadataDto toDto(int order) {
        return new dev.alsalman.agenticworkflowengine.template.dto.ParameterMetadataDto(
            placeholder,
            helpText,
            order,
            displayGroup
        );
    }
}