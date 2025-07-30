package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;

/**
 * Metadata for enhanced parameter discovery with UI hints
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
            case TEXT -> "Enter " + parameter.name().toLowerCase();
            case NUMBER -> "Enter a number";
            case DATE -> "yyyy-MM-dd";
            case CURRENCY -> "1000 USD";
            case LOCATION -> "Paris, France";
            case SELECTION -> parameter.defaultValue() != null ? parameter.defaultValue() : "Select an option";
            case BOOLEAN -> "true";
            case EMAIL -> "user@example.com";
            case URL -> "https://example.com";
            case PERCENTAGE -> "75";
            case PHONE -> "+1 234-567-8900";
            case TIME -> "14:30";
            case DURATION -> "2 hours";
        };
    }
    
    private static String getDefaultHelpText(Parameter parameter) {
        return switch (parameter.type()) {
            case TEXT -> parameter.description();
            case NUMBER -> parameter.description() + " (numeric value)";
            case DATE -> parameter.description() + " (format: yyyy-MM-dd, MM/dd/yyyy, or dd/MM/yyyy)";
            case CURRENCY -> parameter.description() + " (e.g., 1000 USD, EUR 500)";
            case LOCATION -> parameter.description() + " (city and country recommended)";
            case SELECTION -> parameter.description() + " (choose from available options)";
            case BOOLEAN -> parameter.description() + " (true/false)";
            case EMAIL -> parameter.description() + " (valid email address)";
            case URL -> parameter.description() + " (full URL including https://)";
            case PERCENTAGE -> parameter.description() + " (value between 0 and 100)";
            case PHONE -> parameter.description() + " (include country code)";
            case TIME -> parameter.description() + " (24-hour format HH:MM)";
            case DURATION -> parameter.description() + " (e.g., 2 hours, 30 minutes)";
        };
    }
    
    private static int getDefaultOrder(String parameterName) {
        // Define logical ordering for common travel template parameters
        return switch (parameterName.toLowerCase()) {
            case "destination" -> 1;
            case "startdate", "start_date" -> 2;
            case "enddate", "end_date" -> 3;
            case "duration" -> 4;
            case "budget" -> 5;
            case "travelstyle", "travel_style" -> 6;
            case "guests", "travelers" -> 7;
            default -> 10; // Default order for unknown parameters
        };
    }
    
    private static String getDefaultGroup(Parameter parameter) {
        return switch (parameter.name().toLowerCase()) {
            case "destination", "location" -> "Location";
            case "startdate", "start_date", "enddate", "end_date", "duration" -> "Dates";
            case "budget", "currency", "price" -> "Budget";
            case "travelstyle", "travel_style", "style", "preference" -> "Preferences";
            case "guests", "travelers", "adults", "children" -> "Travelers";
            default -> "General";
        };
    }
}