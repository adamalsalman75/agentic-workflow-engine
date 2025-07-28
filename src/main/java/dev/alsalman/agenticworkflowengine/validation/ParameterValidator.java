package dev.alsalman.agenticworkflowengine.validation;

import dev.alsalman.agenticworkflowengine.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.domain.SimpleParameterType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;

/**
 * Validates parameter values based on their type
 */
public class ParameterValidator {
    
    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD"
    );
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }
        
        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
    }
    
    public static ValidationResult validate(SimpleParameter parameter, Object value) {
        if (value == null) {
            if (parameter.required()) {
                return ValidationResult.failure("Required parameter '" + parameter.name() + "' is missing");
            }
            return ValidationResult.success();
        }
        
        return switch (parameter.type()) {
            case STRING -> validateString(parameter, value.toString());
            case NUMBER -> validateNumber(parameter, value);
            case SELECTION -> validateSelection(parameter, value.toString());
            case DATE -> validateDate(parameter, value.toString());
            case CURRENCY -> validateCurrency(parameter, value.toString());
            case LOCATION -> validateLocation(parameter, value.toString());
        };
    }
    
    private static ValidationResult validateString(SimpleParameter parameter, String value) {
        if (value.trim().isEmpty() && parameter.required()) {
            return ValidationResult.failure("Required parameter '" + parameter.name() + "' cannot be empty");
        }
        return ValidationResult.success();
    }
    
    private static ValidationResult validateNumber(SimpleParameter parameter, Object value) {
        try {
            if (value instanceof Number) {
                return ValidationResult.success();
            }
            Double.parseDouble(value.toString());
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Parameter '" + parameter.name() + "' must be a valid number");
        }
    }
    
    private static ValidationResult validateSelection(SimpleParameter parameter, String value) {
        // For now, any non-empty string is valid for selection
        // In Phase 2 Story 2, we'll add allowed values validation
        if (value.trim().isEmpty() && parameter.required()) {
            return ValidationResult.failure("Required parameter '" + parameter.name() + "' must have a selection");
        }
        return ValidationResult.success();
    }
    
    private static ValidationResult validateDate(SimpleParameter parameter, String value) {
        // Try multiple date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(value, formatter);
                return ValidationResult.success();
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        
        return ValidationResult.failure(
            "Parameter '" + parameter.name() + "' must be a valid date. " +
            "Supported formats: yyyy-MM-dd, MM/dd/yyyy, dd/MM/yyyy"
        );
    }
    
    private static ValidationResult validateCurrency(SimpleParameter parameter, String value) {
        // Format: "100 USD" or "USD 100" or just "USD"
        String[] parts = value.trim().split("\\s+");
        
        if (parts.length == 0 || parts.length > 2) {
            return ValidationResult.failure(
                "Parameter '" + parameter.name() + "' must be in format 'amount currency' or 'currency amount'"
            );
        }
        
        String currencyCode = null;
        String amount = null;
        
        if (parts.length == 1) {
            // Just currency code
            currencyCode = parts[0];
        } else {
            // Try to determine which is currency and which is amount
            if (VALID_CURRENCY_CODES.contains(parts[0].toUpperCase())) {
                currencyCode = parts[0];
                amount = parts[1];
            } else if (VALID_CURRENCY_CODES.contains(parts[1].toUpperCase())) {
                amount = parts[0];
                currencyCode = parts[1];
            } else {
                return ValidationResult.failure(
                    "Parameter '" + parameter.name() + "' must include a valid currency code"
                );
            }
        }
        
        // Validate currency code
        if (!VALID_CURRENCY_CODES.contains(currencyCode.toUpperCase())) {
            return ValidationResult.failure(
                "Parameter '" + parameter.name() + "' has invalid currency code. " +
                "Supported: " + String.join(", ", VALID_CURRENCY_CODES)
            );
        }
        
        // Validate amount if present
        if (amount != null) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException e) {
                return ValidationResult.failure(
                    "Parameter '" + parameter.name() + "' has invalid amount: " + amount
                );
            }
        }
        
        return ValidationResult.success();
    }
    
    private static ValidationResult validateLocation(SimpleParameter parameter, String value) {
        // Basic location validation - just check it's not empty
        // Could be enhanced with geocoding API integration
        if (value.trim().isEmpty() && parameter.required()) {
            return ValidationResult.failure("Required parameter '" + parameter.name() + "' cannot be empty");
        }
        
        // Simple validation: must contain at least letters and can have spaces, commas
        if (!value.matches("^[a-zA-Z\\s,.-]+$")) {
            return ValidationResult.failure(
                "Parameter '" + parameter.name() + "' must be a valid location name"
            );
        }
        
        return ValidationResult.success();
    }
}