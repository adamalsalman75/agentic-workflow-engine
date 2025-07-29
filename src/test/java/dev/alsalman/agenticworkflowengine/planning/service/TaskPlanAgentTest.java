package dev.alsalman.agenticworkflowengine.planning.service;

import dev.alsalman.agenticworkflowengine.infrastructure.ResilientChatClient;
import dev.alsalman.agenticworkflowengine.planning.TaskPlanAgent;
import dev.alsalman.agenticworkflowengine.planning.domain.TaskPlan;
import dev.alsalman.agenticworkflowengine.workflow.domain.DependencyType;
import dev.alsalman.agenticworkflowengine.workflow.domain.Task;
import dev.alsalman.agenticworkflowengine.workflow.domain.TaskDependency;
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
class TaskPlanAgentTest {

    @Mock
    private ResilientChatClient resilientChatClient;

    @InjectMocks
    private TaskPlanAgent taskPlanAgent;

    @BeforeEach
    void setUp() {
        // Setup is minimal as most tests configure their own mocks
    }

    @Test
    void createTaskPlanWithDependencies_WithNoDependencies_ShouldReturnTasksWithoutDependencies() {
        // Given
        String userGoal = "Create a simple presentation";
        String aiResponse = """
            TASKS:
            1. Research topic
            2. Create outline
            3. Design slides
            
            DEPENDENCIES:
            """;
        
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn(aiResponse);

        // When
        TaskPlan result = taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        assertThat(result.tasks()).hasSize(3);
        assertThat(result.dependencies()).isEmpty();
        
        assertThat(result.tasks().get(0).description()).isEqualTo("Research topic");
        assertThat(result.tasks().get(1).description()).isEqualTo("Create outline");
        assertThat(result.tasks().get(2).description()).isEqualTo("Design slides");
        
        // All tasks should have empty dependency lists
        result.tasks().forEach(task -> {
            assertThat(task.blockingDependencies()).isEmpty();
            assertThat(task.informationalDependencies()).isEmpty();
        });
    }

    @Test
    void createTaskPlanWithDependencies_WithBlockingDependencies_ShouldParseCorrectly() {
        // Given
        String userGoal = "Launch a new product";
        String aiResponse = """
            TASKS:
            1. Research market demand
            2. Develop product
            3. Create marketing strategy
            
            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - needs market data to guide development
            Task 3 depends on Task 2 (blocking) - needs product features to create strategy
            """;
        
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn(aiResponse);

        // When
        TaskPlan result = taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        assertThat(result.tasks()).hasSize(3);
        assertThat(result.dependencies()).hasSize(2);
        
        // Check task descriptions
        assertThat(result.tasks().get(0).description()).isEqualTo("Research market demand");
        assertThat(result.tasks().get(1).description()).isEqualTo("Develop product");
        assertThat(result.tasks().get(2).description()).isEqualTo("Create marketing strategy");
        
        // Check dependencies
        TaskDependency dep1 = result.dependencies().get(0);
        TaskDependency dep2 = result.dependencies().get(1);
        
        assertThat(dep1.type()).isEqualTo(DependencyType.BLOCKING);
        assertThat(dep1.reason()).isEqualTo("needs market data to guide development");
        
        assertThat(dep2.type()).isEqualTo(DependencyType.BLOCKING);
        assertThat(dep2.reason()).isEqualTo("needs product features to create strategy");
        
        // Check that tasks have correct blocking dependencies
        Task task2 = result.tasks().get(1); // "Develop product"
        Task task3 = result.tasks().get(2); // "Create marketing strategy"
        
        assertThat(task2.blockingDependencies()).containsExactly(result.tasks().get(0).id());
        assertThat(task3.blockingDependencies()).containsExactly(result.tasks().get(1).id());
    }

    @Test
    void createTaskPlanWithDependencies_WithInformationalDependencies_ShouldParseCorrectly() {
        // Given
        String userGoal = "Write a research paper";
        String aiResponse = """
            TASKS:
            1. Gather primary sources
            2. Review existing literature
            3. Write first draft
            
            DEPENDENCIES:
            Task 3 depends on Task 1 (blocking) - needs primary sources to write
            Task 3 depends on Task 2 (informational) - literature review provides context
            """;
        
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn(aiResponse);

        // When
        TaskPlan result = taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        assertThat(result.tasks()).hasSize(3);
        assertThat(result.dependencies()).hasSize(2);
        
        // Check that Task 3 has both types of dependencies
        Task task3 = result.tasks().get(2); // "Write first draft"
        assertThat(task3.blockingDependencies()).containsExactly(result.tasks().get(0).id());
        assertThat(task3.informationalDependencies()).containsExactly(result.tasks().get(1).id());
        
        // Check dependency types
        List<TaskDependency> blockingDeps = result.dependencies().stream()
            .filter(dep -> dep.type() == DependencyType.BLOCKING)
            .toList();
        List<TaskDependency> infoDeps = result.dependencies().stream()
            .filter(dep -> dep.type() == DependencyType.INFORMATIONAL)
            .toList();
        
        assertThat(blockingDeps).hasSize(1);
        assertThat(infoDeps).hasSize(1);
        
        assertThat(blockingDeps.get(0).reason()).isEqualTo("needs primary sources to write");
        assertThat(infoDeps.get(0).reason()).isEqualTo("literature review provides context");
    }

    @Test
    void createTaskPlanWithDependencies_WithMalformedDependencies_ShouldSkipInvalidOnes() {
        // Given
        String userGoal = "Complete project";
        String aiResponse = """
            TASKS:
            1. Start project
            2. Complete project
            
            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - valid dependency
            Invalid dependency line without proper format
            Task 99 depends on Task 1 (blocking) - invalid task number
            """;
        
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn(aiResponse);

        // When
        TaskPlan result = taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        assertThat(result.tasks()).hasSize(2);
        assertThat(result.dependencies()).hasSize(1); // Only the valid dependency should be parsed
        
        assertThat(result.dependencies().get(0).reason()).isEqualTo("valid dependency");
    }

    @Test
    void reviewAndUpdatePlan_WithNoChangesNeeded_ShouldReturnOriginalTasks() {
        // Given
        Task task1 = new Task(UUID.randomUUID(), "Task 1", "Result 1", TaskStatus.COMPLETED,
            List.of(), List.of(), Instant.now(), Instant.now());
        Task task2 = new Task(UUID.randomUUID(), "Task 2", "", TaskStatus.PENDING,
            List.of(), List.of(), Instant.now(), null);
        
        List<Task> currentTasks = List.of(task1, task2);
        
        when(resilientChatClient.call(eq("plan review"), any(String.class)))
            .thenReturn("NO_CHANGES");

        // When
        List<Task> result = taskPlanAgent.reviewAndUpdatePlan(currentTasks, task1);

        // Then
        assertThat(result).isEqualTo(currentTasks);
        assertThat(result).hasSize(2);
    }

    @Test
    void reviewAndUpdatePlan_WithChangesRequested_ShouldPreserveOriginalPlan() {
        // Given
        Task completedTask = new Task(UUID.randomUUID(), "Research task", "Found important data", 
            TaskStatus.COMPLETED, List.of(), List.of(), Instant.now(), Instant.now());
        Task pendingTask = new Task(UUID.randomUUID(), "Implementation task", "", 
            TaskStatus.PENDING, List.of(), List.of(), Instant.now(), null);
        
        List<Task> currentTasks = List.of(completedTask, pendingTask);
        
        when(resilientChatClient.call(eq("plan review"), any(String.class)))
            .thenReturn("Based on the research results, we should add a new validation task");

        // When
        List<Task> result = taskPlanAgent.reviewAndUpdatePlan(currentTasks, completedTask);

        // Then - Should preserve original plan for stability
        assertThat(result).isEqualTo(currentTasks);
        assertThat(result).hasSize(2);
    }

    @Test
    void reviewAndUpdatePlan_ShouldIncludeCompletedTaskInfoInPrompt() {
        // Given
        Task completedTask = new Task(UUID.randomUUID(), "Market research", "Discovered high demand", 
            TaskStatus.COMPLETED, List.of(), List.of(), Instant.now(), Instant.now());
        Task pendingTask = new Task(UUID.randomUUID(), "Product development", "", 
            TaskStatus.PENDING, List.of(), List.of(), Instant.now(), null);
        
        List<Task> currentTasks = List.of(completedTask, pendingTask);
        
        when(resilientChatClient.call(eq("plan review"), any(String.class)))
            .thenReturn("NO_CHANGES");

        // When
        taskPlanAgent.reviewAndUpdatePlan(currentTasks, completedTask);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("plan review"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Market research");
        assertThat(capturedPrompt).contains("Discovered high demand");
        assertThat(capturedPrompt).contains("Product development");
        assertThat(capturedPrompt).contains("Number of Tasks Completed: 1");
    }

    @Test
    void createTaskPlanWithDependencies_ShouldSendCorrectPromptToAI() {
        // Given
        String userGoal = "Build a mobile app";
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn("TASKS:\n1. Design UI\n\nDEPENDENCIES:\n");

        // When
        taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(resilientChatClient).call(eq("task planning"), promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertThat(capturedPrompt).contains("Build a mobile app");
        assertThat(capturedPrompt).contains("Break down the following goal into 3-6 specific, actionable tasks");
        assertThat(capturedPrompt).contains("Key principles:");
        assertThat(capturedPrompt).contains("TASKS:");
        assertThat(capturedPrompt).contains("DEPENDENCIES:");
        assertThat(capturedPrompt).contains("(blocking)");
        assertThat(capturedPrompt).contains("(informational)");
    }

    @Test
    void createTaskPlanWithDependencies_WithTaskNumberingVariations_ShouldParseCorrectly() {
        // Given
        String userGoal = "Create documentation";
        String aiResponse = """
            TASKS:
            1. Research requirements
            2.   Write outline  
            3.Draft content
            
            DEPENDENCIES:
            Task 2 depends on Task 1 (blocking) - needs requirements for outline
            """;
        
        when(resilientChatClient.call(eq("task planning"), any(String.class)))
            .thenReturn(aiResponse);

        // When
        TaskPlan result = taskPlanAgent.createTaskPlanWithDependencies(userGoal);

        // Then
        assertThat(result.tasks()).hasSize(3);
        assertThat(result.tasks().get(0).description()).isEqualTo("Research requirements");
        assertThat(result.tasks().get(1).description()).isEqualTo("Write outline");
        assertThat(result.tasks().get(2).description()).isEqualTo("Draft content");
        
        // Check dependency was parsed correctly despite spacing variations
        assertThat(result.dependencies()).hasSize(1);
        assertThat(result.tasks().get(1).blockingDependencies())
            .containsExactly(result.tasks().get(0).id());
    }
}