package dev.alsalman.agenticworkflowengine.controller;

import dev.alsalman.agenticworkflowengine.domain.ExecutionResponse;
import dev.alsalman.agenticworkflowengine.domain.Goal;
import dev.alsalman.agenticworkflowengine.domain.Task;
import dev.alsalman.agenticworkflowengine.service.WorkflowOrchestrator;
import dev.alsalman.agenticworkflowengine.service.WorkflowPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);
    
    private final WorkflowOrchestrator workflowOrchestrator;
    private final WorkflowPersistenceService persistenceService;
    
    public WorkflowController(WorkflowOrchestrator workflowOrchestrator, WorkflowPersistenceService persistenceService) {
        this.workflowOrchestrator = workflowOrchestrator;
        this.persistenceService = persistenceService;
    }
    
    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeWorkflow(@RequestBody WorkflowRequest request) {
        try {
            // Create initial goal and return goal ID immediately
            Goal goal = Goal.create(request.query());
            goal = persistenceService.saveGoal(goal);
            
            // Execute workflow asynchronously in virtual thread
            final Goal finalGoal = goal;
            final String query = request.query();
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("Starting async workflow execution for goal: {}", finalGoal.id());
                    workflowOrchestrator.executeWorkflow(query, finalGoal.id());
                    log.info("Completed async workflow execution for goal: {}", finalGoal.id());
                } catch (Exception e) {
                    log.error("Async workflow execution failed for goal: {}", finalGoal.id(), e);
                }
            }, Executors.newVirtualThreadPerTaskExecutor());
            
            return ResponseEntity.ok(ExecutionResponse.success(goal.id()));
        } catch (Exception e) {
            log.error("Failed to start workflow execution", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/goal/{goalId}")
    public ResponseEntity<Goal> getGoal(@PathVariable UUID goalId) {
        try {
            Goal goal = persistenceService.findGoalById(goalId);
            if (goal == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(goal);
        } catch (Exception e) {
            log.error("Failed to retrieve goal: {}", goalId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/goal/{goalId}/tasks")
    public ResponseEntity<List<Task>> getGoalTasks(@PathVariable UUID goalId) {
        try {
            List<Task> tasks = persistenceService.findTasksByGoalId(goalId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Failed to retrieve tasks for goal: {}", goalId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    public record WorkflowRequest(String query) {}
}