package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;

/**
 * Simplified metadata for parameter discovery optimized for LLM consumption
 */
public record ParameterMetadataDto(
    String placeholder,
    String helpText,
    int order,
    String group
) {
    
    /**
     * Create default metadata for a parameter based on its type and name
     */
    public static ParameterMetadataDto defaultForParameter(Parameter parameter) {
        String placeholder = getDefaultPlaceholder(parameter);
        String helpText = getDefaultHelpText(parameter);
        int order = getDefaultOrder(parameter.name());
        String group = getDefaultGroup(parameter);
        
        return new ParameterMetadataDto(placeholder, helpText, order, group);
    }
    
    private static String getDefaultPlaceholder(Parameter parameter) {
        return switch (parameter.type()) {
            case TEXT -> determineTextPlaceholder(parameter.name());
            case NUMBER -> "5";
            case BOOLEAN -> "false";
            case SELECTION -> parameter.defaultValue() != null ? parameter.defaultValue() : "Select an option";
        };
    }
    
    private static String determineTextPlaceholder(String paramName) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("email")) return "user@example.com";
        if (lowerName.contains("date")) return "June 2025";
        if (lowerName.contains("location") || lowerName.contains("address")) return "San Francisco, CA";
        if (lowerName.contains("budget") || lowerName.contains("amount") || lowerName.contains("cost")) return "1000 USD";
        if (lowerName.contains("phone")) return "+1 234-567-8900";
        if (lowerName.contains("url") || lowerName.contains("website")) return "https://example.com";
        return "Enter " + paramName.toLowerCase().replace("_", " ");
    }
    
    private static String getDefaultHelpText(Parameter parameter) {
        return switch (parameter.type()) {
            case TEXT -> determineTextHelpText(parameter.name(), parameter.description());
            case NUMBER -> parameter.description() + " (numeric value)";
            case BOOLEAN -> parameter.description() + " (true/false)";
            case SELECTION -> parameter.description() + " (choose from available options)";
        };
    }
    
    private static String determineTextHelpText(String paramName, String description) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("email")) return description + " (any email format)";
        if (lowerName.contains("date")) return description + " (any date format)";
        if (lowerName.contains("location") || lowerName.contains("address")) return description + " (any location format)";
        if (lowerName.contains("budget") || lowerName.contains("amount") || lowerName.contains("cost")) return description + " (any currency format)";
        if (lowerName.contains("phone")) return description + " (any phone format)";
        return description;
    }
    
    private static int getDefaultOrder(String parameterName) {
        // Simple ordering logic
        String lowerName = parameterName.toLowerCase();
        if (lowerName.contains("name") || lowerName.contains("title")) return 1;
        if (lowerName.contains("type") || lowerName.contains("category")) return 2;
        if (lowerName.contains("location") || lowerName.contains("address")) return 3;
        if (lowerName.contains("date") || lowerName.contains("time")) return 4;
        if (lowerName.contains("budget") || lowerName.contains("amount")) return 5;
        if (lowerName.contains("email") || lowerName.contains("contact")) return 6;
        return 10; // Default order
    }
    
    private static String getDefaultGroup(Parameter parameter) {
        String lowerName = parameter.name().toLowerCase();
        
        if (lowerName.contains("name") || lowerName.contains("title") || lowerName.contains("type")) {
            return "Basic Info";
        }
        if (lowerName.contains("location") || lowerName.contains("address")) {
            return "Location";
        }
        if (lowerName.contains("date") || lowerName.contains("time")) {
            return "Schedule";
        }
        if (lowerName.contains("budget") || lowerName.contains("amount") || lowerName.contains("cost")) {
            return "Budget";
        }
        if (lowerName.contains("email") || lowerName.contains("contact") || lowerName.contains("phone")) {
            return "Contact";
        }
        if (parameter.type() == ParameterType.SELECTION) {
            return "Options";
        }
        
        return "General";
    }
}