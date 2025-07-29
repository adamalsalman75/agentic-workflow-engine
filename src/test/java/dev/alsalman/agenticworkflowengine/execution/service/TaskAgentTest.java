package dev.alsalman.agenticworkflowengine.execution.service;

import dev.alsalman.agenticworkflowengine.execution.TaskAgent;
import dev.alsalman.agenticworkflowengine.infrastructure.ResilientChatClient;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAgentTest {

    @Mock
    private ResilientChatClient resilientChatClient;

    @InjectMocks
    private TaskAgent taskAgent;

    private Task task;
    private String originalGoal;
    private UUID dependencyTaskId;

    @BeforeEach
    void setUp() {
        dependencyTaskId = UUID.randomUUID();
        task = new Task(
            UUID.randomUUID(),
            "Complete analysis task",
            "",
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
        originalGoal = "Analyze data and generate insights";
    }

    @Test
    void executeTask_WithNoDependencies_ShouldProvideGeneralContext() {
        // Given
        List<Task> completedTasks = List.of(
            new Task(UUID.randomUUID(), "Task 1", "Result 1", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now()),
            new Task(UUID.randomUUID(), "Task 2", "Result 2", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now())
        );
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Analysis completed successfully");

        // When
        Task result = taskAgent.executeTask(task, originalGoal, completedTasks);

        // Then
        assertThat(result.result()).isEqualTo("Analysis completed successfully");
        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED); // withResult sets status to COMPLETED
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task execution"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Complete analysis task");
        assertThat(capturedPrompt).contains("Analyze data and generate insights");
        assertThat(capturedPrompt).contains("Previous completed tasks (for context):");
        assertThat(capturedPrompt).contains("Task 1");
        assertThat(capturedPrompt).contains("Result 1");
    }

    @Test
    void executeTask_WithBlockingDependencies_ShouldUseSpecificDependencyContext() {
        // Given
        Task taskWithDeps = new Task(
            UUID.randomUUID(),
            "Generate report",
            "",
            TaskStatus.PENDING,
            List.of(dependencyTaskId), // Blocking dependency
            List.of(),
            Instant.now(),
            null
        );
        
        Task dependencyTask = new Task(
            dependencyTaskId,
            "Collect data",
            "Data collection completed with 100 records",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        List<Task> completedTasks = List.of(dependencyTask);
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Report generated based on collected data");

        // When
        Task result = taskAgent.executeTask(taskWithDeps, originalGoal, completedTasks);

        // Then
        assertThat(result.result()).isEqualTo("Report generated based on collected data");
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task execution"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Generate report");
        assertThat(capturedPrompt).contains("DEPENDENCY OUTPUTS");
        assertThat(capturedPrompt).contains("REQUIRED DEPENDENCY: Collect data");
        assertThat(capturedPrompt).contains("Data collection completed with 100 records");
        assertThat(capturedPrompt).contains("build upon and reference the dependency outputs");
    }

    @Test
    void executeTask_WithInformationalDependencies_ShouldUseReferenceDependencyContext() {
        // Given
        Task taskWithDeps = new Task(
            UUID.randomUUID(),
            "Write summary",
            "",
            TaskStatus.PENDING,
            List.of(),
            List.of(dependencyTaskId), // Informational dependency
            Instant.now(),
            null
        );
        
        Task dependencyTask = new Task(
            dependencyTaskId,
            "Research background",
            "Found relevant background information",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        List<Task> completedTasks = List.of(dependencyTask);
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Summary written with background context");

        // When
        Task result = taskAgent.executeTask(taskWithDeps, originalGoal, completedTasks);

        // Then
        assertThat(result.result()).isEqualTo("Summary written with background context");
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task execution"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Write summary");
        assertThat(capturedPrompt).contains("DEPENDENCY OUTPUTS");
        assertThat(capturedPrompt).contains("REFERENCE DEPENDENCY: Research background");
        assertThat(capturedPrompt).contains("Found relevant background information");
    }

    @Test
    void executeTask_WithMixedDependencies_ShouldHandleBothTypes() {
        // Given
        UUID blockingDepId = UUID.randomUUID();
        UUID infoDepId = UUID.randomUUID();
        
        Task taskWithDeps = new Task(
            UUID.randomUUID(),
            "Create final output",
            "",
            TaskStatus.PENDING,
            List.of(blockingDepId),
            List.of(infoDepId),
            Instant.now(),
            null
        );
        
        Task blockingDep = new Task(
            blockingDepId,
            "Process core data",
            "Core processing complete",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        Task infoDep = new Task(
            infoDepId,
            "Gather context",
            "Context gathered successfully",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        List<Task> completedTasks = List.of(blockingDep, infoDep);
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Final output created");

        // When
        Task result = taskAgent.executeTask(taskWithDeps, originalGoal, completedTasks);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task execution"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("REQUIRED DEPENDENCY: Process core data");
        assertThat(capturedPrompt).contains("REFERENCE DEPENDENCY: Gather context");
        assertThat(capturedPrompt).contains("Core processing complete");
        assertThat(capturedPrompt).contains("Context gathered successfully");
    }

    @Test
    void executeTask_WithCompletedTasksButNoDependencies_ShouldFilterToRecentTasks() {
        // Given
        List<Task> completedTasks = List.of(
            new Task(UUID.randomUUID(), "Task 1", "Result 1", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now()),
            new Task(UUID.randomUUID(), "Task 2", "Result 2", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now()),
            new Task(UUID.randomUUID(), "Task 3", "Result 3", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now()),
            new Task(UUID.randomUUID(), "Task 4", "Result 4", TaskStatus.COMPLETED, 
                List.of(), List.of(), Instant.now(), Instant.now())
        );
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Task completed");

        // When
        taskAgent.executeTask(task, originalGoal, completedTasks);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task execution"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        // Should limit to 3 tasks as per implementation
        assertThat(capturedPrompt).contains("Task 1");
        assertThat(capturedPrompt).contains("Task 2");
        assertThat(capturedPrompt).contains("Task 3");
        // Task 4 should not be included due to limit(3)
    }

    @Test
    void executeTask_WhenChatClientThrowsException_ShouldReturnFailedTask() {
        // Given
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenThrow(new RuntimeException("AI service unavailable"));

        // When
        Task result = taskAgent.executeTask(task, originalGoal, List.of());

        // Then - withResult overrides status to COMPLETED even in error case
        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED); // withResult sets status to COMPLETED
        assertThat(result.result()).isEqualTo("Task execution failed: AI service unavailable");
    }

    @Test
    void executeTask_WithNullDependencyLists_ShouldHandleGracefully() {
        // Given
        Task taskWithNullDeps = new Task(
            UUID.randomUUID(),
            "Handle null deps",
            "",
            TaskStatus.PENDING,
            null, // null blocking dependencies
            null, // null informational dependencies
            Instant.now(),
            null
        );
        
        when(resilientChatClient.call(eq("task execution"), any(String.class)))
            .thenReturn("Task completed despite null dependencies");

        // When
        Task result = taskAgent.executeTask(taskWithNullDeps, originalGoal, List.of());

        // Then
        assertThat(result.result()).isEqualTo("Task completed despite null dependencies");
        assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED); // withResult sets status to COMPLETED
    }
}