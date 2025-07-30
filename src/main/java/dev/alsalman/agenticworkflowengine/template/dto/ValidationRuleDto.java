package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;

import java.util.List;

/**
 * Simplified DTO for validation rules optimized for LLM consumption
 */
public record ValidationRuleDto(
    String type,
    List<String> allowedValues,
    String message
) {
    
    /**
     * Convert from domain ValidationRule to DTO - simplified for LLM flexibility
     */
    public static ValidationRuleDto fromValidationRule(ValidationRule rule) {
        return new ValidationRuleDto(
            rule.type().name(),
            rule.allowedValues(),
            rule.customMessage()
        );
    }
}