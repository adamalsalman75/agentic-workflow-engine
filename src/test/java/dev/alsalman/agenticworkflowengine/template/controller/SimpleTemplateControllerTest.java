package dev.alsalman.agenticworkflowengine.template.controller;

import dev.alsalman.agenticworkflowengine.template.SimpleTemplateController;
import dev.alsalman.agenticworkflowengine.template.SimpleTemplateService;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleTemplateControllerTest {

    @Mock
    private SimpleTemplateService templateService;

    @InjectMocks
    private SimpleTemplateController controller;

    private UUID templateId;
    private SimpleWorkflowTemplate template;
    private List<SimpleParameter> parameters;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = SimpleWorkflowTemplate.create(
            "Test Template",
            "Test description",
            "Test category",
            "Test prompt",
            "Test author"
        );
        
        parameters = List.of(
            SimpleParameter.required("param1", "Test parameter", SimpleParameterType.STRING),
            SimpleParameter.optional("param2", "Optional parameter", SimpleParameterType.NUMBER, "10")
        );
    }

    @Test
    void listTemplates_ShouldReturnAllTemplates() {
        // Given
        List<SimpleWorkflowTemplate> templates = List.of(template);
        when(templateService.getAllTemplates()).thenReturn(templates);

        // When
        ResponseEntity<List<SimpleWorkflowTemplate>> response = controller.listTemplates();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).name()).isEqualTo("Test Template");
    }

    @Test
    void getTemplate_WithValidId_ShouldReturnTemplate() {
        // Given
        when(templateService.getTemplate(templateId)).thenReturn(template);

        // When
        ResponseEntity<SimpleWorkflowTemplate> response = controller.getTemplate(templateId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Test Template");
    }

    @Test
    void getTemplate_WithInvalidId_ShouldReturnNotFound() {
        // Given
        when(templateService.getTemplate(templateId)).thenThrow(new IllegalArgumentException("Template not found"));

        // When
        ResponseEntity<SimpleWorkflowTemplate> response = controller.getTemplate(templateId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getTemplateParameters_WithValidId_ShouldReturnParameters() {
        // Given
        when(templateService.getTemplateParameters(templateId)).thenReturn(parameters);

        // When
        ResponseEntity<List<SimpleParameter>> response = controller.getTemplateParameters(templateId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).name()).isEqualTo("param1");
        assertThat(response.getBody().get(1).name()).isEqualTo("param2");
    }

    @Test
    void getTemplateParameters_WithInvalidId_ShouldReturnNotFound() {
        // Given
        when(templateService.getTemplateParameters(templateId)).thenThrow(new IllegalArgumentException("Template not found"));

        // When
        ResponseEntity<List<SimpleParameter>> response = controller.getTemplateParameters(templateId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void executeTemplate_WithValidParameters_ShouldReturnSuccess() {
        // Given
        Goal goal = new Goal(UUID.randomUUID(), "Test goal", List.of(), "Test summary", 
            GoalStatus.COMPLETED, Instant.now(), Instant.now());
        WorkflowResult result = WorkflowResult.success(goal, Instant.now());
        
        Map<String, Object> requestParams = Map.of("param1", "value1", "param2", "20");
        when(templateService.executeTemplate(eq(templateId), any())).thenReturn(result);

        // When
        ResponseEntity<SimpleTemplateController.ExecuteResponse> response = 
            controller.executeTemplate(templateId, requestParams);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().goalId()).isEqualTo(goal.id());
        assertThat(response.getBody().message()).isEqualTo("Template executed successfully");
    }

    @Test
    void executeTemplate_WithFailedExecution_ShouldReturnFailure() {
        // Given
        Goal failedGoal = new Goal(UUID.randomUUID(), "Failed goal", List.of(), "Execution failed", 
            GoalStatus.FAILED, Instant.now(), Instant.now());
        WorkflowResult result = WorkflowResult.failure(failedGoal, Instant.now());
        
        Map<String, Object> requestParams = Map.of("param1", "value1");
        when(templateService.executeTemplate(eq(templateId), any())).thenReturn(result);

        // When
        ResponseEntity<SimpleTemplateController.ExecuteResponse> response = 
            controller.executeTemplate(templateId, requestParams);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().goalId()).isNull();
        assertThat(response.getBody().message()).isEqualTo("Template execution failed");
    }

    @Test
    void executeTemplate_WithValidationError_ShouldReturnBadRequest() {
        // Given
        Map<String, Object> requestParams = Map.of("param1", "invalid");
        when(templateService.executeTemplate(eq(templateId), any()))
            .thenThrow(new IllegalArgumentException("Parameter validation failed: Invalid value"));

        // When
        ResponseEntity<SimpleTemplateController.ExecuteResponse> response = 
            controller.executeTemplate(templateId, requestParams);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().goalId()).isNull();
        assertThat(response.getBody().message()).isEqualTo("Parameter validation failed: Invalid value");
    }

    @Test
    void executeTemplate_WithUnexpectedError_ShouldReturnInternalServerError() {
        // Given
        Map<String, Object> requestParams = Map.of("param1", "value1");
        when(templateService.executeTemplate(eq(templateId), any()))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When
        ResponseEntity<SimpleTemplateController.ExecuteResponse> response = 
            controller.executeTemplate(templateId, requestParams);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().goalId()).isNull();
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }
}