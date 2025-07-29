package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;

import java.util.List;

/**
 * Enhanced DTO for parameter discovery API with full metadata
 */
public record ParameterResponseDto(
    String name,
    String description,
    ParameterType type,
    boolean required,
    String defaultValue,
    List<ValidationRuleDto> validation,
    ParameterMetadataDto metadata
) {
    
    /**
     * Convert from domain Parameter to enhanced DTO
     */
    public static ParameterResponseDto fromParameter(Parameter parameter, ParameterMetadataDto metadata) {
        List<ValidationRuleDto> validationDtos = parameter.validationRules().stream()
            .map(ValidationRuleDto::fromValidationRule)
            .toList();
            
        return new ParameterResponseDto(
            parameter.name(),
            parameter.description(),
            parameter.type(),
            parameter.required(),
            parameter.defaultValue(),
            validationDtos,
            metadata
        );
    }
    
    /**
     * Convert from domain Parameter with default metadata
     */
    public static ParameterResponseDto fromParameter(Parameter parameter) {
        return fromParameter(parameter, ParameterMetadataDto.defaultForParameter(parameter));
    }
}