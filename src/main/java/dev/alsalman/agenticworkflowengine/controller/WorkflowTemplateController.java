package dev.alsalman.agenticworkflowengine.controller;

import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.domain.WorkflowTemplate;
import dev.alsalman.agenticworkflowengine.service.WorkflowTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow/templates")
@CrossOrigin(origins = "*")
public class WorkflowTemplateController {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateController.class);
    
    private final WorkflowTemplateService templateService;
    
    public WorkflowTemplateController(WorkflowTemplateService templateService) {
        this.templateService = templateService;
    }
    
    @GetMapping
    public ResponseEntity<List<WorkflowTemplate>> listTemplates(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) String search
    ) {
        log.info("Listing templates - category: {}, tags: {}, search: {}", category, tags, search);
        List<WorkflowTemplate> templates = templateService.searchTemplates(category, tags, search);
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/{templateId}")
    public ResponseEntity<WorkflowTemplate> getTemplate(@PathVariable UUID templateId) {
        log.info("Getting template: {}", templateId);
        try {
            WorkflowTemplate template = templateService.getTemplate(templateId);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            log.error("Template not found: {}", templateId);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<WorkflowTemplate> createTemplate(@RequestBody WorkflowTemplate template) {
        log.info("Creating new template: {}", template.name());
        try {
            WorkflowTemplate created = templateService.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Invalid template: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{templateId}/execute")
    public ResponseEntity<ExecuteTemplateResponse> executeTemplate(
        @PathVariable UUID templateId,
        @RequestBody Map<String, Object> parameters,
        @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        log.info("Executing template {} with parameters: {}", templateId, parameters);
        try {
            WorkflowResult result = templateService.executeTemplate(templateId, parameters, userId);
            
            if (result.status() == WorkflowResult.Status.SUCCESS) {
                return ResponseEntity.ok(new ExecuteTemplateResponse(
                    result.goal().id(),
                    "Template execution started successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ExecuteTemplateResponse(null, "Template execution failed: " + result.error()));
            }
        } catch (IllegalArgumentException e) {
            log.error("Failed to execute template: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ExecuteTemplateResponse(null, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error executing template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExecuteTemplateResponse(null, "Internal server error"));
        }
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        log.info("Getting template categories");
        List<String> categories = templateService.getCategories();
        return ResponseEntity.ok(categories);
    }
    
    @PostMapping("/{templateId}/validate")
    public ResponseEntity<ValidationResponse> validateParameters(
        @PathVariable UUID templateId,
        @RequestBody Map<String, Object> parameters
    ) {
        log.info("Validating parameters for template: {}", templateId);
        try {
            WorkflowTemplate template = templateService.getTemplate(templateId);
            WorkflowTemplateService.ValidationResult result = templateService.validateParameters(template, parameters);
            
            return ResponseEntity.ok(new ValidationResponse(
                result.isValid(),
                result.errors()
            ));
        } catch (IllegalArgumentException e) {
            log.error("Template not found: {}", templateId);
            return ResponseEntity.notFound().build();
        }
    }
    
    public record ExecuteTemplateResponse(
        UUID goalId,
        String message
    ) {}
    
    public record ValidationResponse(
        boolean valid,
        List<String> errors
    ) {}
}