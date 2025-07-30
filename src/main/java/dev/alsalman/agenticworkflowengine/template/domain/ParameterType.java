package dev.alsalman.agenticworkflowengine.template.domain;

/**
 * Simplified parameter types optimized for LLM consumption.
 * LLMs can naturally handle flexible inputs, so we focus on 4 core types
 * that provide clear user guidance while trusting LLM flexibility.
 */
public enum ParameterType {
    TEXT,        // Free text input - LLMs handle emails, dates, locations naturally
    NUMBER,      // Numeric values - LLMs parse currencies, percentages, durations
    BOOLEAN,     // True/false values
    SELECTION    // Multiple choice from predefined options
}