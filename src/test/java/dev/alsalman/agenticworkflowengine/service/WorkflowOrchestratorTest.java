package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.agent.GoalAgent;
import dev.alsalman.agenticworkflowengine.agent.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.GoalStatus;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.domain.TaskStatus;
import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowOrchestratorTest {

    @Mock
    private TaskPlanAgent taskPlanAgent;

    @Mock
    private TaskDependencyResolver taskDependencyResolver;

    @Mock
    private GoalAgent goalAgent;

    @Mock
    private DependencyResolver dependencyResolver;

    @Mock
    private WorkflowPersistenceService persistenceService;
    
    @Mock
    private TaskPreparationService taskPreparationService;
    
    @Mock
    private TaskExecutionService taskExecutionService;
    
    @Mock
    private PlanReviewService planReviewService;

    @InjectMocks
    private WorkflowOrchestrator workflowOrchestrator;

    private Goal testGoal;
    private Task testTask1, testTask2;
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
        
        testGoal = new Goal(
            testGoalId,
            "Test query",
            List.of(),
            null,
            GoalStatus.IN_PROGRESS,
            Instant.now(),
            null
        );
    }

    @Test
    void executeWorkflow_ShouldCreateAndExecuteTasks_WhenSuccessful() {
        // Given
        String userQuery = "Test query";
        List<Task> initialTasks = List.of(testTask1, testTask2);
        
        Goal savedGoal = testGoal.withTasks(initialTasks);
        Goal completedGoal = savedGoal.withStatus(GoalStatus.COMPLETED).withSummary("Test summary");
        
        Task completedTask1 = testTask1.withResult("Task 1 result");
        Task completedTask2 = testTask2.withResult("Task 2 result");

        // Mocking the flow
        when(persistenceService.findGoalById(testGoalId)).thenReturn(savedGoal);
        TaskPlan taskPlan = TaskPlan.of(initialTasks, List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenReturn(taskPlan);
        when(taskDependencyResolver.coordinateTaskPersistence(any(TaskPlan.class), eq(testGoalId))).thenReturn(initialTasks);
        lenient().when(persistenceService.saveTask(any(Task.class), eq(testGoalId)))
            .thenReturn(testTask1, testTask2); // Return tasks with IDs
        
        // Mock the new service calls
        when(taskPreparationService.prepareTasks(anyList())).thenReturn(initialTasks);
        when(dependencyResolver.getExecutableTasks(anyList()))
            .thenReturn(List.of(testTask1, testTask2))
            .thenReturn(List.of()); // Second call returns empty (all tasks completed)
        
        when(taskExecutionService.executeTasksInParallel(anyList(), eq(userQuery), anyList()))
            .thenReturn(List.of(completedTask1, completedTask2));
        when(planReviewService.updateTaskInList(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> {
                List<Task> tasks = invocation.getArgument(0);
                Task completedTask = invocation.getArgument(1);
                // Simple mock: replace task in list
                return tasks.stream()
                    .map(t -> t.id().equals(completedTask.id()) ? completedTask : t)
                    .toList();
            });
        when(planReviewService.handlePlanReview(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> invocation.getArgument(0)); // Return unchanged
        
        when(goalAgent.summarizeGoalCompletion(any(Goal.class))).thenReturn(completedGoal);
        when(persistenceService.saveGoal(completedGoal)).thenReturn(completedGoal);

        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.goal().status()).isEqualTo(GoalStatus.COMPLETED);
        
        // Verify interactions with new service structure
        verify(taskPlanAgent).createTaskPlanWithDependencies(userQuery);
        verify(taskDependencyResolver).coordinateTaskPersistence(any(TaskPlan.class), eq(testGoalId));
        verify(taskPreparationService).prepareTasks(anyList());
        verify(taskExecutionService).executeTasksInParallel(anyList(), eq(userQuery), anyList());
        verify(planReviewService, atLeast(2)).updateTaskInList(anyList(), any(Task.class), eq(testGoalId));
        verify(planReviewService, atLeast(2)).handlePlanReview(anyList(), any(Task.class), eq(testGoalId));
        verify(goalAgent).summarizeGoalCompletion(any(Goal.class));
    }

    @Test
    void executeWorkflow_WithGoalId_ShouldLoadExistingGoal() {
        // Given
        String userQuery = "Test query";
        List<Task> initialTasks = List.of(testTask1);
        
        Goal existingGoal = testGoal.withTasks(List.of());
        Goal completedGoal = existingGoal.withStatus(GoalStatus.COMPLETED).withSummary("Test summary");
        Task completedTask = testTask1.withResult("Task result");

        when(persistenceService.findGoalById(testGoalId)).thenReturn(existingGoal);
        TaskPlan taskPlan = TaskPlan.of(initialTasks, List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenReturn(taskPlan);
        when(taskDependencyResolver.coordinateTaskPersistence(any(TaskPlan.class), eq(testGoalId))).thenReturn(initialTasks);
        lenient().when(persistenceService.saveTask(any(Task.class), eq(testGoalId))).thenReturn(testTask1);
        
        when(taskPreparationService.prepareTasks(anyList())).thenReturn(List.of(testTask1));
        when(dependencyResolver.getExecutableTasks(anyList()))
            .thenReturn(List.of(testTask1))
            .thenReturn(List.of());
        
        when(taskExecutionService.executeTasksInParallel(anyList(), eq(userQuery), anyList())).thenReturn(List.of(completedTask));
        when(planReviewService.updateTaskInList(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> {
                List<Task> tasks = invocation.getArgument(0);
                Task updatedTask = invocation.getArgument(1);
                return tasks.stream()
                    .map(t -> t.id().equals(updatedTask.id()) ? updatedTask : t)
                    .toList();
            });
        when(planReviewService.handlePlanReview(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(goalAgent.summarizeGoalCompletion(any(Goal.class))).thenReturn(completedGoal);
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(completedGoal);

        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isTrue();
        verify(persistenceService).findGoalById(testGoalId);
        verify(taskPlanAgent).createTaskPlanWithDependencies(userQuery);
    }

    @Test
    void executeWorkflow_ShouldHandleValidationErrors() {
        // Given
        String userQuery = "Test query";
        List<Task> initialTasks = List.of(testTask1);
        List<String> validationErrors = List.of("Invalid dependency");
        
        Goal savedGoal = testGoal.withTasks(initialTasks);
        Goal completedGoal = savedGoal.withStatus(GoalStatus.COMPLETED);
        Task completedTask = testTask1.withResult("Task result");

        when(persistenceService.findGoalById(testGoalId)).thenReturn(savedGoal);
        TaskPlan taskPlan = TaskPlan.of(initialTasks, List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenReturn(taskPlan);
        when(taskDependencyResolver.coordinateTaskPersistence(any(TaskPlan.class), eq(testGoalId))).thenReturn(initialTasks);
        lenient().when(persistenceService.saveTask(any(Task.class), eq(testGoalId))).thenReturn(testTask1);
        
        when(taskPreparationService.prepareTasks(anyList())).thenReturn(List.of(testTask1));
        when(dependencyResolver.getExecutableTasks(anyList()))
            .thenReturn(List.of(testTask1))
            .thenReturn(List.of());
        
        when(taskExecutionService.executeTasksInParallel(anyList(), eq(userQuery), anyList())).thenReturn(List.of(completedTask));
        when(planReviewService.updateTaskInList(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> {
                List<Task> tasks = invocation.getArgument(0);
                Task updatedTask = invocation.getArgument(1);
                return tasks.stream()
                    .map(t -> t.id().equals(updatedTask.id()) ? updatedTask : t)
                    .toList();
            });
        when(planReviewService.handlePlanReview(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(goalAgent.summarizeGoalCompletion(any(Goal.class))).thenReturn(completedGoal);
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(completedGoal);

        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isTrue();
        verify(taskPreparationService).prepareTasks(anyList());
    }

    @Test
    void executeWorkflow_ShouldHandleCircularDependencies() {
        // Given
        String userQuery = "Test query";
        List<Task> initialTasks = List.of(testTask1);
        
        Goal savedGoal = testGoal.withTasks(initialTasks);
        Goal completedGoal = savedGoal.withStatus(GoalStatus.COMPLETED);
        Task completedTask = testTask1.withResult("Task result");

        when(persistenceService.findGoalById(testGoalId)).thenReturn(savedGoal);
        TaskPlan taskPlan = TaskPlan.of(initialTasks, List.of());
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenReturn(taskPlan);
        when(taskDependencyResolver.coordinateTaskPersistence(any(TaskPlan.class), eq(testGoalId))).thenReturn(initialTasks);
        lenient().when(persistenceService.saveTask(any(Task.class), eq(testGoalId))).thenReturn(testTask1);
        
        when(taskPreparationService.prepareTasks(anyList())).thenReturn(List.of(testTask1));
        when(dependencyResolver.getExecutableTasks(anyList()))
            .thenReturn(List.of(testTask1))
            .thenReturn(List.of());
        
        when(taskExecutionService.executeTasksInParallel(anyList(), eq(userQuery), anyList())).thenReturn(List.of(completedTask));
        when(planReviewService.updateTaskInList(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> {
                List<Task> tasks = invocation.getArgument(0);
                Task updatedTask = invocation.getArgument(1);
                return tasks.stream()
                    .map(t -> t.id().equals(updatedTask.id()) ? updatedTask : t)
                    .toList();
            });
        when(planReviewService.handlePlanReview(anyList(), any(Task.class), eq(testGoalId)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(goalAgent.summarizeGoalCompletion(any(Goal.class))).thenReturn(completedGoal);
        when(persistenceService.saveGoal(any(Goal.class))).thenReturn(completedGoal);

        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isTrue();
        verify(taskPreparationService).prepareTasks(anyList());
    }

    @Test
    void executeWorkflow_ShouldHandleException_AndReturnFailure() {
        // Given
        String userQuery = "Test query";
        
        // Mock goal loading to succeed, but task planning to fail
        when(persistenceService.findGoalById(testGoalId)).thenReturn(testGoal);
        when(taskPlanAgent.createTaskPlanWithDependencies(userQuery)).thenThrow(new RuntimeException("Database error"));
        when(persistenceService.saveGoal(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.goal().status()).isEqualTo(GoalStatus.FAILED);
    }

    @Test
    void executeWorkflow_WithGoalId_ShouldHandleGoalNotFound() {
        // Given
        String userQuery = "Test query";
        
        when(persistenceService.findGoalById(testGoalId)).thenReturn(null);
        lenient().when(persistenceService.saveGoal(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // When
        WorkflowResult result = workflowOrchestrator.executeWorkflow(userQuery, testGoalId);

        // Then
        assertThat(result.success()).isFalse();
        verify(persistenceService, atLeast(1)).findGoalById(testGoalId);
    }
}
