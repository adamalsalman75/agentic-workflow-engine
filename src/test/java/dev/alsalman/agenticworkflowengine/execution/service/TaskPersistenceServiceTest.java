package dev.alsalman.agenticworkflowengine.execution.service;

import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.execution.TaskDependencyResolver;
import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.execution.TaskPersistenceService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskPersistenceServiceTest {

    @Mock
    private TaskDependencyResolver dependencyResolver;

    @Mock
    private WorkflowPersistenceService persistenceService;

    @InjectMocks
    private TaskPersistenceService taskPersistenceService;

    private Task testTask1, testTask2;
    private TaskPlan testTaskPlan;
    private UUID testGoalId;

    @BeforeEach
    void setUp() {
        testGoalId = UUID.randomUUID();
        
        testTask1 = new Task(
            UUID.randomUUID(),
            "Test task 1",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );

        testTask2 = new Task(
            UUID.randomUUID(),
            "Test task 2",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );

        testTaskPlan = TaskPlan.of(List.of(testTask1, testTask2), List.of());
    }

    @Test
    void persistTaskPlan_ShouldDelegateToTaskDependencyResolver() {
        // Given
        List<Task> persistedTasks = List.of(testTask1, testTask2);
        when(dependencyResolver.coordinateTaskPersistence(testTaskPlan, testGoalId)).thenReturn(persistedTasks);

        // When
        List<Task> result = taskPersistenceService.persistTaskPlan(testTaskPlan, testGoalId);

        // Then
        assertThat(result).isEqualTo(persistedTasks);
        verify(dependencyResolver).coordinateTaskPersistence(testTaskPlan, testGoalId);
    }

    @Test
    void saveTaskUpdate_ShouldDelegateToWorkflowPersistenceService() {
        // Given
        Task updatedTask = testTask1.withResult("Task completed");
        when(persistenceService.saveTask(updatedTask, testGoalId)).thenReturn(updatedTask);

        // When
        Task result = taskPersistenceService.saveTaskUpdate(updatedTask, testGoalId);

        // Then
        assertThat(result).isEqualTo(updatedTask);
        verify(persistenceService).saveTask(updatedTask, testGoalId);
    }

    @Test
    void loadTasksForGoal_ShouldDelegateToWorkflowPersistenceService() {
        // Given
        List<Task> goalTasks = List.of(testTask1, testTask2);
        when(persistenceService.findTasksByGoalId(testGoalId)).thenReturn(goalTasks);

        // When
        List<Task> result = taskPersistenceService.loadTasksForGoal(testGoalId);

        // Then
        assertThat(result).isEqualTo(goalTasks);
        verify(persistenceService).findTasksByGoalId(testGoalId);
    }
}