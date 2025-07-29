package dev.alsalman.agenticworkflowengine.template.service;

import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import dev.alsalman.agenticworkflowengine.template.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import dev.alsalman.agenticworkflowengine.template.repository.SimpleTemplateRepository;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.junit.jupiter.api.BeforeEach;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.junit.jupiter.api.Test;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.junit.jupiter.api.extension.ExtendWith;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.mockito.InjectMocks;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.mockito.Mock;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import org.mockito.junit.jupiter.MockitoExtension;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import java.time.Instant;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import java.util.HashMap;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import java.util.List;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import java.util.Map;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import java.util.Optional;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import java.util.UUID;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import static org.assertj.core.api.Assertions.assertThat;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import static org.assertj.core.api.Assertions.assertThatThrownBy;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import static org.mockito.ArgumentMatchers.any;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;import static org.mockito.Mockito.when;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;
import dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator;@ExtendWith(MockitoExtension.class)
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
            .hasMessageContaining("must include a valid currency code");
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