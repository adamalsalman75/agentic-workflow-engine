package dev.alsalman.agenticworkflowengine.template.service;

import dev.alsalman.agenticworkflowengine.template.domain.*;
import dev.alsalman.agenticworkflowengine.template.dto.ParameterResponseDto;
import dev.alsalman.agenticworkflowengine.template.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParameterPersistenceServiceTest {
    
    @Mock
    private TemplateParameterRepository parameterRepository;
    
    @Mock
    private ParameterMetadataRepository metadataRepository;
    
    @Mock
    private ParameterValidationRuleRepository validationRuleRepository;
    
    private ParameterPersistenceService service;
    
    @BeforeEach
    void setUp() {
        service = new ParameterPersistenceService(
            parameterRepository,
            metadataRepository,
            validationRuleRepository
        );
    }
    
    @Test
    void saveTemplateParameters_ShouldSaveParametersWithMetadataAndValidation() {
        // Given
        UUID templateId = UUID.randomUUID();
        List<Parameter> parameters = Arrays.asList(
            Parameter.requiredWithValidation(
                "destination", 
                "Where are you traveling to?", 
                ParameterType.LOCATION,
                List.of(ValidationRule.pattern("^[A-Za-z\\s,.-]+$", "Invalid location format"))
            ),
            Parameter.optional(
                "budget", 
                "Travel budget", 
                ParameterType.CURRENCY, 
                "1000 USD"
            )
        );
        
        when(parameterRepository.save(any(TemplateParameter.class)))
            .thenAnswer(invocation -> {
                TemplateParameter param = invocation.getArgument(0);
                return new TemplateParameter(
                    UUID.randomUUID(),
                    param.templateId(),
                    param.name(),
                    param.description(),
                    param.type(),
                    param.required(),
                    param.defaultValue(),
                    param.displayOrder(),
                    param.createdAt()
                );
            });
        
        // When
        service.saveTemplateParameters(templateId, parameters);
        
        // Then
        verify(parameterRepository).deleteByTemplateId(templateId);
        verify(parameterRepository, times(2)).save(any(TemplateParameter.class));
        verify(metadataRepository, times(2)).save(any(ParameterMetadata.class));
        verify(validationRuleRepository, times(1)).save(any(ParameterValidationRule.class));
    }
    
    @Test
    void loadTemplateParameters_ShouldReturnParametersWithValidationRules() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID paramId = UUID.randomUUID();
        
        TemplateParameter templateParam = new TemplateParameter(
            paramId,
            templateId,
            "startDate",
            "Departure date",
            ParameterType.DATE.name(),
            true,
            null,
            0,
            Instant.now()
        );
        
        ParameterValidationRule validationRule = new ParameterValidationRule(
            UUID.randomUUID(),
            paramId,
            "DATE_RANGE",
            "{\"min\": \"" + LocalDate.now().toString() + "\"}",
            "Date cannot be in the past",
            Instant.now()
        );
        
        when(parameterRepository.findByTemplateIdOrderByDisplayOrder(templateId))
            .thenReturn(List.of(templateParam));
        when(validationRuleRepository.findByParameterId(paramId))
            .thenReturn(List.of(validationRule));
        
        // When
        List<Parameter> result = service.loadTemplateParameters(templateId);
        
        // Then
        assertEquals(1, result.size());
        Parameter param = result.get(0);
        assertEquals("startDate", param.name());
        assertEquals(ParameterType.DATE, param.type());
        assertTrue(param.required());
        assertEquals(1, param.validationRules().size());
        
        ValidationRule rule = param.validationRules().get(0);
        assertEquals(ValidationRule.ValidationRuleType.DATE_RANGE, rule.type());
        assertEquals(LocalDate.now(), rule.minDate());
        assertNull(rule.maxDate());
    }
    
    @Test
    void loadParametersWithMetadata_ShouldReturnFullParameterResponseDtos() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID paramId = UUID.randomUUID();
        
        TemplateParameter templateParam = new TemplateParameter(
            paramId,
            templateId,
            "destination",
            "Where are you traveling to?",
            ParameterType.LOCATION.name(),
            true,
            null,
            0,
            Instant.now()
        );
        
        ParameterMetadata metadata = new ParameterMetadata(
            UUID.randomUUID(),
            paramId,
            "Paris, France",
            "Enter a city, state, or country",
            "Location",
            null,
            null,
            Instant.now()
        );
        
        ParameterValidationRule validationRule = new ParameterValidationRule(
            UUID.randomUUID(),
            paramId,
            "PATTERN",
            "{\"pattern\": \"^[A-Za-z\\\\s,.-]+$\"}",
            "Invalid location format",
            Instant.now()
        );
        
        when(parameterRepository.findByTemplateIdOrderByDisplayOrder(templateId))
            .thenReturn(List.of(templateParam));
        when(metadataRepository.findByParameterId(paramId))
            .thenReturn(Optional.of(metadata));
        when(validationRuleRepository.findByParameterId(paramId))
            .thenReturn(List.of(validationRule));
        
        // When
        List<ParameterResponseDto> result = service.loadParametersWithMetadata(templateId);
        
        // Then
        assertEquals(1, result.size());
        ParameterResponseDto dto = result.get(0);
        assertEquals("destination", dto.name());
        assertEquals(ParameterType.LOCATION, dto.type());
        assertTrue(dto.required());
        
        assertNotNull(dto.metadata());
        assertEquals("Paris, France", dto.metadata().placeholder());
        assertEquals("Enter a city, state, or country", dto.metadata().helpText());
        assertEquals("Location", dto.metadata().group());
        assertEquals(1, dto.metadata().order());
        
        assertEquals(1, dto.validation().size());
        var validationDto = dto.validation().get(0);
        assertEquals("PATTERN", validationDto.type());
        assertEquals("^[A-Za-z\\s,.-]+$", validationDto.pattern());
    }
    
    @Test
    void saveTemplateParameters_WithNoValidationRules_ShouldOnlySaveParameterAndMetadata() {
        // Given
        UUID templateId = UUID.randomUUID();
        List<Parameter> parameters = List.of(
            Parameter.optional("notes", "Additional notes", ParameterType.TEXT, null)
        );
        
        when(parameterRepository.save(any(TemplateParameter.class)))
            .thenAnswer(invocation -> {
                TemplateParameter param = invocation.getArgument(0);
                return new TemplateParameter(
                    UUID.randomUUID(),
                    param.templateId(),
                    param.name(),
                    param.description(),
                    param.type(),
                    param.required(),
                    param.defaultValue(),
                    param.displayOrder(),
                    param.createdAt()
                );
            });
        
        // When
        service.saveTemplateParameters(templateId, parameters);
        
        // Then
        verify(parameterRepository).save(any(TemplateParameter.class));
        verify(metadataRepository).save(any(ParameterMetadata.class));
        verify(validationRuleRepository, never()).save(any(ParameterValidationRule.class));
    }
    
    @Test
    void loadParametersWithMetadata_WithNoMetadata_ShouldReturnNullMetadata() {
        // Given
        UUID templateId = UUID.randomUUID();
        UUID paramId = UUID.randomUUID();
        
        TemplateParameter templateParam = new TemplateParameter(
            paramId,
            templateId,
            "duration",
            "Number of days",
            ParameterType.NUMBER.name(),
            true,
            null,
            0,
            Instant.now()
        );
        
        when(parameterRepository.findByTemplateIdOrderByDisplayOrder(templateId))
            .thenReturn(List.of(templateParam));
        when(metadataRepository.findByParameterId(paramId))
            .thenReturn(Optional.empty());
        when(validationRuleRepository.findByParameterId(paramId))
            .thenReturn(List.of());
        
        // When
        List<ParameterResponseDto> result = service.loadParametersWithMetadata(templateId);
        
        // Then
        assertEquals(1, result.size());
        ParameterResponseDto dto = result.get(0);
        assertNull(dto.metadata());
    }
}