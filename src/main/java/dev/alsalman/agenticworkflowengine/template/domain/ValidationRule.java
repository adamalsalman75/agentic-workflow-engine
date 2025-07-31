package dev.alsalman.agenticworkflowengine.template.domain;

import java.util.List;

/**
 * Simplified validation rules optimized for LLM consumption.
 * Focus on essential validation while trusting LLM flexibility.
 */
public record ValidationRule(
    ValidationRuleType type,
    List<String> allowedValues, // For SELECTION validation only
    String customMessage        // Custom error message for validation failure
) {
    
    /**
     * Simplified validation rule types - only essential validation
     */
    public enum ValidationRuleType {
        REQUIRED,        // Value must be provided
        ALLOWED_VALUES   // Value must be one of allowed values (for SELECTION only)
    }
    
    /**
     * Factory method for creating allowed values validation rule for SELECTION parameters
     */
    public static ValidationRule allowedValues(List<String> values, String customMessage) {
        return new ValidationRule(
            ValidationRuleType.ALLOWED_VALUES,
            values,
            customMessage
        );
    }
    
    /**
     * Factory method for creating a required validation rule
     */
    public static ValidationRule required(String customMessage) {
        return new ValidationRule(
            ValidationRuleType.REQUIRED,
            null,
            customMessage
        );
    }
}