package dev.alsalman.agenticworkflowengine.controller;

import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.service.WorkflowOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    
    private final WorkflowOrchestrator workflowOrchestrator;
    
    public WorkflowController(WorkflowOrchestrator workflowOrchestrator) {
        this.workflowOrchestrator = workflowOrchestrator;
    }
    
    @PostMapping("/execute")
    public ResponseEntity<WorkflowResult> executeWorkflow(@RequestBody WorkflowRequest request) {
        try {
            WorkflowResult result = workflowOrchestrator.executeWorkflow(request.query());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    public record WorkflowRequest(String query) {}
}