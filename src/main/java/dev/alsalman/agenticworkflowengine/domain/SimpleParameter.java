package dev.alsalman.agenticworkflowengine.domain;

/**
 * Simple parameter definition for Phase 1
 */
public record SimpleParameter(
    String name,
    String description,
    SimpleParameterType type,
    boolean required,
    String defaultValue
) {
    
    public static SimpleParameter required(String name, String description, SimpleParameterType type) {
        return new SimpleParameter(name, description, type, true, null);
    }
    
    public static SimpleParameter optional(String name, String description, SimpleParameterType type, String defaultValue) {
        return new SimpleParameter(name, description, type, false, defaultValue);
    }
}