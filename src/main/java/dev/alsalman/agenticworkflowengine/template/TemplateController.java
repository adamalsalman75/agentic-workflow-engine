package dev.alsalman.agenticworkflowengine.template;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.WorkflowTemplate;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.template.TemplateService;
import dev.alsalman.agenticworkflowengine.template.dto.ParameterDiscoveryResponseDto;
import dev.alsalman.agenticworkflowengine.template.service.ParameterPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "*")
public class TemplateController {
    
    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);
    
    private final TemplateService templateService;
    private final ParameterPersistenceService parameterPersistenceService;
    
    public TemplateController(TemplateService templateService, 
                            ParameterPersistenceService parameterPersistenceService) {
        this.templateService = templateService;
        this.parameterPersistenceService = parameterPersistenceService;
    }
    
    @GetMapping
    public ResponseEntity<List<WorkflowTemplate>> listTemplates() {
        log.info("Listing simple templates");
        List<WorkflowTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }
    
    @GetMapping("/{templateId}")
    public ResponseEntity<WorkflowTemplate> getTemplate(@PathVariable UUID templateId) {
        log.info("Getting template: {}", templateId);
        try {
            WorkflowTemplate template = templateService.getTemplate(templateId);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{templateId}/parameters")
    public ResponseEntity<ParameterDiscoveryResponseDto> getTemplateParameters(@PathVariable UUID templateId) {
        log.info("Getting parameters for template: {}", templateId);
        try {
            WorkflowTemplate template = templateService.getTemplate(templateId);
            var parametersWithMetadata = parameterPersistenceService.loadParametersWithMetadata(templateId);
            
            ParameterDiscoveryResponseDto response = new ParameterDiscoveryResponseDto(
                templateId, 
                template.name(), 
                parametersWithMetadata
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{templateId}/execute")
    public ResponseEntity<ExecuteResponse> executeTemplate(
        @PathVariable UUID templateId,
        @RequestBody Map<String, Object> parameters
    ) {
        log.info("Executing template {} with parameters: {}", templateId, parameters);
        try {
            WorkflowResult result = templateService.executeTemplate(templateId, parameters);
            
            if (result.success()) {
                return ResponseEntity.ok(new ExecuteResponse(
                    result.goal().id(),
                    "Template executed successfully",
                    true
                ));
            } else {
                return ResponseEntity.ok(new ExecuteResponse(
                    null,
                    "Template execution failed", 
                    false
                ));
            }
        } catch (IllegalArgumentException e) {
            log.error("Template execution failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ExecuteResponse(
                null,
                e.getMessage(),
                false
            ));
        } catch (Exception e) {
            log.error("Unexpected error executing template", e);
            return ResponseEntity.status(500).body(new ExecuteResponse(
                null,
                "Internal server error",
                false
            ));
        }
    }
    
    public record ExecuteResponse(
        UUID goalId,
        String message,
        boolean success
    ) {}
}