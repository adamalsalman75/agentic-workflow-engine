package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.repository.SimpleTemplateRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleTemplateServicePhase2Test {
    
    @Mock
    private SimpleTemplateRepository repository;
    
    @Mock
    private WorkflowOrchestrator orchestrator;
    
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
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Paris, France");
        parameters.put("startDate", "invalid-date");
        parameters.put("duration", "7");
        
        // When/Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a valid date");
    }
    
    @Test
    void testExecuteTemplate_WithInvalidCurrency() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "Paris, France");
        parameters.put("startDate", "2024-06-15");
        parameters.put("duration", "7");
        parameters.put("budget", "2000 XYZ"); // Invalid currency code
        
        // When/Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid currency code");
    }
    
    @Test
    void testExecuteTemplate_WithInvalidLocation() {
        // Given
        when(repository.findById(templateId)).thenReturn(Optional.of(template));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("destination", "123456"); // Only numbers
        parameters.put("startDate", "2024-06-15");
        parameters.put("duration", "7");
        
        // When/Then
        assertThatThrownBy(() -> templateService.executeTemplate(templateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a valid location");
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
        parameters.put("destination", "Paris, France");
        parameters.put("startDate", "2024-06-15");
        parameters.put("duration", "7");
        // budget and travelStyle should use defaults
        
        // When
        WorkflowResult result = templateService.executeTemplate(templateId, parameters);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(parameters).containsEntry("budget", "1000 USD");
        assertThat(parameters).containsEntry("travelStyle", "Mid-range");
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
        
        // Test different date formats
        String[] dateFormats = {"2024-06-15", "06/15/2024", "15/06/2024"};
        
        for (String date : dateFormats) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("destination", "Paris, France");
            parameters.put("startDate", date);
            parameters.put("duration", "7");
            
            // When
            WorkflowResult result = templateService.executeTemplate(templateId, parameters);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.success()).isTrue();
        }
    }
}