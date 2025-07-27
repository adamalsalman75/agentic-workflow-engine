package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.agent.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskPlan;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskPlanServiceTest {

    @Mock
    private TaskPlanAgent taskPlanAgent;

    @InjectMocks
    private TaskPlanService taskPlanService;

    private Task testTask1, testTask2;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void createTaskPlan_ShouldDelegateToTaskPlanAgent() {
        // Given
        String userQuery = "Test query";
        TaskPlan expectedTaskPlan = TaskPlan.of(List.of(testTask1, testTask2), List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenReturn(expectedTaskPlan);

        // When
        TaskPlan result = taskPlanService.createTaskPlan(userQuery);

        // Then
        assertThat(result).isEqualTo(expectedTaskPlan);
        verify(taskPlanAgent).createTaskPlanWithDependencies(userQuery);
    }

    @Test
    void createTaskPlan_WithEmptyQuery_ShouldStillDelegateToAgent() {
        // Given
        String emptyQuery = "";
        TaskPlan emptyTaskPlan = TaskPlan.of(List.of(), List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(emptyQuery)).thenReturn(emptyTaskPlan);

        // When
        TaskPlan result = taskPlanService.createTaskPlan(emptyQuery);

        // Then
        assertThat(result).isEqualTo(emptyTaskPlan);
        verify(taskPlanAgent).createTaskPlanWithDependencies(emptyQuery);
    }
}