package dev.alsalman.agenticworkflowengine.template.validation;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedParameterValidatorTest {
    
    private AdvancedParameterValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new AdvancedParameterValidator();
    }
    
    @Test
    void testRequiredValidation_Success() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        Parameter param = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        
        List<String> errors = validator.validateParameter(param, "John Doe");
        
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testRequiredValidation_Failure_Null() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        Parameter param = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        
        List<String> errors = validator.validateParameter(param, null);
        
        assertThat(errors).containsExactly("Required parameter 'name' is missing");
    }
    
    @Test
    void testRequiredValidation_Failure_Empty() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        Parameter param = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        
        List<String> errors = validator.validateParameter(param, "");
        
        assertThat(errors).containsExactly("Required parameter 'name' cannot be empty");
    }
    
    @Test
    void testRequiredValidation_Failure_Blank() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        Parameter param = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        
        List<String> errors = validator.validateParameter(param, "   ");
        
        assertThat(errors).containsExactly("Required parameter 'name' cannot be empty");
    }
    
    @Test
    void testAllowedValuesValidation_Success() {
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, List.of(allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, "medium");
        
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testAllowedValuesValidation_Failure() {
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, List.of(allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, "extra-large");
        
        assertThat(errors).containsExactly("Parameter 'size' must be one of: small, medium, large");
    }
    
    @Test
    void testMultipleValidationRules_Success() {
        ValidationRule requiredRule = ValidationRule.required("Size is required");
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, 
            List.of(requiredRule, allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, "large");
        
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testMultipleValidationRules_BothFail() {
        ValidationRule requiredRule = ValidationRule.required("Size is required");
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, 
            List.of(requiredRule, allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, null);
        
        assertThat(errors).containsExactly("Required parameter 'size' is missing");
    }
    
    @Test
    void testMultipleValidationRules_OneFails() {
        ValidationRule requiredRule = ValidationRule.required("Size is required");
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, true, null, 
            List.of(requiredRule, allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, "invalid");
        
        assertThat(errors).containsExactly("Parameter 'size' must be one of: small, medium, large");
    }
    
    @Test
    void testOptionalParameter_NoValidation_WhenEmpty() {
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be small, medium, or large"
        );
        Parameter param = new Parameter("size", "Size", ParameterType.SELECTION, false, "medium", 
            List.of(allowedValuesRule));
        
        List<String> errors = validator.validateParameter(param, null);
        
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testTextParameter_LLMFlexibility() {
        // TEXT parameters should accept any format - trusting LLM flexibility
        Parameter param = new Parameter("description", "Description", ParameterType.TEXT, true, null, List.of());
        
        // Should accept email formats
        assertThat(validator.validateParameter(param, "contact@company.com")).isEmpty();
        
        // Should accept date formats
        assertThat(validator.validateParameter(param, "June 15, 2025")).isEmpty();
        
        // Should accept currency formats
        assertThat(validator.validateParameter(param, "$50,000 USD")).isEmpty();
        
        // Should accept location formats
        assertThat(validator.validateParameter(param, "San Francisco, CA")).isEmpty();
    }
    
    @Test
    void testNumberParameter_BasicValidation() {
        Parameter param = new Parameter("count", "Count", ParameterType.NUMBER, true, null, List.of());
        
        assertThat(validator.validateParameter(param, "42")).isEmpty();
        assertThat(validator.validateParameter(param, "42.5")).isEmpty();
        assertThat(validator.validateParameter(param, "-10")).isEmpty();
        
        List<String> errors = validator.validateParameter(param, "not-a-number");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("must be a valid number");
    }
    
    @Test
    void testBooleanParameter_FlexibleFormats() {
        Parameter param = new Parameter("enabled", "Enabled", ParameterType.BOOLEAN, true, null, List.of());
        
        assertThat(validator.validateParameter(param, "true")).isEmpty();
        assertThat(validator.validateParameter(param, "false")).isEmpty();
        assertThat(validator.validateParameter(param, "yes")).isEmpty();
        assertThat(validator.validateParameter(param, "no")).isEmpty();
        assertThat(validator.validateParameter(param, "1")).isEmpty();
        assertThat(validator.validateParameter(param, "0")).isEmpty();
        
        List<String> errors = validator.validateParameter(param, "maybe");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("must be true/false, yes/no, or 1/0");
    }
    
    @Test
    void testValidateMultipleParameters() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        ValidationRule sizeRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be valid"
        );
        
        Parameter nameParam = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        Parameter sizeParam = new Parameter("size", "Size", ParameterType.SELECTION, true, null, List.of(sizeRule));
        
        List<Parameter> parameters = List.of(nameParam, sizeParam);
        Map<String, String> values = Map.of(
            "name", "John Doe",
            "size", "medium"
        );
        
        List<String> errors = validator.validateParameters(parameters, values);
        
        assertThat(errors).isEmpty();
    }
    
    @Test
    void testValidateMultipleParameters_WithErrors() {
        ValidationRule requiredRule = ValidationRule.required("Name is required");
        ValidationRule sizeRule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"), 
            "Size must be valid"
        );
        
        Parameter nameParam = new Parameter("name", "Name", ParameterType.TEXT, true, null, List.of(requiredRule));
        Parameter sizeParam = new Parameter("size", "Size", ParameterType.SELECTION, true, null, List.of(sizeRule));
        
        List<Parameter> parameters = List.of(nameParam, sizeParam);
        Map<String, String> values = Map.of(
            "size", "invalid"
            // name is missing
        );
        
        List<String> errors = validator.validateParameters(parameters, values);
        
        assertThat(errors).hasSize(2);
        assertThat(errors).contains("Required parameter 'name' is missing");
        assertThat(errors).contains("Parameter 'size' must be one of: small, medium, large");
    }
}