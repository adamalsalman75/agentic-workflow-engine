package dev.alsalman.agenticworkflowengine.template.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a validation rule that can be applied to template parameters.
 * Different validation types are supported based on the parameter type.
 */
public record ValidationRule(
    ValidationRuleType type,
    String pattern,           // For STRING regex validation
    BigDecimal minValue,      // For NUMBER min validation
    BigDecimal maxValue,      // For NUMBER max validation
    LocalDate minDate,        // For DATE min validation
    LocalDate maxDate,        // For DATE max validation
    List<String> allowedValues, // For SELECTION validation
    String customMessage      // Custom error message for validation failure
) {
    
    /**
     * Types of validation rules that can be applied
     */
    public enum ValidationRuleType {
        REQUIRED,        // Value must be provided
        PATTERN,         // String must match regex pattern
        RANGE,           // Number must be within min/max range
        DATE_RANGE,      // Date must be within min/max range
        ALLOWED_VALUES   // Value must be one of allowed values
    }
    
    /**
     * Factory method for creating a regex pattern validation rule for STRING parameters
     */
    public static ValidationRule pattern(String pattern, String customMessage) {
        return new ValidationRule(
            ValidationRuleType.PATTERN,
            pattern,
            null, null, null, null, null,
            customMessage
        );
    }
    
    /**
     * Factory method for creating a min/max validation rule for NUMBER parameters
     */
    public static ValidationRule range(BigDecimal min, BigDecimal max, String customMessage) {
        return new ValidationRule(
            ValidationRuleType.RANGE,
            null,
            min, max,
            null, null, null,
            customMessage
        );
    }
    
    /**
     * Factory method for creating a date range validation rule for DATE parameters
     */
    public static ValidationRule dateRange(LocalDate min, LocalDate max, String customMessage) {
        return new ValidationRule(
            ValidationRuleType.DATE_RANGE,
            null, null, null,
            min, max,
            null,
            customMessage
        );
    }
    
    /**
     * Factory method for creating allowed values validation rule for SELECTION parameters
     */
    public static ValidationRule allowedValues(List<String> values, String customMessage) {
        return new ValidationRule(
            ValidationRuleType.ALLOWED_VALUES,
            null, null, null, null, null,
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
            null, null, null, null, null, null,
            customMessage
        );
    }
}