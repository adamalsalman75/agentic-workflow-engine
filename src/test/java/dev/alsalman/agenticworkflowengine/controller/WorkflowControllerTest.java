package dev.alsalman.agenticworkflowengine.controller;

import dev.alsalman.agenticworkflowengine.domain.ExecutionResponse;
import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.domain.GoalSummary;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.service.WorkflowOrchestrator;
import dev.alsalman.agenticworkflowengine.service.WorkflowPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private WorkflowOrchestrator workflowOrchestrator;

    @Mock
    private WorkflowPersistenceService persistenceService;

    @InjectMocks
    private WorkflowController workflowController;

    private Goal testGoal;
    private UUID testGoalId;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testGoalId = UUID.randomUUID();
        testTask = new Task(
            UUID.randomUUID(),
            "Test task",
            "Test result",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        testGoal = new Goal(
            testGoalId,
            "Test query",
            List.of(testTask),
            "Test summary",
            GoalStatus.COMPLETED,
            Instant.now(),
            Instant.now()
        );
    }

    @Test
    void executeWorkflow_ShouldReturnGoalIdImmediately() {
        // Given
        WorkflowController.WorkflowRequest request = new WorkflowController.WorkflowRequest("Test query");
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(testGoal);

        // When
        ResponseEntity<ExecutionResponse> response = workflowController.executeWorkflow(request);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().goalId()).isEqualTo(testGoalId);
        assertThat(response.getBody().message()).isEqualTo("Workflow execution started");
        
        // Verify goal was saved
        verify(persistenceService).saveGoal(any(Goal.class));
        
        // Give some time for async execution to potentially start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void executeWorkflow_ShouldHandleException() {
        // Given
        WorkflowController.WorkflowRequest request = new WorkflowController.WorkflowRequest("Test query");
        when(persistenceService.saveGoal(any(Goal.class))).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<ExecutionResponse> response = workflowController.executeWorkflow(request);

        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getGoal_ShouldReturnGoal_WhenGoalExists() {
        // Given
        when(persistenceService.findGoalById(testGoalId)).thenReturn(testGoal);

        // When
        ResponseEntity<GoalSummary> response = workflowController.getGoal(testGoalId);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(GoalSummary.from(testGoal));
        verify(persistenceService).findGoalById(testGoalId);
    }

    @Test
    void getGoal_ShouldReturnNotFound_WhenGoalDoesNotExist() {
        // Given
        when(persistenceService.findGoalById(testGoalId)).thenReturn(null);

        // When
        ResponseEntity<GoalSummary> response = workflowController.getGoal(testGoalId);

        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(persistenceService).findGoalById(testGoalId);
    }

    @Test
    void getGoal_ShouldHandleException() {
        // Given
        when(persistenceService.findGoalById(testGoalId)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<GoalSummary> response = workflowController.getGoal(testGoalId);

        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        verify(persistenceService).findGoalById(testGoalId);
    }

    @Test
    void getGoalTasks_ShouldReturnTasks() {
        // Given
        List<Task> tasks = List.of(testTask);
        when(persistenceService.findTasksByGoalId(testGoalId)).thenReturn(tasks);

        // When
        ResponseEntity<List<Task>> response = workflowController.getGoalTasks(testGoalId);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(tasks);
        verify(persistenceService).findTasksByGoalId(testGoalId);
    }

    @Test
    void getGoalTasks_ShouldHandleException() {
        // Given
        when(persistenceService.findTasksByGoalId(testGoalId)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<List<Task>> response = workflowController.getGoalTasks(testGoalId);

        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        verify(persistenceService).findTasksByGoalId(testGoalId);
    }
}