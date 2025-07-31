package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule.ValidationRuleType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified parameter validator optimized for LLM consumption.
 * Focus on essential validation while trusting LLM flexibility.
 */
@Component
public class AdvancedParameterValidator {
    
    public AdvancedParameterValidator() {
    }
    
    /**
     * Validates a parameter value against its validation rules
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
        
        // Apply validation rules - only REQUIRED and ALLOWED_VALUES are supported
        for (ValidationRule rule : parameter.validationRules()) {
            switch (rule.type()) {
                case REQUIRED -> {
                    if (value == null || value.isBlank()) {
                        errors.add(rule.customMessage() != null ? 
                            rule.customMessage() : 
                            "Required parameter '" + parameter.name() + "' is missing");
                    }
                }
                case ALLOWED_VALUES -> {
                    if (value != null && !rule.allowedValues().contains(value)) {
                        errors.add(rule.customMessage() != null ? 
                            rule.customMessage() : 
                            "Parameter '" + parameter.name() + "' must be one of: " + 
                            String.join(", ", rule.allowedValues()));
                    }
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Validates multiple parameters at once
     */
    public List<String> validateParameters(List<Parameter> parameters, java.util.Map<String, String> values) {
        List<String> allErrors = new ArrayList<>();
        
        for (Parameter parameter : parameters) {
            String value = values.get(parameter.name());
            List<String> paramErrors = validateParameter(parameter, value);
            allErrors.addAll(paramErrors);
        }
        
        return allErrors;
    }
}