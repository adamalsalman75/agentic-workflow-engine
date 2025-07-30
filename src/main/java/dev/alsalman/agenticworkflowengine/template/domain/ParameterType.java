package dev.alsalman.agenticworkflowengine.template.domain;

/**
 * Parameter types for template execution
 * Phase 1: TEXT, NUMBER, SELECTION
 * Phase 2: DATE, CURRENCY, LOCATION
 * Phase 3: BOOLEAN, EMAIL, URL, PERCENTAGE, PHONE, TIME, DURATION
 */
public enum ParameterType {
    TEXT,       // Changed from STRING for consistency
    NUMBER,
    SELECTION,
    DATE,
    CURRENCY,
    LOCATION,
    BOOLEAN,
    EMAIL,
    URL,
    PERCENTAGE,
    PHONE,
    TIME,
    DURATION
}