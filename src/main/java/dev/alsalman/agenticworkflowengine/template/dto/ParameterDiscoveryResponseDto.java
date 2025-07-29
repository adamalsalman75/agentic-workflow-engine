package dev.alsalman.agenticworkflowengine.template.dto;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;

import java.util.List;
import java.util.UUID;

/**
 * Complete response for parameter discovery API
 */
public record ParameterDiscoveryResponseDto(
    UUID templateId,
    String templateName,
    List<ParameterResponseDto> parameters
) {
    
    /**
     * Create discovery response from template info and parameters
     */
    public static ParameterDiscoveryResponseDto create(UUID templateId, String templateName, List<Parameter> parameters) {
        List<ParameterResponseDto> parameterDtos = parameters.stream()
            .map(ParameterResponseDto::fromParameter)
            .sorted((a, b) -> Integer.compare(a.metadata().order(), b.metadata().order()))
            .toList();
            
        return new ParameterDiscoveryResponseDto(templateId, templateName, parameterDtos);
    }
}