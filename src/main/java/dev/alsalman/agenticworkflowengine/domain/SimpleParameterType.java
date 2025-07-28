package dev.alsalman.agenticworkflowengine.domain;

/**
 * Parameter types for template execution
 * Phase 1: STRING, NUMBER, SELECTION
 * Phase 2: DATE, CURRENCY, LOCATION
 */
public enum SimpleParameterType {
    STRING,
    NUMBER,
    SELECTION,
    DATE,
    CURRENCY,
    LOCATION
}