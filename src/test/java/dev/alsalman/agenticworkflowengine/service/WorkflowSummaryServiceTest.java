package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.agent.GoalAgent;
import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowSummaryServiceTest {

    @Mock
    private GoalAgent goalAgent;

    @Mock
    private GoalService goalService;

    @InjectMocks
    private WorkflowSummaryService summaryService;

    private Goal testGoal;
    private Task testTask1, testTask2;
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

        testTask1 = new Task(
            UUID.randomUUID(),
            "Test task 1",
            "Task 1 completed successfully",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );

        testTask2 = new Task(
            UUID.randomUUID(),
            "Test task 2",
            "Task 2 completed successfully",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
    }

    @Test
    void summarizeWorkflow_ShouldGenerateSummaryAndMarkGoalCompleted() {
        // Given
        List<Task> completedTasks = List.of(testTask1, testTask2);
        Goal goalWithTasks = testGoal.withTasks(completedTasks);
        
        Goal summarizedGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            completedTasks,
            "AI-generated summary based on completed tasks",
            GoalStatus.COMPLETED,
            testGoal.createdAt(),
            Instant.now()
        );
        
        Goal finalGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            completedTasks,
            "AI-generated summary based on completed tasks",
            GoalStatus.COMPLETED,
            testGoal.createdAt(),
            Instant.now()
        );

        when(goalAgent.summarizeGoalCompletion(goalWithTasks)).thenReturn(summarizedGoal);
        when(goalService.markGoalAsCompleted(goalWithTasks, summarizedGoal.summary())).thenReturn(finalGoal);

        // When
        Goal result = summaryService.summarizeWorkflow(testGoal, completedTasks);

        // Then
        assertThat(result).isEqualTo(finalGoal);
        assertThat(result.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(result.summary()).isEqualTo("AI-generated summary based on completed tasks");
        
        verify(goalAgent).summarizeGoalCompletion(goalWithTasks);
        verify(goalService).markGoalAsCompleted(goalWithTasks, summarizedGoal.summary());
    }

    @Test
    void summarizeWorkflow_WithEmptyTasks_ShouldStillGenerateSummary() {
        // Given
        List<Task> emptyTasks = List.of();
        Goal goalWithEmptyTasks = testGoal.withTasks(emptyTasks);
        
        Goal summarizedGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            emptyTasks,
            "No tasks were completed",
            GoalStatus.COMPLETED,
            testGoal.createdAt(),
            Instant.now()
        );
        
        Goal finalGoal = new Goal(
            testGoal.id(),
            testGoal.query(),
            emptyTasks,
            "No tasks were completed",
            GoalStatus.COMPLETED,
            testGoal.createdAt(),
            Instant.now()
        );

        when(goalAgent.summarizeGoalCompletion(goalWithEmptyTasks)).thenReturn(summarizedGoal);
        when(goalService.markGoalAsCompleted(goalWithEmptyTasks, summarizedGoal.summary())).thenReturn(finalGoal);

        // When
        Goal result = summaryService.summarizeWorkflow(testGoal, emptyTasks);

        // Then
        assertThat(result).isEqualTo(finalGoal);
        assertThat(result.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(result.summary()).isEqualTo("No tasks were completed");
        
        verify(goalAgent).summarizeGoalCompletion(goalWithEmptyTasks);
        verify(goalService).markGoalAsCompleted(goalWithEmptyTasks, summarizedGoal.summary());
    }

    @Test
    void summarizeWorkflow_ShouldUpdateGoalWithCompletedTasks() {
        // Given
        List<Task> completedTasks = List.of(testTask1);
        Goal summarizedGoal = testGoal.withSummary("Test summary");
        Goal finalGoal = testGoal.withSummary("Test summary");

        when(goalAgent.summarizeGoalCompletion(any(Goal.class))).thenReturn(summarizedGoal);
        when(goalService.markGoalAsCompleted(any(Goal.class), eq("Test summary"))).thenReturn(finalGoal);

        // When
        summaryService.summarizeWorkflow(testGoal, completedTasks);

        // Then
        // Verify that the goal passed to goalAgent includes the completed tasks
        verify(goalAgent).summarizeGoalCompletion(testGoal.withTasks(completedTasks));
        verify(goalService).markGoalAsCompleted(testGoal.withTasks(completedTasks), "Test summary");
    }
}