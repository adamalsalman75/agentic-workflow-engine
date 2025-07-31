package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

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
    
    // TEXT validation tests - LLM-friendly, accepts anything
    @Test
    void testTextValidation() {
        Parameter param = Parameter.required("name", "Name", ParameterType.TEXT);
        
        // TEXT type should accept any format - trusting LLM flexibility
        assertThat(ParameterValidator.validate(param, "John Doe").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "john.doe@company.com").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "San Francisco, CA").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "June 15, 2025").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "$50,000 USD").isValid()).isTrue();
        
        // Only fail on empty required fields
        assertThat(ParameterValidator.validate(param, "").isValid()).isFalse();
        assertThat(ParameterValidator.validate(param, "   ").isValid()).isFalse();
    }
    
    // NUMBER validation tests
    @Test
    void testNumberValidation() {
        Parameter param = Parameter.required("age", "Age", ParameterType.NUMBER);
        
        assertThat(ParameterValidator.validate(param, 25).isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "42").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "42.5").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "-10").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "0").isValid()).isTrue();
        
        assertThat(ParameterValidator.validate(param, "not a number").isValid()).isFalse();
        assertThat(ParameterValidator.validate(param, "").isValid()).isFalse();
    }
    
    // BOOLEAN validation tests
    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "TRUE", "FALSE", "yes", "no", "YES", "NO", "1", "0"})
    void testBooleanValidation_ValidFormats(String value) {
        Parameter param = Parameter.required("flag", "Flag", ParameterType.BOOLEAN);
        
        assertThat(ParameterValidator.validate(param, value).isValid()).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"maybe", "2", "on", "off", "enabled", "disabled"})
    void testBooleanValidation_InvalidFormats(String value) {
        Parameter param = Parameter.required("flag", "Flag", ParameterType.BOOLEAN);
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, value);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0)).contains("must be true/false, yes/no, or 1/0");
    }
    
    // SELECTION validation tests
    @Test
    void testSelectionValidation_WithValidValues() {
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, List.of(allowedValuesRule));
        
        assertThat(ParameterValidator.validate(param, "small").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "medium").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "large").isValid()).isTrue();
        
        ParameterValidator.ValidationResult result = ParameterValidator.validate(param, "extra-large");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().get(0)).contains("must be one of: small, medium, large");
    }
    
    @Test
    void testSelectionValidation_WithoutValidationRule() {
        // SELECTION without ALLOWED_VALUES rule should accept anything
        Parameter param = Parameter.required("category", "Category", ParameterType.SELECTION);
        
        assertThat(ParameterValidator.validate(param, "any-value").isValid()).isTrue();
        assertThat(ParameterValidator.validate(param, "anything").isValid()).isTrue();
    }
}