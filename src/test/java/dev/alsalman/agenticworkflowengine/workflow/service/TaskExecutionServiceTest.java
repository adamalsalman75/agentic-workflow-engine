package dev.alsalman.agenticworkflowengine.workflow.service;

import dev.alsalman.agenticworkflowengine.execution.TaskAgent;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.BeforeEach;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.Test;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.extension.ExtendWith;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.InjectMocks;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.Mock;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.junit.jupiter.MockitoExtension;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import java.time.Instant;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import java.util.List;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import java.util.UUID;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import java.util.concurrent.ExecutionException;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import static org.assertj.core.api.Assertions.assertThat;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.assertj.core.api.Assertions.assertThatThrownBy;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.ArgumentMatchers.any;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.ArgumentMatchers.eq;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.Mockito.times;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.Mockito.verify;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.Mockito.when;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.workflow.service.TaskExecutionService;@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    @Mock
    private TaskAgent taskAgent;
    
    @Mock
    private DependencyResolver dependencyResolver;

    @InjectMocks
    private TaskExecutionService taskExecutionService;

    private Task testTask1, testTask2, testTask3;
    private String userQuery = "Test user query";

    @BeforeEach
    void setUp() {
        testTask1 = new Task(
            UUID.randomUUID(),
            "Task 1",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );

        testTask2 = new Task(
            UUID.randomUUID(),
            "Task 2",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );

        testTask3 = new Task(
            UUID.randomUUID(),
            "Task 3",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
    }

    @Test
    void getExecutableTasks_ShouldDelegateToResolver() {
        // Given
        List<Task> remainingTasks = List.of(testTask1, testTask2);
        List<Task> executableTasks = List.of(testTask1);
        when(dependencyResolver.getExecutableTasks(remainingTasks)).thenReturn(executableTasks);

        // When
        List<Task> result = taskExecutionService.getExecutableTasks(remainingTasks);

        // Then
        assertThat(result).isEqualTo(executableTasks);
        verify(dependencyResolver).getExecutableTasks(remainingTasks);
    }

    @Test
    void executeTasksInParallel_WithSingleTask_ShouldExecuteDirectly() {
        // Given
        List<Task> executableTasks = List.of(testTask1);
        List<Task> completedTasks = List.of();
        
        Task completedTask = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask);

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(completedTask);
        assertThat(result.get(0).result()).isEqualTo("Task 1 completed");
        assertThat(result.get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        
        verify(taskAgent, times(1)).executeTask(eq(testTask1), eq(userQuery), eq(completedTasks));
    }

    @Test
    void executeTasksInParallel_WithMultipleTasks_ShouldExecuteInParallel() {
        // Given
        List<Task> executableTasks = List.of(testTask1, testTask2, testTask3);
        List<Task> completedTasks = List.of();
        
        Task completedTask1 = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        Task completedTask2 = testTask2.withResult("Task 2 completed").withStatus(TaskStatus.COMPLETED);
        Task completedTask3 = testTask3.withResult("Task 3 completed").withStatus(TaskStatus.COMPLETED);
        
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask1);
        when(taskAgent.executeTask(eq(testTask2), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask2);
        when(taskAgent.executeTask(eq(testTask3), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask3);

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(completedTask1, completedTask2, completedTask3);
        
        // Verify all tasks were executed
        verify(taskAgent, times(1)).executeTask(eq(testTask1), eq(userQuery), eq(completedTasks));
        verify(taskAgent, times(1)).executeTask(eq(testTask2), eq(userQuery), eq(completedTasks));
        verify(taskAgent, times(1)).executeTask(eq(testTask3), eq(userQuery), eq(completedTasks));
    }

    @Test
    void executeTasksInParallel_WithEmptyTaskList_ShouldReturnEmptyList() {
        // Given
        List<Task> executableTasks = List.of();
        List<Task> completedTasks = List.of();

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).isEmpty();
        verify(taskAgent, times(0)).executeTask(any(), any(), any());
    }

    @Test
    void executeTasksInParallel_WithCompletedDependencies_ShouldPassThemToTaskAgent() {
        // Given
        Task completedDependency = new Task(
            UUID.randomUUID(),
            "Completed dependency",
            "Dependency result",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        
        List<Task> executableTasks = List.of(testTask1);
        List<Task> completedTasks = List.of(completedDependency);
        
        Task completedTask = testTask1.withResult("Task 1 completed with dependency").withStatus(TaskStatus.COMPLETED);
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask);

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).result()).isEqualTo("Task 1 completed with dependency");
        
        // Verify the completed tasks were passed correctly
        verify(taskAgent, times(1)).executeTask(eq(testTask1), eq(userQuery), eq(completedTasks));
    }

    @Test
    void executeTasksInParallel_WhenTaskExecutionFails_ShouldPropagateException() {
        // Given
        List<Task> executableTasks = List.of(testTask1, testTask2);
        List<Task> completedTasks = List.of();
        
        Task completedTask1 = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask1);
        when(taskAgent.executeTask(eq(testTask2), eq(userQuery), eq(completedTasks)))
            .thenThrow(new RuntimeException("Task execution failed"));

        // When/Then
        assertThatThrownBy(() -> 
            taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Parallel execution failed")
        .hasCauseInstanceOf(ExecutionException.class);
    }

    @Test
    void executeTasksInParallel_ShouldMaintainTaskOrder() {
        // Given
        List<Task> executableTasks = List.of(testTask3, testTask1, testTask2);
        List<Task> completedTasks = List.of();
        
        Task completedTask1 = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        Task completedTask2 = testTask2.withResult("Task 2 completed").withStatus(TaskStatus.COMPLETED);
        Task completedTask3 = testTask3.withResult("Task 3 completed").withStatus(TaskStatus.COMPLETED);
        
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask1);
        when(taskAgent.executeTask(eq(testTask2), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask2);
        when(taskAgent.executeTask(eq(testTask3), eq(userQuery), eq(completedTasks)))
            .thenReturn(completedTask3);

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).hasSize(3);
        // Results should contain all completed tasks (order may vary due to parallel execution)
        assertThat(result).containsExactlyInAnyOrder(completedTask1, completedTask2, completedTask3);
        
        // All tasks should have been executed
        verify(taskAgent, times(1)).executeTask(eq(testTask1), eq(userQuery), eq(completedTasks));
        verify(taskAgent, times(1)).executeTask(eq(testTask2), eq(userQuery), eq(completedTasks));
        verify(taskAgent, times(1)).executeTask(eq(testTask3), eq(userQuery), eq(completedTasks));
    }

    @Test
    void executeTasksInParallel_WithFailedTask_ShouldReturnFailedTask() {
        // Given
        List<Task> executableTasks = List.of(testTask1);
        List<Task> completedTasks = List.of();
        
        Task failedTask = testTask1.withStatus(TaskStatus.FAILED);
        when(taskAgent.executeTask(eq(testTask1), eq(userQuery), eq(completedTasks)))
            .thenReturn(failedTask);

        // When
        List<Task> result = taskExecutionService.executeTasksInParallel(executableTasks, userQuery, completedTasks);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.get(0).result()).isNull();
    }
}