package dev.alsalman.agenticworkflowengine.planning.service;

import dev.alsalman.agenticworkflowengine.planning.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import dev.alsalman.agenticworkflowengine.workflow.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.junit.jupiter.api.BeforeEach;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.junit.jupiter.api.Test;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.junit.jupiter.api.extension.ExtendWith;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.mockito.InjectMocks;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.mockito.Mock;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import org.mockito.junit.jupiter.MockitoExtension;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import java.time.Instant;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import java.util.ArrayList;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import java.util.List;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import java.util.UUID;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import static org.assertj.core.api.Assertions.assertThat;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.ArgumentMatchers.any;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.ArgumentMatchers.anyList;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.ArgumentMatchers.eq;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.Mockito.never;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.Mockito.times;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.Mockito.verify;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;import static org.mockito.Mockito.when;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;import dev.alsalman.agenticworkflowengine.infrastructure.WorkflowPersistenceService;
import dev.alsalman.agenticworkflowengine.planning.PlanReviewService;@ExtendWith(MockitoExtension.class)
class PlanReviewServiceTest {

    @Mock
    private TaskPlanAgent taskPlanAgent;

    @Mock
    private WorkflowPersistenceService persistenceService;

    @InjectMocks
    private PlanReviewService planReviewService;

    private Task testTask1, testTask2, testTask3;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        goalId = UUID.randomUUID();
        
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
    void updateTaskInList_ShouldReplaceTaskInList() {
        // Given
        Task completedTask1 = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> remainingTasks = new ArrayList<>(List.of(testTask1, testTask2, testTask3));

        // When
        List<Task> result = planReviewService.updateTaskInList(remainingTasks, completedTask1, goalId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(completedTask1);
        assertThat(result.get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.get(0).result()).isEqualTo("Task 1 completed");
        assertThat(result.get(1)).isEqualTo(testTask2);
        assertThat(result.get(2)).isEqualTo(testTask3);

        // Verify persistence
        verify(persistenceService).saveTask(completedTask1, goalId);
    }

    @Test
    void updateTaskInList_ShouldMaintainTaskOrder() {
        // Given
        Task completedTask2 = testTask2.withResult("Task 2 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> remainingTasks = new ArrayList<>(List.of(testTask1, testTask2, testTask3));

        // When
        List<Task> result = planReviewService.updateTaskInList(remainingTasks, completedTask2, goalId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(testTask1);
        assertThat(result.get(1)).isEqualTo(completedTask2);
        assertThat(result.get(2)).isEqualTo(testTask3);
    }

    @Test
    void updateTaskInList_WithTaskNotInList_ShouldReturnUnchangedList() {
        // Given
        Task unknownTask = new Task(
            UUID.randomUUID(),
            "Unknown task",
            "Result",
            TaskStatus.COMPLETED,
            List.of(),
            List.of(),
            Instant.now(),
            Instant.now()
        );
        List<Task> remainingTasks = new ArrayList<>(List.of(testTask1, testTask2));

        // When
        List<Task> result = planReviewService.updateTaskInList(remainingTasks, unknownTask, goalId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testTask1, testTask2);
        
        // Unknown task should still be saved
        verify(persistenceService).saveTask(unknownTask, goalId);
    }

    @Test
    void handlePlanReview_WithFailedTask_ShouldReturnOriginalTasks() {
        // Given
        Task failedTask = testTask1.withStatus(TaskStatus.FAILED);
        List<Task> allTasks = List.of(failedTask, testTask2, testTask3);

        // When
        List<Task> result = planReviewService.handlePlanReview(allTasks, failedTask, goalId);

        // Then
        assertThat(result).isEqualTo(allTasks);
        
        // Verify no review was performed for failed task
        verify(taskPlanAgent, never()).reviewAndUpdatePlan(any(), any());
    }

    @Test
    void handlePlanReview_WithRevision_ShouldReturnRevisedTasks() {
        // Given
        Task completedTask = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> allTasks = List.of(completedTask, testTask2, testTask3);
        
        // Create a new task with null ID (simulating new task from plan review)
        Task newTask4 = new Task(
            null, // null ID indicates new task
            "New Task 4",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
        
        Task savedNewTask4 = new Task(
            UUID.randomUUID(), // Saved task will have ID
            "New Task 4",
            null,
            TaskStatus.PENDING,
            List.of(),
            List.of(),
            Instant.now(),
            null
        );
        
        List<Task> revisedTasks = List.of(completedTask, testTask2, testTask3, newTask4);
        
        when(taskPlanAgent.reviewAndUpdatePlan(eq(allTasks), eq(completedTask))).thenReturn(revisedTasks);
        when(persistenceService.saveTask(eq(newTask4), eq(goalId))).thenReturn(savedNewTask4);

        // When
        List<Task> result = planReviewService.handlePlanReview(allTasks, completedTask, goalId);

        // Then
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(completedTask);
        assertThat(result.get(1)).isEqualTo(testTask2);
        assertThat(result.get(2)).isEqualTo(testTask3);
        assertThat(result.get(3)).isEqualTo(savedNewTask4); // New task should have ID after save
        
        // Verify revision flow
        verify(taskPlanAgent).reviewAndUpdatePlan(eq(allTasks), eq(completedTask));
        verify(persistenceService).saveTask(eq(newTask4), eq(goalId));
    }

    @Test
    void handlePlanReview_WithCompletedTask_NoRevision_ShouldReturnOriginalTasks() {
        // Given
        Task completedTask = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> allTasks = List.of(completedTask, testTask2, testTask3);
        
        // Plan review returns same tasks (no changes)
        when(taskPlanAgent.reviewAndUpdatePlan(eq(allTasks), eq(completedTask))).thenReturn(allTasks);

        // When
        List<Task> result = planReviewService.handlePlanReview(allTasks, completedTask, goalId);

        // Then
        assertThat(result).isEqualTo(allTasks);
        verify(taskPlanAgent).reviewAndUpdatePlan(eq(allTasks), eq(completedTask));
    }

    @Test
    void handlePlanReview_WhenPlanReviewFails_ShouldReturnOriginalTasks() {
        // Given
        Task completedTask = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> allTasks = List.of(completedTask, testTask2, testTask3);
        
        // Plan review throws exception
        when(taskPlanAgent.reviewAndUpdatePlan(eq(allTasks), eq(completedTask)))
            .thenThrow(new RuntimeException("Plan review failed"));

        // When
        List<Task> result = planReviewService.handlePlanReview(allTasks, completedTask, goalId);

        // Then - should return original tasks when review fails
        assertThat(result).isEqualTo(allTasks);
        verify(taskPlanAgent).reviewAndUpdatePlan(eq(allTasks), eq(completedTask));
    }

    @Test
    void handlePlanReview_WithPendingTask_ShouldNotReview() {
        // Given
        Task pendingTask = testTask1; // Still PENDING status
        List<Task> allTasks = List.of(pendingTask, testTask2, testTask3);

        // When
        List<Task> result = planReviewService.handlePlanReview(allTasks, pendingTask, goalId);

        // Then - should return original tasks without review
        assertThat(result).isEqualTo(allTasks);
        verify(taskPlanAgent, never()).reviewAndUpdatePlan(any(), any());
    }

    @Test
    void updateTaskInList_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        Task completedTask = testTask1.withResult("Task 1 completed").withStatus(TaskStatus.COMPLETED);
        List<Task> emptyList = new ArrayList<>();

        // When
        List<Task> result = planReviewService.updateTaskInList(emptyList, completedTask, goalId);

        // Then
        assertThat(result).isEmpty();
        
        // Task should still be saved
        verify(persistenceService).saveTask(completedTask, goalId);
    }
}