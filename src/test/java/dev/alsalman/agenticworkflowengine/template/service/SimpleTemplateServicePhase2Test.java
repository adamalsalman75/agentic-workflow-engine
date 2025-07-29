package dev.alsalman.agenticworkflowengine.template.service;

import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;
import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;
import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.template.repository.SimpleTemplateRepository;
import dev.alsalman.agenticworkflowengine.template.validation.AdvancedParameterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleTemplateServicePhase2Test {
    
    @Mock
    private SimpleTemplateRepository repository;
    
    @Mock
    private WorkflowOrchestrator orchestrator;
    
    @Mock
    private AdvancedParameterValidator advancedValidator;
    
    @InjectMocks
    private SimpleTemplateService templateService;
    
    private UUID templateId;
    private SimpleWorkflowTemplate template;
    
    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = SimpleWorkflowTemplate.create(
            "Simple Trip Planner",
            "Test description",
            "Travel",
            "Test prompt with {{destination}} {{startDate}} {{duration}} {{budget}} {{travelStyle}}",
            "Test Author"
        );
        
        // Mock advanced validator to return no errors by default (lenient for flexible use)
        lenient().when(advancedValidator.validateParameter(any(), anyString())).thenReturn(List.of());
    }
    
    @Test
    void testExecuteTemplate_WithAllNewParameterTypes() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        Goal goal = new Goal(UUID.randomUUID(), "Test prompt", List.of(), "Test summary", 
            GoalStatus.COMPLETED, Instant.now(), Instant.now());
        when(orchestrator.executeWorkflow(any(), any())).thenReturn(
            WorkflowResult.success(goal, Instant.now())
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Paris, France");
        parameters.put("startDate", "2024-06-15");
        parameters.put("duration", "7");
        parameters.put("budget", "2000 EUR");
        parameters.put("travelStyle", "Luxury");
        
        // When
        WorkflowResult result = templateService.executeTemplate(templateId, parameters);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }
    
    @Test
    void testExecuteTemplate_WithInvalidDate() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        when(advancedValidator.validateParameter(any(), any())).thenReturn(List.of("Invalid date format"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Paris");
        parameters.put("startDate", "invalid-date");
        parameters.put("duration", "5");
        
        // When & Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter validation failed");
    }
    
    @Test
    void testExecuteTemplate_WithInvalidCurrency() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        when(advancedValidator.validateParameter(any(), any())).thenReturn(List.of("Invalid currency format"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Tokyo");
        parameters.put("startDate", "2024-08-01");
        parameters.put("duration", "10");
        parameters.put("budget", "invalid-currency");
        
        // When & Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter validation failed");
    }
    
    @Test
    void testExecuteTemplate_WithInvalidLocation() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        when(advancedValidator.validateParameter(any(), any())).thenReturn(List.of("Invalid location"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "123!@#");
        parameters.put("startDate", "2024-07-01");
        parameters.put("duration", "3");
        
        // When & Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Parameter validation failed");
    }
    
    @Test
    void testExecuteTemplate_WithDefaultValues() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        Goal goal = new Goal(UUID.randomUUID(), "Test prompt", List.of(), "Test summary", 
            GoalStatus.COMPLETED, Instant.now(), Instant.now());
        when(orchestrator.executeWorkflow(any(), any())).thenReturn(
            WorkflowResult.success(goal, Instant.now())
        );
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "London, UK");
        parameters.put("startDate", "2024-09-01");
        parameters.put("duration", "14");
        // budget and travelStyle will use defaults
        
        // When
        WorkflowResult result = templateService.executeTemplate(templateId, parameters);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }
    
    @Test
    void testExecuteTemplate_WithMultipleDateFormats() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        Goal goal = new Goal(UUID.randomUUID(), "Test prompt", List.of(), "Test summary", 
            GoalStatus.COMPLETED, Instant.now(), Instant.now());
        when(orchestrator.executeWorkflow(any(), any())).thenReturn(
            WorkflowResult.success(goal, Instant.now())
        );
        
        // Test MM/dd/yyyy format
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Rome, Italy");
        parameters.put("startDate", "12/25/2024");
        parameters.put("duration", "4");
        
        // When
        WorkflowResult result = templateService.executeTemplate(templateId, parameters);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
    }
}