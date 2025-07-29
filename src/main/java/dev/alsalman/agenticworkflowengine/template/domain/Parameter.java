package dev.alsalman.agenticworkflowengine.template.domain;

import java.util.List;

/**
 * Parameter definition with validation rules support
 */
public record Parameter(
    String name,
    String description,
    ParameterType type,
    boolean required,
    String defaultValue,
    List<ValidationRule> validationRules
) {
    
    public static Parameter required(String name, String description, ParameterType type) {
        return new Parameter(name, description, type, true, null, List.of());
    }
    
    public static Parameter optional(String name, String description, ParameterType type, String defaultValue) {
        return new Parameter(name, description, type, false, defaultValue, List.of());
    }
    
    public static Parameter requiredWithValidation(String name, String description, ParameterType type, List<ValidationRule> rules) {
        return new Parameter(name, description, type, true, null, rules);
    }
    
    public static Parameter optionalWithValidation(String name, String description, ParameterType type, String defaultValue, List<ValidationRule> rules) {
        return new Parameter(name, description, type, false, defaultValue, rules);
    }
}