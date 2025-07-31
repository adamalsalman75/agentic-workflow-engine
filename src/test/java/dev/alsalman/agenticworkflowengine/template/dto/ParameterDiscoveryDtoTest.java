package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterDiscoveryDtoTest {
    
    @Test
    void parameterResponseDto_FromParameter_ShouldMapCorrectly() {
        // Given - using simplified validation rules
        ValidationRule rule = ValidationRule.required("Parameter is required");
        Parameter parameter = new Parameter(
            "testParam", 
            "Test parameter description", 
            ParameterType.TEXT,
            true,
            null,
            List.of(rule)
        );
        
        // When
        ParameterResponseDto dto = ParameterResponseDto.fromParameter(parameter);
        
        // Then
        assertThat(dto.name()).isEqualTo("testParam");
        assertThat(dto.description()).isEqualTo("Test parameter description");
        assertThat(dto.type()).isEqualTo(ParameterType.TEXT);
        assertThat(dto.required()).isTrue();
        assertThat(dto.validation()).hasSize(1);
        assertThat(dto.validation().get(0).type()).isEqualTo("REQUIRED");
        assertThat(dto.validation().get(0).message()).isEqualTo("Parameter is required");
        assertThat(dto.metadata()).isNotNull();
        assertThat(dto.metadata().placeholder()).isEqualTo("Enter testparam");
    }
    
    @Test
    void validationRuleDto_FromValidationRule_ShouldMapAllowedValues() {
        // Given - using simplified ALLOWED_VALUES validation
        ValidationRule rule = ValidationRule.allowedValues(
            List.of("small", "medium", "large"),
            "Size must be small, medium, or large"
        );
        
        // When
        ValidationRuleDto dto = ValidationRuleDto.fromValidationRule(rule);
        
        // Then
        assertThat(dto.type()).isEqualTo("ALLOWED_VALUES");
        assertThat(dto.allowedValues()).containsExactly("small", "medium", "large");
        assertThat(dto.message()).isEqualTo("Size must be small, medium, or large");
    }
    
    @Test
    void validationRuleDto_FromValidationRule_ShouldMapRequired() {
        // Given
        ValidationRule rule = ValidationRule.required("Field is required");
        
        // When
        ValidationRuleDto dto = ValidationRuleDto.fromValidationRule(rule);
        
        // Then
        assertThat(dto.type()).isEqualTo("REQUIRED");
        assertThat(dto.allowedValues()).isNull();
        assertThat(dto.message()).isEqualTo("Field is required");
    }
    
    @Test
    void parameterMetadataDto_DefaultForParameter_ShouldCreateCorrectMetadata() {
        // Given - using TEXT type with smart placeholder detection
        Parameter locationParam = Parameter.required("business_location", "Where is your business located", ParameterType.TEXT);
        
        // When
        ParameterMetadataDto metadata = ParameterMetadataDto.defaultForParameter(locationParam);
        
        // Then
        assertThat(metadata.placeholder()).isEqualTo("San Francisco, CA");
        assertThat(metadata.helpText()).isEqualTo("Where is your business located (any location format)");
        assertThat(metadata.order()).isEqualTo(3); // location gets order 3
        assertThat(metadata.group()).isEqualTo("Location");
    }
    
    @Test
    void parameterDiscoveryResponseDto_Create_ShouldOrderParametersCorrectly() {
        // Given - using simplified parameter types
        UUID templateId = UUID.randomUUID();
        String templateName = "Test Template";
        List<Parameter> parameters = List.of(
            Parameter.required("funding_amount", "Budget amount", ParameterType.TEXT),
            Parameter.required("business_location", "Where to locate business", ParameterType.TEXT),
            Parameter.required("launch_date", "Launch date", ParameterType.TEXT)
        );
        
        // When
        ParameterDiscoveryResponseDto response = ParameterDiscoveryResponseDto.create(
            templateId, templateName, parameters
        );
        
        // Then
        assertThat(response.templateId()).isEqualTo(templateId);
        assertThat(response.templateName()).isEqualTo(templateName);
        assertThat(response.parameters()).hasSize(3);
        
        // Should be ordered by metadata.order(): location(3), date(4), amount(5)
        assertThat(response.parameters().get(0).name()).isEqualTo("business_location");
        assertThat(response.parameters().get(1).name()).isEqualTo("launch_date");
        assertThat(response.parameters().get(2).name()).isEqualTo("funding_amount");
    }
    
    @Test
    void parameterMetadataDto_ShouldHandleAllParameterTypes() {
        // Test different parameter types get correct placeholders
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("name", "Name", ParameterType.TEXT)
        ).placeholder()).isEqualTo("Enter name");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("amount", "Amount", ParameterType.NUMBER)
        ).placeholder()).isEqualTo("5");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("is_active", "Is Active", ParameterType.BOOLEAN)
        ).placeholder()).isEqualTo("false");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.optional("style", "Style", ParameterType.SELECTION, "Standard")
        ).placeholder()).isEqualTo("Standard");
    }
    
    @Test
    void parameterMetadataDto_ShouldDetectSmartPlaceholders() {
        // Test smart placeholder detection based on parameter names
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("business_email", "Email", ParameterType.TEXT)
        ).placeholder()).isEqualTo("user@example.com");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("launch_date", "Date", ParameterType.TEXT)
        ).placeholder()).isEqualTo("June 2025");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("funding_amount", "Budget", ParameterType.TEXT)
        ).placeholder()).isEqualTo("1000 USD");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("business_location", "Location", ParameterType.TEXT)
        ).placeholder()).isEqualTo("San Francisco, CA");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("contact_phone", "Phone", ParameterType.TEXT)
        ).placeholder()).isEqualTo("+1 234-567-8900");
    }
    
    @Test
    void selectionParameter_WithAllowedValues_ShouldValidateCorrectly() {
        // Given
        ValidationRule allowedValuesRule = ValidationRule.allowedValues(
            List.of("Technology", "Healthcare", "Finance"), 
            "Please select a valid industry"
        );
        Parameter parameter = new Parameter(
            "industry", 
            "Business industry", 
            ParameterType.SELECTION,
            true,
            "Technology",
            List.of(allowedValuesRule)
        );
        
        // When
        ParameterResponseDto dto = ParameterResponseDto.fromParameter(parameter);
        
        // Then
        assertThat(dto.type()).isEqualTo(ParameterType.SELECTION);
        assertThat(dto.validation()).hasSize(1);
        assertThat(dto.validation().get(0).type()).isEqualTo("ALLOWED_VALUES");
        assertThat(dto.validation().get(0).allowedValues()).containsExactly("Technology", "Healthcare", "Finance");
        assertThat(dto.defaultValue()).isEqualTo("Technology");
    }
}