package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterDiscoveryDtoTest {
    
    @Test
    void parameterResponseDto_FromParameter_ShouldMapCorrectly() {
        // Given
        ValidationRule rule = ValidationRule.range(BigDecimal.ONE, BigDecimal.TEN, "Must be between 1 and 10");
        Parameter parameter = Parameter.requiredWithValidation(
            "testParam", 
            "Test parameter description", 
            ParameterType.NUMBER,
            List.of(rule)
        );
        
        // When
        ParameterResponseDto dto = ParameterResponseDto.fromParameter(parameter);
        
        // Then
        assertThat(dto.name()).isEqualTo("testParam");
        assertThat(dto.description()).isEqualTo("Test parameter description");
        assertThat(dto.type()).isEqualTo(ParameterType.NUMBER);
        assertThat(dto.required()).isTrue();
        assertThat(dto.validation()).hasSize(1);
        assertThat(dto.validation().get(0).type()).isEqualTo("RANGE");
        assertThat(dto.validation().get(0).minValue()).isEqualTo(BigDecimal.ONE);
        assertThat(dto.validation().get(0).maxValue()).isEqualTo(BigDecimal.TEN);
        assertThat(dto.metadata()).isNotNull();
        assertThat(dto.metadata().placeholder()).isEqualTo("Enter a number");
    }
    
    @Test
    void validationRuleDto_FromValidationRule_ShouldMapAllFields() {
        // Given
        ValidationRule rule = ValidationRule.dateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            "Date must be in 2024"
        );
        
        // When
        ValidationRuleDto dto = ValidationRuleDto.fromValidationRule(rule);
        
        // Then
        assertThat(dto.type()).isEqualTo("DATE_RANGE");
        assertThat(dto.minDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(dto.maxDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(dto.message()).isEqualTo("Date must be in 2024");
        assertThat(dto.pattern()).isNull();
        assertThat(dto.allowedValues()).isNull();
    }
    
    @Test
    void parameterMetadataDto_DefaultForParameter_ShouldCreateCorrectMetadata() {
        // Given
        Parameter locationParam = Parameter.required("destination", "Where to go", ParameterType.LOCATION);
        
        // When
        ParameterMetadataDto metadata = ParameterMetadataDto.defaultForParameter(locationParam);
        
        // Then
        assertThat(metadata.placeholder()).isEqualTo("Paris, France");
        assertThat(metadata.helpText()).isEqualTo("Where to go (city and country recommended)");
        assertThat(metadata.order()).isEqualTo(1); // destination gets order 1
        assertThat(metadata.group()).isEqualTo("Location");
    }
    
    @Test
    void parameterDiscoveryResponseDto_Create_ShouldOrderParametersCorrectly() {
        // Given
        UUID templateId = UUID.randomUUID();
        String templateName = "Test Template";
        List<Parameter> parameters = List.of(
            Parameter.required("budget", "Budget amount", ParameterType.CURRENCY),
            Parameter.required("destination", "Where to travel", ParameterType.LOCATION),
            Parameter.required("startDate", "Departure date", ParameterType.DATE)
        );
        
        // When
        ParameterDiscoveryResponseDto response = ParameterDiscoveryResponseDto.create(
            templateId, templateName, parameters
        );
        
        // Then
        assertThat(response.templateId()).isEqualTo(templateId);
        assertThat(response.templateName()).isEqualTo(templateName);
        assertThat(response.parameters()).hasSize(3);
        
        // Should be ordered by metadata.order(): destination(1), startDate(2), budget(5)
        assertThat(response.parameters().get(0).name()).isEqualTo("destination");
        assertThat(response.parameters().get(1).name()).isEqualTo("startDate");
        assertThat(response.parameters().get(2).name()).isEqualTo("budget");
    }
    
    @Test
    void parameterMetadataDto_ShouldHandleAllParameterTypes() {
        // Test different parameter types get correct placeholders
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("name", "Name", ParameterType.STRING)
        ).placeholder()).isEqualTo("Enter name");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("amount", "Amount", ParameterType.NUMBER)
        ).placeholder()).isEqualTo("Enter a number");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("date", "Date", ParameterType.DATE)
        ).placeholder()).isEqualTo("yyyy-MM-dd");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.required("price", "Price", ParameterType.CURRENCY)
        ).placeholder()).isEqualTo("1000 USD");
        
        assertThat(ParameterMetadataDto.defaultForParameter(
            Parameter.optional("style", "Style", ParameterType.SELECTION, "Standard")
        ).placeholder()).isEqualTo("Standard");
    }
}