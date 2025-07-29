package dev.alsalman.agenticworkflowengine.execution.service;

import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import dev.alsalman.agenticworkflowengine.workflow.domain.TaskDependency;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import dev.alsalman.agenticworkflowengine.workflow.domain.DependencyType;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.BeforeEach;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.Test;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.junit.jupiter.api.extension.ExtendWith;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.InjectMocks;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.Mock;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import org.mockito.junit.jupiter.MockitoExtension;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import java.time.Instant;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import java.util.List;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import java.util.UUID;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import static org.assertj.core.api.Assertions.assertThat;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.ArgumentMatchers.anyList;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;import static org.mockito.Mockito.when;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;import dev.alsalman.agenticworkflowengine.planning.DependencyResolver;
import dev.alsalman.agenticworkflowengine.execution.TaskPreparationService;@ExtendWith(MockitoExtension.class)
class TaskPreparationServiceTest {

    @Mock
    private DependencyResolver dependencyResolver;

    @InjectMocks
    private TaskPreparationService taskPreparationService;

    private Task testTask1, testTask2, testTask3;
    private TaskDependency dependency1, dependency2;

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

        dependency1 = new TaskDependency(
            UUID.randomUUID(),
            testTask2.id(),
            testTask1.id(),
            DependencyType.BLOCKING,
            "Task 2 depends on Task 1",
            Instant.now()
        );

        dependency2 = new TaskDependency(
            UUID.randomUUID(),
            testTask3.id(),
            testTask2.id(),
            DependencyType.INFORMATIONAL,
            "Task 3 references Task 2",
            Instant.now()
        );
    }

    @Test
    void prepareTasks_ShouldReturnTasksUnchanged_WhenNoDependencyIssues() {
        // Given
        List<Task> inputTasks = List.of(testTask1, testTask2, testTask3);
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(List.of());
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(false);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(testTask1, testTask2, testTask3);
        
        // Verify all dependencies are preserved
        assertThat(result.get(0).blockingDependencies()).isEqualTo(testTask1.blockingDependencies());
        assertThat(result.get(0).informationalDependencies()).isEqualTo(testTask1.informationalDependencies());
    }

    @Test
    void prepareTasks_ShouldCleanInvalidDependencies_WhenValidationErrorsExist() {
        // Given
        Task taskWithDependencies = new Task(
            UUID.randomUUID(),
            "Task with dependencies",
            null,
            TaskStatus.PENDING,
            List.of(dependency1.id()),
            List.of(dependency2.id()),
            Instant.now(),
            null
        );
        
        List<Task> inputTasks = List.of(taskWithDependencies);
        List<String> validationErrors = List.of("Invalid dependency: task not found");
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(validationErrors);
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(false);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(1);
        Task cleanedTask = result.get(0);
        
        // Dependencies should be removed due to validation errors
        assertThat(cleanedTask.blockingDependencies()).isEmpty();
        assertThat(cleanedTask.informationalDependencies()).isEmpty();
        
        // Other properties should be preserved
        assertThat(cleanedTask.id()).isEqualTo(taskWithDependencies.id());
        assertThat(cleanedTask.description()).isEqualTo(taskWithDependencies.description());
        assertThat(cleanedTask.status()).isEqualTo(taskWithDependencies.status());
    }

    @Test
    void prepareTasks_ShouldRemoveAllDependencies_WhenCircularDependenciesDetected() {
        // Given
        Task taskWithDependencies = new Task(
            UUID.randomUUID(),
            "Task with circular dependencies",
            null,
            TaskStatus.PENDING,
            List.of(dependency1.id()),
            List.of(dependency2.id()),
            Instant.now(),
            null
        );
        
        List<Task> inputTasks = List.of(taskWithDependencies);
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(List.of());
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(true);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(1);
        Task cleanedTask = result.get(0);
        
        // All dependencies should be removed due to circular dependency
        assertThat(cleanedTask.blockingDependencies()).isEmpty();
        assertThat(cleanedTask.informationalDependencies()).isEmpty();
        
        // Other properties should be preserved
        assertThat(cleanedTask.id()).isEqualTo(taskWithDependencies.id());
        assertThat(cleanedTask.description()).isEqualTo(taskWithDependencies.description());
        assertThat(cleanedTask.status()).isEqualTo(taskWithDependencies.status());
    }

    @Test
    void prepareTasks_ShouldHandleBothValidationAndCircularIssues() {
        // Given
        Task taskWithDependencies = new Task(
            UUID.randomUUID(),
            "Task with multiple issues",
            null,
            TaskStatus.PENDING,
            List.of(dependency1.id()),
            List.of(dependency2.id()),
            Instant.now(),
            null
        );
        
        List<Task> inputTasks = List.of(taskWithDependencies);
        List<String> validationErrors = List.of("Multiple validation errors");
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(validationErrors);
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(true);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(1);
        Task cleanedTask = result.get(0);
        
        // All dependencies should be removed due to both issues
        assertThat(cleanedTask.blockingDependencies()).isEmpty();
        assertThat(cleanedTask.informationalDependencies()).isEmpty();
    }

    @Test
    void prepareTasks_ShouldHandleEmptyTaskList() {
        // Given
        List<Task> inputTasks = List.of();
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(List.of());
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(false);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void prepareTasks_ShouldPreserveTaskOrder() {
        // Given
        List<Task> inputTasks = List.of(testTask3, testTask1, testTask2);
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(List.of());
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(false);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(testTask3, testTask1, testTask2);
    }

    @Test
    void prepareTasks_ShouldHandleTasksWithoutDependencies() {
        // Given
        List<Task> inputTasks = List.of(testTask1, testTask2, testTask3);
        List<String> validationErrors = List.of("Some error");
        
        when(dependencyResolver.validateDependencies(anyList())).thenReturn(validationErrors);
        when(dependencyResolver.hasCircularDependencies(anyList())).thenReturn(false);

        // When
        List<Task> result = taskPreparationService.prepareTasks(inputTasks);

        // Then
        assertThat(result).hasSize(3);
        
        // Tasks already have no dependencies, so they should remain unchanged
        for (int i = 0; i < result.size(); i++) {
            Task originalTask = inputTasks.get(i);
            Task resultTask = result.get(i);
            
            assertThat(resultTask.id()).isEqualTo(originalTask.id());
            assertThat(resultTask.description()).isEqualTo(originalTask.description());
            assertThat(resultTask.blockingDependencies()).isEmpty();
            assertThat(resultTask.informationalDependencies()).isEmpty();
        }
    }
}