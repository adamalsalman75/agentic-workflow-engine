package dev.alsalman.agenticworkflowengine.domain;

import java.util.List;
import java.util.Map;

public record TemplateParameter(
    String name,
    String description,
    ParameterType type,
    boolean required,
    Object defaultValue,
    List<String> allowedValues,
    Map<String, Object> validation,
    String placeholder,
    int orderIndex
) {
    public TemplateParameter {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parameter name cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Parameter type cannot be null");
        }
        if (allowedValues == null) {
            allowedValues = List.of();
        }
        if (validation == null) {
            validation = Map.of();
        }
    }
    
    public static TemplateParameter required(
        String name,
        String description,
        ParameterType type,
        int orderIndex
    ) {
        return new TemplateParameter(
            name, description, type, true, null, 
            List.of(), Map.of(), null, orderIndex
        );
    }
    
    public static TemplateParameter optional(
        String name,
        String description,
        ParameterType type,
        Object defaultValue,
        int orderIndex
    ) {
        return new TemplateParameter(
            name, description, type, false, defaultValue, 
            List.of(), Map.of(), null, orderIndex
        );
    }
    
    public static TemplateParameter selection(
        String name,
        String description,
        List<String> allowedValues,
        String defaultValue,
        int orderIndex
    ) {
        return new TemplateParameter(
            name, description, ParameterType.SELECTION, 
            defaultValue == null, defaultValue, allowedValues, 
            Map.of(), null, orderIndex
        );
    }
}