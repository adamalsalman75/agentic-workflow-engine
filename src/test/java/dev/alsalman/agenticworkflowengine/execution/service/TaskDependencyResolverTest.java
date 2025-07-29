package dev.alsalman.agenticworkflowengine.execution.service;

import dev.alsalman.agenticworkflowengine.execution.TaskDependencyResolver;
import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.workflow.domain.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDependencyResolverTest {

    @Mock
    private WorkflowPersistenceService persistenceService;

    @InjectMocks
    private TaskDependencyResolver resolver;

    private UUID goalId;
    private UUID planningTask1Id;
    private UUID planningTask2Id;
    private UUID databaseTask1Id;
    private UUID databaseTask2Id;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();
        planningTask1Id = UUID.randomUUID();
        planningTask2Id = UUID.randomUUID();
        databaseTask1Id = UUID.randomUUID();
        databaseTask2Id = UUID.randomUUID();
    }

    @Test
    void coordinateTaskPersistence_WithNoDependencies_ShouldPersistTasksCorrectly() {
        // Given
        Task planningTask1 = new Task(planningTask1Id, "Task 1", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask2 = new Task(planningTask2Id, "Task 2", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        
        TaskPlan taskPlan = new TaskPlan(List.of(planningTask1, planningTask2), List.of());
        
        Task persistedTask1 = new Task(databaseTask1Id, "Task 1", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask2 = new Task(databaseTask2Id, "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        when(persistenceService.saveTask(any(), eq(goalId)))
            .thenReturn(persistedTask1)
            .thenReturn(persistedTask2);

        // When
        List<Task> result = resolver.coordinateTaskPersistence(taskPlan, goalId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(databaseTask1Id);
        assertThat(result.get(1).id()).isEqualTo(databaseTask2Id);
        
        verify(persistenceService, times(2)).saveTask(any(), eq(goalId));
        verify(persistenceService, never()).saveTaskDependency(any());
    }

    @Test
    void coordinateTaskPersistence_WithDependencies_ShouldRemapDependenciesCorrectly() {
        // Given
        Task planningTask1 = new Task(planningTask1Id, "Task 1", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask2 = new Task(planningTask2Id, "Task 2", "", TaskStatus.PENDING, 
            List.of(planningTask1Id), List.of(), Instant.now(), null);
        
        TaskDependency planningDependency = new TaskDependency(null, planningTask2Id, planningTask1Id, 
            DependencyType.BLOCKING, "Task 2 depends on Task 1", Instant.now());
        
        TaskPlan taskPlan = new TaskPlan(List.of(planningTask1, planningTask2), List.of(planningDependency));
        
        Task persistedTask1 = new Task(databaseTask1Id, "Task 1", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask2 = new Task(databaseTask2Id, "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        when(persistenceService.saveTask(any(), eq(goalId)))
            .thenReturn(persistedTask1)
            .thenReturn(persistedTask2);

        // When
        List<Task> result = resolver.coordinateTaskPersistence(taskPlan, goalId);

        // Then
        assertThat(result).hasSize(2);
        
        // Check that Task 2 now has the remapped dependency on Task 1
        Task resultTask2 = result.stream()
            .filter(task -> task.description().equals("Task 2"))
            .findFirst()
            .orElseThrow();
        
        assertThat(resultTask2.blockingDependencies()).containsExactly(databaseTask1Id);
        
        verify(persistenceService, times(2)).saveTask(any(), eq(goalId));
        verify(persistenceService, times(1)).saveTaskDependency(any());
    }

    @Test
    void coordinateTaskPersistence_WithInformationalDependencies_ShouldHandleCorrectly() {
        // Given
        Task planningTask1 = new Task(planningTask1Id, "Task 1", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask2 = new Task(planningTask2Id, "Task 2", "", TaskStatus.PENDING, 
            List.of(), List.of(planningTask1Id), Instant.now(), null);
        
        TaskDependency planningDependency = new TaskDependency(null, planningTask2Id, planningTask1Id, 
            DependencyType.INFORMATIONAL, "Task 2 uses info from Task 1", Instant.now());
        
        TaskPlan taskPlan = new TaskPlan(List.of(planningTask1, planningTask2), List.of(planningDependency));
        
        Task persistedTask1 = new Task(databaseTask1Id, "Task 1", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask2 = new Task(databaseTask2Id, "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        when(persistenceService.saveTask(any(), eq(goalId)))
            .thenReturn(persistedTask1)
            .thenReturn(persistedTask2);

        // When
        List<Task> result = resolver.coordinateTaskPersistence(taskPlan, goalId);

        // Then
        assertThat(result).hasSize(2);
        
        // Check that Task 2 now has the remapped informational dependency
        Task resultTask2 = result.stream()
            .filter(task -> task.description().equals("Task 2"))
            .findFirst()
            .orElseThrow();
        
        assertThat(resultTask2.informationalDependencies()).containsExactly(databaseTask1Id);
        assertThat(resultTask2.blockingDependencies()).isEmpty();
        
        verify(persistenceService, times(1)).saveTaskDependency(any());
    }

    @Test
    void coordinateTaskPersistence_WithMixedDependencyTypes_ShouldHandleBothTypes() {
        // Given
        UUID planningTask3Id = UUID.randomUUID();
        UUID databaseTask3Id = UUID.randomUUID();
        
        Task planningTask1 = new Task(planningTask1Id, "Task 1", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask2 = new Task(planningTask2Id, "Task 2", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask3 = new Task(planningTask3Id, "Task 3", "", TaskStatus.PENDING, 
            List.of(planningTask1Id), List.of(planningTask2Id), Instant.now(), null);
        
        TaskDependency blockingDep = new TaskDependency(null, planningTask3Id, planningTask1Id, 
            DependencyType.BLOCKING, "Task 3 blocked by Task 1", Instant.now());
        TaskDependency infoDep = new TaskDependency(null, planningTask3Id, planningTask2Id, 
            DependencyType.INFORMATIONAL, "Task 3 uses info from Task 2", Instant.now());
        
        TaskPlan taskPlan = new TaskPlan(
            List.of(planningTask1, planningTask2, planningTask3), 
            List.of(blockingDep, infoDep)
        );
        
        Task persistedTask1 = new Task(databaseTask1Id, "Task 1", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask2 = new Task(databaseTask2Id, "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask3 = new Task(databaseTask3Id, "Task 3", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        when(persistenceService.saveTask(any(), eq(goalId)))
            .thenReturn(persistedTask1)
            .thenReturn(persistedTask2)
            .thenReturn(persistedTask3);

        // When
        List<Task> result = resolver.coordinateTaskPersistence(taskPlan, goalId);

        // Then
        assertThat(result).hasSize(3);
        
        // Check that Task 3 has both dependency types correctly mapped
        Task resultTask3 = result.stream()
            .filter(task -> task.description().equals("Task 3"))
            .findFirst()
            .orElseThrow();
        
        assertThat(resultTask3.blockingDependencies()).containsExactly(databaseTask1Id);
        assertThat(resultTask3.informationalDependencies()).containsExactly(databaseTask2Id);
        
        verify(persistenceService, times(3)).saveTask(any(), eq(goalId));
        verify(persistenceService, times(2)).saveTaskDependency(any());
    }

    @Test
    void coordinateTaskPersistence_WithDependencyPersistenceFailure_ShouldContinueProcessing() {
        // Given
        Task planningTask1 = new Task(planningTask1Id, "Task 1", "", TaskStatus.PENDING, 
            List.of(), List.of(), Instant.now(), null);
        Task planningTask2 = new Task(planningTask2Id, "Task 2", "", TaskStatus.PENDING, 
            List.of(planningTask1Id), List.of(), Instant.now(), null);
        
        TaskDependency planningDependency = new TaskDependency(null, planningTask2Id, planningTask1Id, 
            DependencyType.BLOCKING, "Task 2 depends on Task 1", Instant.now());
        
        TaskPlan taskPlan = new TaskPlan(List.of(planningTask1, planningTask2), List.of(planningDependency));
        
        Task persistedTask1 = new Task(databaseTask1Id, "Task 1", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        Task persistedTask2 = new Task(databaseTask2Id, "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        when(persistenceService.saveTask(any(), eq(goalId)))
            .thenReturn(persistedTask1)
            .thenReturn(persistedTask2);
        doThrow(new RuntimeException("Database error")).when(persistenceService).saveTaskDependency(any());

        // When
        List<Task> result = resolver.coordinateTaskPersistence(taskPlan, goalId);

        // Then - Should still return tasks even if dependency persistence fails
        assertThat(result).hasSize(2);
        
        verify(persistenceService, times(2)).saveTask(any(), eq(goalId));
        verify(persistenceService, times(1)).saveTaskDependency(any());
    }
}