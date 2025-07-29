package dev.alsalman.agenticworkflowengine.workflow.service;

import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private WorkflowPersistenceService persistenceService;

    @InjectMocks
    private GoalService goalService;

    private Goal testGoal;
    private UUID testGoalId;

    @BeforeEach
    void setUp() {
        testGoalId = UUID.randomUUID();
        testGoal = new Goal(
            testGoalId,
            "Test query",
            List.of(),
            null,
            GoalStatus.IN_PROGRESS,
            Instant.now(),
            null
        );
    }

    @Test
    void initializeGoal_WithExistingGoalId_ShouldReturnExistingGoal() {
        // Given
        String userQuery = "Test query";
        when(persistenceService.findGoalById(testGoalId)).thenReturn(testGoal);

        // When
        Goal result = goalService.initializeGoal(userQuery, testGoalId);

        // Then
        assertThat(result).isEqualTo(testGoal);
        verify(persistenceService).findGoalById(testGoalId);
    }

    @Test
    void initializeGoal_WithNonExistentGoalId_ShouldCreateNewGoal() {
        // Given
        String userQuery = "Test query";
        Goal newGoal = new Goal(null, userQuery, List.of(), null, GoalStatus.IN_PROGRESS, Instant.now(), null);
        Goal savedGoal = new Goal(testGoalId, userQuery, List.of(), null, GoalStatus.IN_PROGRESS, Instant.now(), null);
        
        when(persistenceService.findGoalById(testGoalId)).thenReturn(null);
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(savedGoal);

        // When
        Goal result = goalService.initializeGoal(userQuery, testGoalId);

        // Then
        assertThat(result).isEqualTo(savedGoal);
        verify(persistenceService).findGoalById(testGoalId);
        verify(persistenceService).saveGoal(any(Goal.class));
    }

    @Test
    void initializeGoal_WithNullGoalId_ShouldCreateNewGoal() {
        // Given
        String userQuery = "Test query";
        Goal savedGoal = new Goal(testGoalId, userQuery, List.of(), null, GoalStatus.IN_PROGRESS, Instant.now(), null);
        
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(savedGoal);

        // When
        Goal result = goalService.initializeGoal(userQuery, null);

        // Then
        assertThat(result).isEqualTo(savedGoal);
        verify(persistenceService).saveGoal(any(Goal.class));
    }

    @Test
    void markGoalAsFailed_ShouldUpdateGoalStatusAndSave() {
        // Given
        String errorMessage = "Test error";
        Goal failedGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            testGoal.tasks(),
            "Workflow failed: " + errorMessage,
            GoalStatus.FAILED,
            testGoal.createdAt(),
            Instant.now()
        );
        
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(failedGoal);

        // When
        Goal result = goalService.markGoalAsFailed(testGoal, errorMessage);

        // Then
        assertThat(result.status()).isEqualTo(GoalStatus.FAILED);
        assertThat(result.summary()).isEqualTo("Workflow failed: " + errorMessage);
        assertThat(result.completedAt()).isNotNull();
        verify(persistenceService).saveGoal(any(Goal.class));
    }

    @Test
    void markGoalAsCompleted_ShouldUpdateGoalStatusAndSave() {
        // Given
        String summary = "Test summary";
        Goal completedGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            testGoal.tasks(),
            summary,
            GoalStatus.COMPLETED,
            testGoal.createdAt(),
            Instant.now()
        );
        
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(completedGoal);

        // When
        Goal result = goalService.markGoalAsCompleted(testGoal, summary);

        // Then
        assertThat(result.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(result.summary()).isEqualTo(summary);
        assertThat(result.completedAt()).isNotNull();
        verify(persistenceService).saveGoal(any(Goal.class));
    }
}