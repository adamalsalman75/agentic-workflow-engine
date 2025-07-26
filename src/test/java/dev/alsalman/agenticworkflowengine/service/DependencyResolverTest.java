package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyResolverTest {

    private DependencyResolver dependencyResolver;
    private UUID task1Id, task2Id, task3Id, task4Id;
    private Task task1, task2, task3, task4;

    @BeforeEach
    void setUp() {
        dependencyResolver = new DependencyResolver();
        
        task1Id = UUID.randomUUID();
        task2Id = UUID.randomUUID();
        task3Id = UUID.randomUUID();
        task4Id = UUID.randomUUID();
        
        // Task1 - no dependencies
        task1 = new Task(task1Id, "Task 1", null, TaskStatus.PENDING, List.of(), List.of(), Instant.now(), null);
        
        // Task2 - depends on Task1 (blocking)
        task2 = new Task(task2Id, "Task 2", null, TaskStatus.PENDING, List.of(task1Id), List.of(), Instant.now(), null);
        
        // Task3 - depends on Task1 (informational)
        task3 = new Task(task3Id, "Task 3", null, TaskStatus.PENDING, List.of(), List.of(task1Id), Instant.now(), null);
        
        // Task4 - depends on Task2 and Task3 (blocking)
        task4 = new Task(task4Id, "Task 4", null, TaskStatus.PENDING, List.of(task2Id, task3Id), List.of(), Instant.now(), null);
    }

    @Test
    void getExecutableTasks_ShouldReturnTasksWithNoDependencies() {
        // Given
        List<Task> tasks = List.of(task1, task2, task3, task4);

        // When
        List<Task> executableTasks = dependencyResolver.getExecutableTasks(tasks);

        // Then
        assertThat(executableTasks).hasSize(2);
        assertThat(executableTasks).contains(task1, task3); // task1 has no deps, task3 has only informational deps
    }

    @Test
    void getExecutableTasks_ShouldReturnTasksWithSatisfiedDependencies() {
        // Given - task1 is completed
        Task completedTask1 = task1.withResult("Task 1 completed");
        List<Task> tasks = List.of(completedTask1, task2, task3, task4);

        // When
        List<Task> executableTasks = dependencyResolver.getExecutableTasks(tasks);

        // Then
        assertThat(executableTasks).hasSize(2);
        assertThat(executableTasks).contains(task2, task3); // task2 can now run (dependency satisfied), task3 can run (no blocking deps)
    }

    @Test
    void getExecutableTasks_ShouldReturnOnlyPendingTasksWhenAllDependenciesSatisfied() {
        // Given - all dependencies completed
        Task completedTask1 = task1.withResult("Task 1 completed");
        Task completedTask2 = task2.withResult("Task 2 completed");  
        Task completedTask3 = task3.withResult("Task 3 completed");
        List<Task> tasks = List.of(completedTask1, completedTask2, completedTask3, task4);

        // When
        List<Task> executableTasks = dependencyResolver.getExecutableTasks(tasks);

        // Then
        assertThat(executableTasks).hasSize(1);
        assertThat(executableTasks).contains(task4); // Only task4 is still pending and can execute
    }

    @Test
    void getExecutableTasks_ShouldExcludeCompletedTasks() {
        // Given - all tasks completed
        Task completedTask1 = task1.withResult("Task 1 completed");
        Task completedTask2 = task2.withResult("Task 2 completed");
        Task completedTask3 = task3.withResult("Task 3 completed");
        Task completedTask4 = task4.withResult("Task 4 completed");
        List<Task> tasks = List.of(completedTask1, completedTask2, completedTask3, completedTask4);

        // When
        List<Task> executableTasks = dependencyResolver.getExecutableTasks(tasks);

        // Then
        assertThat(executableTasks).isEmpty(); // No pending tasks to execute
    }

    @Test
    void validateDependencies_ShouldReturnEmptyForValidDependencies() {
        // Given
        List<Task> tasks = List.of(task1, task2, task3, task4);

        // When
        List<String> errors = dependencyResolver.validateDependencies(tasks);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    void validateDependencies_ShouldReturnErrorsForInvalidDependencies() {
        // Given - task with dependency on non-existent task
        UUID nonExistentId = UUID.randomUUID();
        Task invalidTask = new Task(UUID.randomUUID(), "Invalid Task", null, TaskStatus.PENDING, 
                                  List.of(nonExistentId), List.of(), Instant.now(), null);
        List<Task> tasks = List.of(task1, invalidTask);

        // When
        List<String> errors = dependencyResolver.validateDependencies(tasks);

        // Then
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).contains("invalid blocking dependency");
    }

    @Test
    void hasCircularDependencies_ShouldReturnFalseForValidDependencies() {
        // Given
        List<Task> tasks = List.of(task1, task2, task3, task4);

        // When
        boolean hasCircular = dependencyResolver.hasCircularDependencies(tasks);

        // Then
        assertThat(hasCircular).isFalse();
    }

    @Test
    void hasCircularDependencies_ShouldReturnTrueForCircularDependencies() {
        // Given - create circular dependency: task1 -> task2 -> task1
        Task circularTask1 = new Task(task1Id, "Task 1", null, TaskStatus.PENDING, List.of(task2Id), List.of(), Instant.now(), null);
        Task circularTask2 = new Task(task2Id, "Task 2", null, TaskStatus.PENDING, List.of(task1Id), List.of(), Instant.now(), null);
        List<Task> tasks = List.of(circularTask1, circularTask2);

        // When
        boolean hasCircular = dependencyResolver.hasCircularDependencies(tasks);

        // Then
        assertThat(hasCircular).isTrue();
    }

    @Test
    void hasCircularDependencies_ShouldReturnTrueForSelfDependency() {
        // Given - task depends on itself
        Task selfDependentTask = new Task(task1Id, "Task 1", null, TaskStatus.PENDING, List.of(task1Id), List.of(), Instant.now(), null);
        List<Task> tasks = List.of(selfDependentTask);

        // When
        boolean hasCircular = dependencyResolver.hasCircularDependencies(tasks);

        // Then
        assertThat(hasCircular).isTrue();
    }

    @Test
    void hasCircularDependencies_ShouldReturnTrueForLongerCircularChain() {
        // Given - create longer circular chain: task1 -> task2 -> task3 -> task1
        Task circularTask1 = new Task(task1Id, "Task 1", null, TaskStatus.PENDING, List.of(task3Id), List.of(), Instant.now(), null);
        Task circularTask2 = new Task(task2Id, "Task 2", null, TaskStatus.PENDING, List.of(task1Id), List.of(), Instant.now(), null);
        Task circularTask3 = new Task(task3Id, "Task 3", null, TaskStatus.PENDING, List.of(task2Id), List.of(), Instant.now(), null);
        List<Task> tasks = List.of(circularTask1, circularTask2, circularTask3);

        // When
        boolean hasCircular = dependencyResolver.hasCircularDependencies(tasks);

        // Then
        assertThat(hasCircular).isTrue();
    }
}