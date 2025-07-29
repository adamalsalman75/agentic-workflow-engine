package dev.alsalman.agenticworkflowengine.execution.service;

import dev.alsalman.agenticworkflowengine.execution.GoalAgent;
import dev.alsalman.agenticworkflowengine.infrastructure.ResilientChatClient;
import dev.alsalman.agenticworkflowengine.workflow.domain.Goal;
import dev.alsalman.agenticworkflowengine.workflow.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class GoalAgentTest {

    @Mock
    private ResilientChatClient resilientChatClient;

    @InjectMocks
    private GoalAgent goalAgent;

    private Goal goal;
    private Task completedTask1;
    private Task completedTask2;
    private Task failedTask;

    @BeforeEach
    void setUp() {
        Instant fixedTime = Instant.parse("2024-01-01T12:00:00Z");
        
        completedTask1 = new Task(
            UUID.randomUUID(),
            "Complete data analysis",
            "Analysis completed with key insights discovered",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            fixedTime,
            fixedTime
        );

        completedTask2 = new Task(
            UUID.randomUUID(),
            "Generate report",
            "Report generated successfully",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            fixedTime,
            fixedTime
        );

        failedTask = new Task(
            UUID.randomUUID(),
            "Send email notifications",
            null,
            TaskStatus.FAILED,
            List.of(),
            List.of(),
            fixedTime,
            null
        );

        goal = new Goal(
            UUID.randomUUID(),
            "Analyze customer data and generate insights report",
            List.of(completedTask1, completedTask2, failedTask),
            "", // Empty summary initially
            GoalStatus.COMPLETED,
            fixedTime,
            fixedTime
        );
    }

    @Test
    void summarizeGoalCompletion_WithMixedTaskStatuses_ShouldCreateComprehensiveSummary() {
        // Given
        String expectedSummary = "Goal achieved with 2 out of 3 tasks completed successfully. " +
                                "Data analysis revealed key insights, report generated. " +
                                "Email notification task failed but core objective met.";
        
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn(expectedSummary);

        // When
        Goal result = goalAgent.summarizeGoalCompletion(goal);

        // Then
        assertThat(result.summary()).isEqualTo(expectedSummary);
        assertThat(result.id()).isEqualTo(goal.id()); // Other properties should be preserved
        assertThat(result.query()).isEqualTo(goal.query());
        assertThat(result.tasks()).isEqualTo(goal.tasks());
    }

    @Test
    void summarizeGoalCompletion_ShouldIncludeCorrectTaskCountsInPrompt() {
        // Given
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Summary of goal completion");

        // When
        goalAgent.summarizeGoalCompletion(goal);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Results: 2/3 completed, 1 failed");
        assertThat(capturedPrompt).contains("Analyze customer data and generate insights report");
    }

    @Test
    void summarizeGoalCompletion_ShouldIncludeAllTaskDetailsInPrompt() {
        // Given
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Detailed summary");

        // When
        goalAgent.summarizeGoalCompletion(goal);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        
        // Check that all task information is included
        assertThat(capturedPrompt).contains("Complete data analysis");
        assertThat(capturedPrompt).contains("[COMPLETED]");
        assertThat(capturedPrompt).contains("Analysis completed with key insights discovered");
        
        assertThat(capturedPrompt).contains("Generate report");
        assertThat(capturedPrompt).contains("Report generated successfully");
        
        assertThat(capturedPrompt).contains("Send email notifications");
        assertThat(capturedPrompt).contains("[FAILED]");
        assertThat(capturedPrompt).contains("No result");
    }

    @Test
    void summarizeGoalCompletion_WithAllTasksCompleted_ShouldReflectSuccess() {
        // Given
        Goal successfulGoal = new Goal(
            UUID.randomUUID(),
            "Complete project successfully",
            List.of(completedTask1, completedTask2),
            "",
            GoalStatus.COMPLETED,
            Instant.now(),
            Instant.now()
        );
        
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Project completed successfully with all objectives met");

        // When
        goalAgent.summarizeGoalCompletion(successfulGoal);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Results: 2/2 completed, 0 failed");
    }

    @Test
    void summarizeGoalCompletion_WithAllTasksFailed_ShouldReflectFailure() {
        // Given
        Task failedTask2 = new Task(
            UUID.randomUUID(),
            "Another failed task",
            "Error occurred during execution",
            TaskStatus.FAILED,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
        
        Goal failedGoal = new Goal(
            UUID.randomUUID(),
            "Attempt difficult task",
            List.of(failedTask, failedTask2),
            "",
            GoalStatus.FAILED,
            Instant.now(),
            Instant.now()
        );
        
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Goal failed due to multiple task failures");

        // When
        goalAgent.summarizeGoalCompletion(failedGoal);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Results: 0/2 completed, 2 failed");
    }

    @Test
    void summarizeGoalCompletion_WithEmptyTaskList_ShouldHandleGracefully() {
        // Given
        Goal emptyGoal = new Goal(
            UUID.randomUUID(),
            "Goal with no tasks",
            List.of(),
            "",
            GoalStatus.COMPLETED,
            Instant.now(),
            Instant.now()
        );
        
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Goal completed but no tasks were defined");

        // When
        Goal result = goalAgent.summarizeGoalCompletion(emptyGoal);

        // Then
        assertThat(result.summary()).isEqualTo("Goal completed but no tasks were defined");
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Results: 0/0 completed, 0 failed");
        assertThat(capturedPrompt).contains("Tasks:\n\n"); // Empty task section
    }

    @Test
    void summarizeGoalCompletion_WithTasksHavingNullResults_ShouldHandleGracefully() {
        // Given
        Task taskWithNullResult = new Task(
            UUID.randomUUID(),
            "Task with null result",
            null, // null result
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        Goal goalWithNullResults = new Goal(
            UUID.randomUUID(),
            "Test goal",
            List.of(taskWithNullResult),
            "",
            GoalStatus.COMPLETED,
            Instant.now(),
            Instant.now()
        );
        
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Goal completed with limited results");

        // When
        goalAgent.summarizeGoalCompletion(goalWithNullResults);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("goal summarization"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Task with null result");
        assertThat(capturedPrompt).contains("No result");
    }

    @Test
    void summarizeGoalCompletion_ShouldPreserveAllGoalProperties() {
        // Given
        when(resilientChatClient.call(eq("goal summarization"), any(String.class)))
            .thenReturn("Updated summary");

        // When
        Goal result = goalAgent.summarizeGoalCompletion(goal);

        // Then - All properties except summary and completedAt should be preserved
        assertThat(result.id()).isEqualTo(goal.id());
        assertThat(result.query()).isEqualTo(goal.query());
        assertThat(result.tasks()).isEqualTo(goal.tasks());
        assertThat(result.status()).isEqualTo(GoalStatus.COMPLETED); // withSummary sets to COMPLETED
        assertThat(result.createdAt()).isEqualTo(goal.createdAt());
        // completedAt is updated by withSummary, so don't compare it
        
        // Only summary should be updated
        assertThat(result.summary()).isEqualTo("Updated summary");
        assertThat(result.summary()).isNotEqualTo(goal.summary());
    }
}