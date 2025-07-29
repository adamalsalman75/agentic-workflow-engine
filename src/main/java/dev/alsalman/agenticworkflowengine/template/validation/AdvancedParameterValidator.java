package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule.ValidationRuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Advanced parameter validator that validates parameters against their validation rules
 */
@Component
public class AdvancedParameterValidator {
    
    // Date formats to try for parsing
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,    // yyyy-MM-dd
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );
    
    public AdvancedParameterValidator() {
    }
    
    /**
     * Validates a parameter value against all its validation rules
     * @return List of error messages (empty if valid)
     */
    public List<String> validateParameter(Parameter parameter, String value) {
        List<String> errors = new ArrayList<>();
        
        // First perform basic validation
        ParameterValidator.ValidationResult basicResult = ParameterValidator.validate(parameter, value);
        if (!basicResult.isValid()) {
            errors.addAll(basicResult.getErrors());
            return errors; // Return early if basic validation fails
        }
        
        // Skip further validation if value is null/empty and not required
        if ((value == null || value.isBlank()) && !parameter.required()) {
            return errors;
        }
        
        // Apply each validation rule
        for (ValidationRule rule : parameter.validationRules()) {
            String error = validateRule(parameter, value, rule);
            if (error != null) {
                errors.add(error);
            }
        }
        
        return errors;
    }
    
    private String validateRule(Parameter parameter, String value, ValidationRule rule) {
        return switch (rule.type()) {
            case REQUIRED -> validateRequired(value, rule);
            case PATTERN -> validatePattern(value, rule);
            case RANGE -> validateRange(parameter, value, rule);
            case DATE_RANGE -> validateDateRange(parameter, value, rule);
            case ALLOWED_VALUES -> validateAllowedValues(value, rule);
        };
    }
    
    private String validateRequired(String value, ValidationRule rule) {
        if (value == null || value.isBlank()) {
            return rule.customMessage() != null ? rule.customMessage() : "Value is required";
        }
        return null;
    }
    
    private String validatePattern(String value, ValidationRule rule) {
        if (rule.pattern() == null) {
            return null;
        }
        
        try {
            Pattern pattern = Pattern.compile(rule.pattern());
            if (!pattern.matcher(value).matches()) {
                return rule.customMessage() != null ? 
                    rule.customMessage() : 
                    String.format("Value must match pattern: %s", rule.pattern());
            }
        } catch (PatternSyntaxException e) {
            return "Invalid validation pattern configured";
        }
        
        return null;
    }
    
    private String validateRange(Parameter parameter, String value, ValidationRule rule) {
        if (parameter.type() != ParameterType.NUMBER) {
            return null; // Range validation only applies to NUMBER types
        }
        
        try {
            BigDecimal numValue = new BigDecimal(value);
            
            if (rule.minValue() != null && numValue.compareTo(rule.minValue()) < 0) {
                return rule.customMessage() != null ?
                    rule.customMessage() :
                    String.format("Value must be at least %s", rule.minValue());
            }
            
            if (rule.maxValue() != null && numValue.compareTo(rule.maxValue()) > 0) {
                return rule.customMessage() != null ?
                    rule.customMessage() :
                    String.format("Value must be at most %s", rule.maxValue());
            }
        } catch (NumberFormatException e) {
            // This should have been caught by basic validation
            return "Invalid number format";
        }
        
        return null;
    }
    
    private String validateDateRange(Parameter parameter, String value, ValidationRule rule) {
        if (parameter.type() != ParameterType.DATE) {
            return null; // Date range validation only applies to DATE types
        }
        
        LocalDate dateValue = parseDate(value);
        if (dateValue == null) {
            return "Invalid date format"; // Should have been caught by basic validation
        }
        
        if (rule.minDate() != null && dateValue.isBefore(rule.minDate())) {
            return rule.customMessage() != null ?
                rule.customMessage() :
                String.format("Date must be on or after %s", rule.minDate());
        }
        
        if (rule.maxDate() != null && dateValue.isAfter(rule.maxDate())) {
            return rule.customMessage() != null ?
                rule.customMessage() :
                String.format("Date must be on or before %s", rule.maxDate());
        }
        
        return null;
    }
    
    private String validateAllowedValues(String value, ValidationRule rule) {
        if (rule.allowedValues() == null || rule.allowedValues().isEmpty()) {
            return null;
        }
        
        if (!rule.allowedValues().contains(value)) {
            return rule.customMessage() != null ?
                rule.customMessage() :
                String.format("Value must be one of: %s", String.join(", ", rule.allowedValues()));
        }
        
        return null;
    }
    
    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        return null;
    }
}