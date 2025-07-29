package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedParameterValidatorTest {

    private AdvancedParameterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AdvancedParameterValidator();
    }

    @Nested
    @DisplayName("Pattern Validation Tests")
    class PatternValidationTests {
        
        @Test
        void shouldValidateStringMatchingPattern() {
            // Given
            var rule = ValidationRule.pattern("^[A-Z]{3}$", "Must be 3 uppercase letters");
            var parameter = Parameter.requiredWithValidation(
                "airportCode", "Airport code", ParameterType.STRING, List.of(rule)
            );
            
            // Basic validation will pass for valid string
            
            // When
            List<String> errors = validator.validateParameter(parameter, "LAX");
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldRejectStringNotMatchingPattern() {
            // Given
            var rule = ValidationRule.pattern("^[A-Z]{3}$", "Must be 3 uppercase letters");
            var parameter = Parameter.requiredWithValidation(
                "airportCode", "Airport code", ParameterType.STRING, List.of(rule)
            );
            
            // Basic validation will pass for valid string
            
            // When
            List<String> errors = validator.validateParameter(parameter, "la");
            
            // Then
            assertThat(errors).containsExactly("Must be 3 uppercase letters");
        }
        
        @Test
        void shouldUseDefaultMessageWhenCustomMessageNotProvided() {
            // Given
            var rule = ValidationRule.pattern("^[A-Z]{3}$", null);
            var parameter = Parameter.requiredWithValidation(
                "code", "Code", ParameterType.STRING, List.of(rule)
            );
            
            // Basic validation will pass for valid string
            
            // When
            List<String> errors = validator.validateParameter(parameter, "123");
            
            // Then
            assertThat(errors).containsExactly("Value must match pattern: ^[A-Z]{3}$");
        }
    }

    @Nested
    @DisplayName("Number Range Validation Tests")
    class NumberRangeValidationTests {
        
        @Test
        void shouldValidateNumberWithinRange() {
            // Given
            var rule = ValidationRule.range(
                new BigDecimal("1"), 
                new BigDecimal("365"), 
                "Duration must be between 1 and 365 days"
            );
            var parameter = Parameter.requiredWithValidation(
                "duration", "Duration in days", ParameterType.NUMBER, List.of(rule)
            );
            
            // Basic validation will pass for valid number
            
            // When
            List<String> errors = validator.validateParameter(parameter, "30");
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldRejectNumberBelowMinimum() {
            // Given
            var rule = ValidationRule.range(
                new BigDecimal("1"), 
                new BigDecimal("365"), 
                "Duration must be between 1 and 365 days"
            );
            var parameter = Parameter.requiredWithValidation(
                "duration", "Duration in days", ParameterType.NUMBER, List.of(rule)
            );
            
            // Basic validation will pass for valid number
            
            // When
            List<String> errors = validator.validateParameter(parameter, "0");
            
            // Then
            assertThat(errors).containsExactly("Duration must be between 1 and 365 days");
        }
        
        @Test
        void shouldRejectNumberAboveMaximum() {
            // Given
            var rule = ValidationRule.range(
                new BigDecimal("1"), 
                new BigDecimal("365"), 
                null
            );
            var parameter = Parameter.requiredWithValidation(
                "duration", "Duration in days", ParameterType.NUMBER, List.of(rule)
            );
            
            // Basic validation will pass for valid number
            
            // When
            List<String> errors = validator.validateParameter(parameter, "400");
            
            // Then
            assertThat(errors).containsExactly("Value must be at most 365");
        }
        
        @Test
        void shouldValidateWithOnlyMinimum() {
            // Given
            var rule = ValidationRule.range(new BigDecimal("0"), null, null);
            var parameter = Parameter.requiredWithValidation(
                "age", "Age", ParameterType.NUMBER, List.of(rule)
            );
            
            // Basic validation will pass for valid number
            
            // When
            List<String> errors = validator.validateParameter(parameter, "25");
            
            // Then
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("Date Range Validation Tests")
    class DateRangeValidationTests {
        
        @Test
        void shouldValidateDateWithinRange() {
            // Given
            var rule = ValidationRule.dateRange(
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                "Date must be within the next year"
            );
            var parameter = Parameter.requiredWithValidation(
                "departureDate", "Departure date", ParameterType.DATE, List.of(rule)
            );
            
            // Basic validation will pass
            String futureDate = LocalDate.now().plusMonths(3).toString();
            
            // When
            List<String> errors = validator.validateParameter(parameter, futureDate);
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldRejectDateBeforeMinimum() {
            // Given
            var rule = ValidationRule.dateRange(
                LocalDate.now(),
                null,
                "Date cannot be in the past"
            );
            var parameter = Parameter.requiredWithValidation(
                "eventDate", "Event date", ParameterType.DATE, List.of(rule)
            );
            
            // Basic validation will pass
            String pastDate = LocalDate.now().minusDays(1).toString();
            
            // When
            List<String> errors = validator.validateParameter(parameter, pastDate);
            
            // Then
            assertThat(errors).containsExactly("Date cannot be in the past");
        }
        
        @Test
        void shouldRejectDateAfterMaximum() {
            // Given
            var rule = ValidationRule.dateRange(
                null,
                LocalDate.now().plusDays(30),
                null
            );
            var parameter = Parameter.requiredWithValidation(
                "deadline", "Deadline", ParameterType.DATE, List.of(rule)
            );
            
            // Basic validation will pass
            String futureDate = LocalDate.now().plusDays(31).toString();
            
            // When
            List<String> errors = validator.validateParameter(parameter, futureDate);
            
            // Then
            assertThat(errors).containsExactly("Date must be on or before " + LocalDate.now().plusDays(30));
        }
    }

    @Nested
    @DisplayName("Allowed Values Validation Tests")
    class AllowedValuesValidationTests {
        
        @Test
        void shouldAcceptAllowedValue() {
            // Given
            var rule = ValidationRule.allowedValues(
                List.of("Economy", "Business", "First"),
                "Invalid travel class"
            );
            var parameter = Parameter.requiredWithValidation(
                "travelClass", "Travel class", ParameterType.SELECTION, List.of(rule)
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "Business");
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldRejectDisallowedValue() {
            // Given
            var rule = ValidationRule.allowedValues(
                List.of("Economy", "Business", "First"),
                "Invalid travel class"
            );
            var parameter = Parameter.requiredWithValidation(
                "travelClass", "Travel class", ParameterType.SELECTION, List.of(rule)
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "Premium");
            
            // Then
            assertThat(errors).containsExactly("Invalid travel class");
        }
        
        @Test
        void shouldProvideDefaultMessageForDisallowedValue() {
            // Given
            var rule = ValidationRule.allowedValues(
                List.of("Low", "Medium", "High"),
                null
            );
            var parameter = Parameter.requiredWithValidation(
                "priority", "Priority", ParameterType.SELECTION, List.of(rule)
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "Critical");
            
            // Then
            assertThat(errors).containsExactly("Value must be one of: Low, Medium, High");
        }
    }

    @Nested
    @DisplayName("Multiple Validation Rules Tests")
    class MultipleValidationRulesTests {
        
        @Test
        void shouldValidateAllRules() {
            // Given
            var rules = List.of(
                ValidationRule.pattern("^\\d+$", "Must contain only digits"),
                ValidationRule.range(
                    new BigDecimal("1"), 
                    new BigDecimal("999"), 
                    "Must be between 1 and 999"
                )
            );
            var parameter = Parameter.requiredWithValidation(
                "code", "Numeric code", ParameterType.STRING, rules
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "abc");
            
            // Then
            assertThat(errors).containsExactly("Must contain only digits");
        }
        
        @Test
        void shouldCollectAllValidationErrors() {
            // Given
            var rules = List.of(
                ValidationRule.pattern("^[A-Z]+$", "Must be uppercase"),
                ValidationRule.pattern("^.{3}$", "Must be exactly 3 characters")
            );
            var parameter = Parameter.requiredWithValidation(
                "code", "Code", ParameterType.STRING, rules
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "ab");
            
            // Then
            assertThat(errors).containsExactlyInAnyOrder(
                "Must be uppercase",
                "Must be exactly 3 characters"
            );
        }
    }

    @Nested
    @DisplayName("Basic Validation Integration Tests")
    class BasicValidationIntegrationTests {
        
        @Test
        void shouldReturnBasicValidationErrorsWhenBasicValidationFails() {
            // Given
            var parameter = Parameter.required("name", "Name", ParameterType.STRING);
            
            // When passing null to a required parameter
            List<String> errors = validator.validateParameter(parameter, null);
            
            // Then basic validation should fail
            assertThat(errors).containsExactly("Required parameter 'name' is missing");
        }
        
        @Test
        void shouldSkipAdvancedValidationWhenBasicValidationFails() {
            // Given
            var rule = ValidationRule.pattern("^[A-Z]+$", "Must be uppercase");
            var parameter = Parameter.requiredWithValidation(
                "code", "Code", ParameterType.STRING, List.of(rule)
            );
            
            // When passing null to a required parameter
            List<String> errors = validator.validateParameter(parameter, null);
            
            // Then basic validation should fail, advanced validation should not run
            assertThat(errors).containsExactly("Required parameter 'code' is missing");
            // Advanced validation pattern check should not run
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {
        
        @Test
        void shouldHandleEmptyValidationRulesList() {
            // Given
            var parameter = Parameter.required("field", "Field", ParameterType.STRING);
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "value");
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldSkipValidationForOptionalEmptyValues() {
            // Given
            var rule = ValidationRule.pattern("^[A-Z]+$", "Must be uppercase");
            var parameter = Parameter.optionalWithValidation(
                "optionalCode", "Optional code", ParameterType.STRING, null, List.of(rule)
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "");
            
            // Then
            assertThat(errors).isEmpty();
        }
        
        @Test
        void shouldHandleInvalidRegexPattern() {
            // Given
            var rule = ValidationRule.pattern("[", "Invalid pattern");  // Invalid regex
            var parameter = Parameter.requiredWithValidation(
                "field", "Field", ParameterType.STRING, List.of(rule)
            );
            
            // Basic validation will pass
            
            // When
            List<String> errors = validator.validateParameter(parameter, "value");
            
            // Then
            assertThat(errors).containsExactly("Invalid validation pattern configured");
        }
    }
}