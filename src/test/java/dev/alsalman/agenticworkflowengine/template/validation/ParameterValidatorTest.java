package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterValidatorTest {
    
    @Test
    void testRequiredParameterMissing() {
        Parameter param = Parameter.required("test", "Test param", ParameterType.TEXT);
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, null);
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsExactly("Required parameter 'test' is missing");
    }
    
    @Test
    void testOptionalParameterMissing() {
        Parameter param = Parameter.optional("test", "Test param", ParameterType.TEXT, "default");
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, null);
        
        assertThat(result.isValid()).isTrue();
    }
    
    // String validation tests
    @Test
    void testStringValidation() {
        Parameter param = Parameter.required("name", "Name", ParameterType.TEXT);
        
        assertThat(ParameterValidator.validate(param, "John Doe").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "").isValid()).isFalse();
        assertThat(ParameterValidator.validate(param, "   ").isValid()).isFalse();
    }
    
    // Number validation tests
    @Test
    void testNumberValidation() {
        Parameter param = Parameter.required("age", "Age", ParameterType.NUMBER);
        
        assertThat(ParameterValidator.validate(param, 25).isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "42").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "42.5").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "not a number").isValid()).isFalse();
    }
    
    // Date validation tests
    @ParameterizedTest
    @ValueSource(strings = {
        "2024-01-15",
        "01/15/2024",
        "15/01/2024"
    })
    void testDateValidation_ValidFormats(String date) {
        Parameter param = Parameter.required("date", "Date", ParameterType.DATE);
        
        assertThat(ParameterValidator.validate(param, date).isValid()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "2024-13-01",  // Invalid month
        "not a date",
        "2024/01/15",  // Wrong separator
        "15-01-2024"   // Wrong format
    })
    void testDateValidation_InvalidFormats(String date) {
        Parameter param = Parameter.required("date", "Date", ParameterType.DATE);
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, date);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0)).contains("must be a valid date");
    }
    
    // Currency validation tests
    @ParameterizedTest
    @ValueSource(strings = {
        "USD",
        "100 USD",
        "USD 100",
        "1000.50 EUR",
        "GBP 2500"
    })
    void testCurrencyValidation_ValidFormats(String currency) {
        Parameter param = Parameter.required("budget", "Budget", ParameterType.CURRENCY);
        
        assertThat(ParameterValidator.validate(param, currency).isValid()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "XYZ",          // Invalid currency code
        "100",          // No currency code
        "USD EUR",      // Two currency codes
        "ABC 100",      // Invalid currency code
        "100 200 USD"   // Too many parts
    })
    void testCurrencyValidation_InvalidFormats(String currency) {
        Parameter param = Parameter.required("budget", "Budget", ParameterType.CURRENCY);
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, currency);
        assertThat(result.isValid()).isFalse();
    }
    
    // Location validation tests
    @ParameterizedTest
    @ValueSource(strings = {
        "Paris",
        "New York City",
        "Paris, France",
        "San Francisco, CA",
        "Tokyo-Japan",
        "St. Petersburg"
    })
    void testLocationValidation_ValidFormats(String location) {
        Parameter param = Parameter.required("destination", "Destination", ParameterType.LOCATION);
        
        assertThat(ParameterValidator.validate(param, location).isValid()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "123456",       // Only numbers
        "Paris@France", // Invalid character
        "New York!"     // Invalid character
    })
    void testLocationValidation_InvalidFormats(String location) {
        Parameter param = Parameter.required("destination", "Destination", ParameterType.LOCATION);
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, location);
        assertThat(result.isValid()).isFalse();
    }
    
    // Selection validation tests
    @Test
    void testSelectionValidation() {
        Parameter param = Parameter.required("style", "Style", ParameterType.SELECTION);
        
        assertThat(ParameterValidator.validate(param, "Economy").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "Business").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "").isValid()).isFalse();
    }
}