package dev.alsalman.agenticworkflowengine.template.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Database entity for parameter validation rules - simplified for LLM flexibility
 */
@Table("parameter_validation_rules")
public record ParameterValidationRule(
    @Id
    UUID id,
    UUID parameterId,
    String validationType,
    String ruleValue, // JSON as TEXT for Spring Data JDBC compatibility
    String errorMessage,
    Instant createdAt
) {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static ParameterValidationRule create(
        UUID parameterId,
        String validationType,
        Map<String, Object> ruleValue,
        String errorMessage
    ) {
        String ruleValueJson;
        try {
            ruleValueJson = objectMapper.writeValueAsString(ruleValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize rule value to JSON", e);
        }
        
        return new ParameterValidationRule(
            null, // Let database generate ID
            parameterId,
            validationType,
            ruleValueJson,
            errorMessage,
            Instant.now()
        );
    }
    
    /**
     * Get rule value as Map for business logic
     */
    public Map<String, Object> getRuleValueAsMap() {
        try {
            return objectMapper.readValue(ruleValue, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse rule value JSON", e);
        }
    }
    
    /**
     * Convert database entity to domain ValidationRule - simplified for LLM flexibility
     */
    public ValidationRule toDomainValidationRule() {
        Map<String, Object> ruleMap = getRuleValueAsMap();
        
        return switch (validationType) {
            case "ALLOWED_VALUES" -> {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) ruleMap.get("values");
                yield ValidationRule.allowedValues(values, errorMessage);
            }
            case "REQUIRED" -> ValidationRule.required(errorMessage);
            default -> throw new IllegalArgumentException("Unknown validation type: " + validationType);
        };
    }
    
    /**
     * Create from domain ValidationRule - simplified for LLM flexibility
     */
    public static ParameterValidationRule fromDomainValidationRule(UUID parameterId, ValidationRule rule) {
        Map<String, Object> ruleValueMap = switch (rule.type()) {
            case ALLOWED_VALUES -> Map.of("values", rule.allowedValues());
            case REQUIRED -> Map.of(); // No additional data needed for required validation
        };
        
        return create(
            parameterId,
            rule.type().name(),
            ruleValueMap,
            rule.customMessage()
        );
    }
}