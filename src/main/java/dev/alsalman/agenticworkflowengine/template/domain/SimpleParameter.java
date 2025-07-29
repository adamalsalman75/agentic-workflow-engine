package dev.alsalman.agenticworkflowengine.template.domain;

import java.util.List;

/**
 * Simple parameter definition with validation rules support
 */
public record SimpleParameter(
    String name,
    String description,
    SimpleParameterType type,
    boolean required,
    String defaultValue,
    List<ValidationRule> validationRules
) {
    
    public static SimpleParameter required(String name, String description, SimpleParameterType type) {
        return new SimpleParameter(name, description, type, true, null, List.of());
    }
    
    public static SimpleParameter optional(String name, String description, SimpleParameterType type, String defaultValue) {
        return new SimpleParameter(name, description, type, false, defaultValue, List.of());
    }
    
    public static SimpleParameter requiredWithValidation(String name, String description, SimpleParameterType type, List<ValidationRule> rules) {
        return new SimpleParameter(name, description, type, true, null, rules);
    }
    
    public static SimpleParameter optionalWithValidation(String name, String description, SimpleParameterType type, String defaultValue, List<ValidationRule> rules) {
        return new SimpleParameter(name, description, type, false, defaultValue, rules);
    }
}