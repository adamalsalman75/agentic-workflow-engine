package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for validation rules in parameter discovery API
 */
public record ValidationRuleDto(
    String type,
    String pattern,
    BigDecimal minValue,
    BigDecimal maxValue,
    LocalDate minDate,
    LocalDate maxDate,
    List<String> allowedValues,
    String message
) {
    
    /**
     * Convert from domain ValidationRule to DTO
     */
    public static ValidationRuleDto fromValidationRule(ValidationRule rule) {
        return new ValidationRuleDto(
            rule.type().name(),
            rule.pattern(),
            rule.minValue(),
            rule.maxValue(),
            rule.minDate(),
            rule.maxDate(),
            rule.allowedValues(),
            rule.customMessage()
        );
    }
}