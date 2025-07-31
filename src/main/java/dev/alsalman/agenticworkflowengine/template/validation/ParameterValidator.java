package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;

import java.util.List;

/**
 * Simplified parameter validator optimized for LLM consumption.
 * Focus on essential validation while trusting LLM flexibility.
 */
public class ParameterValidator {
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }
        
        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
    }
    
    public static ValidationResult validate(Parameter parameter, Object value) {
        if (value == null) {
            if (parameter.required()) {
                return ValidationResult.failure("Required parameter '" + parameter.name() + "' is missing");
            }
            return ValidationResult.success();
        }
        
        return switch (parameter.type()) {
            case TEXT -> validateText(parameter, value.toString());
            case NUMBER -> validateNumber(parameter, value);
            case BOOLEAN -> validateBoolean(parameter, value.toString());
            case SELECTION -> validateSelection(parameter, value.toString());
        };
    }
    
    private static ValidationResult validateText(Parameter parameter, String value) {
        if (value.trim().isEmpty() && parameter.required()) {
            return ValidationResult.failure("Required parameter '" + parameter.name() + "' cannot be empty");
        }
        // For TEXT type, we trust LLM to handle any format (emails, dates, currencies, etc.)
        return ValidationResult.success();
    }
    
    private static ValidationResult validateNumber(Parameter parameter, Object value) {
        try {
            if (value instanceof Number) {
                return ValidationResult.success();
            }
            Double.parseDouble(value.toString());
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Parameter '" + parameter.name() + "' must be a valid number");
        }
    }
    
    private static ValidationResult validateBoolean(Parameter parameter, String value) {
        String normalized = value.toLowerCase().trim();
        if (normalized.equals("true") || normalized.equals("false") || 
            normalized.equals("yes") || normalized.equals("no") ||
            normalized.equals("1") || normalized.equals("0")) {
            return ValidationResult.success();
        }
        return ValidationResult.failure("Parameter '" + parameter.name() + "' must be true/false, yes/no, or 1/0");
    }
    
    private static ValidationResult validateSelection(Parameter parameter, String value) {
        // Check if parameter has ALLOWED_VALUES validation rule
        for (ValidationRule rule : parameter.validationRules()) {
            if (rule.type() == ValidationRule.ValidationRuleType.ALLOWED_VALUES) {
                if (!rule.allowedValues().contains(value)) {
                    return ValidationResult.failure("Parameter '" + parameter.name() + 
                        "' must be one of: " + String.join(", ", rule.allowedValues()));
                }
            }
        }
        return ValidationResult.success();
    }
}