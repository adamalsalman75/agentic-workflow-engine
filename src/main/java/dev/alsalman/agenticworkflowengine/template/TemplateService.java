package dev.alsalman.agenticworkflowengine.template;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.WorkflowTemplate;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.template.repository.TemplateRepository;
import dev.alsalman.agenticworkflowengine.template.validation.ParameterValidator;
import dev.alsalman.agenticworkflowengine.template.validation.AdvancedParameterValidator;
import dev.alsalman.agenticworkflowengine.template.service.ParameterPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
    
    private final TemplateRepository repository;
    private final dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator orchestrator;
    private final AdvancedParameterValidator advancedValidator;
    private final ParameterPersistenceService parameterPersistenceService;
    
    // Parameters are now stored in database via Flyway migrations
    
    public TemplateService(TemplateRepository repository, 
                                dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator orchestrator,
                                AdvancedParameterValidator advancedValidator,
                                ParameterPersistenceService parameterPersistenceService) {
        this.repository = repository;
        this.orchestrator = orchestrator;
        this.advancedValidator = advancedValidator;
        this.parameterPersistenceService = parameterPersistenceService;
    }
    
    // Template initialization is now handled by Flyway migrations
    // V3__migrate_trip_planner_parameters.sql creates the Trip Planner template and parameters
    
    public List<WorkflowTemplate> getAllTemplates() {
        return repository.findByIsPublicTrue();
    }
    
    public WorkflowTemplate getTemplate(UUID templateId) {
        return repository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }
    
    public List<Parameter> getTemplateParameters(UUID templateId) {
        // Load parameters from database
        return parameterPersistenceService.loadTemplateParameters(templateId);
    }
    
    public void validateParameters(UUID templateId, Map<String, Object> parameters) {
        List<Parameter> templateParams = getTemplateParameters(templateId);
        
        // Validate all parameters
        List<String> validationErrors = new ArrayList<>();
        for (Parameter param : templateParams) {
            Object value = parameters.get(param.name());
            
            // Use default value if not provided
            if (value == null && param.defaultValue() != null) {
                value = param.defaultValue();
                parameters.put(param.name(), value);
            }
            
            // Convert value to string for validation
            String stringValue = value != null ? value.toString() : null;
            
            // Use advanced validator for parameters with validation rules
            List<String> errors = advancedValidator.validateParameter(param, stringValue);
            validationErrors.addAll(errors);
        }
        
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Parameter validation failed: " + String.join("; ", validationErrors));
        }
    }
    
    public WorkflowResult executeTemplate(UUID templateId, Map<String, Object> parameters) {
        log.info("Executing template {} with parameters: {}", templateId, parameters);
        
        WorkflowTemplate template = getTemplate(templateId);
        
        // Validate parameters
        validateParameters(templateId, parameters);
        
        // Render prompt with parameters
        String renderedPrompt = renderPrompt(template.promptTemplate(), parameters);
        log.info("Rendered prompt: {}", renderedPrompt);
        
        // Execute through existing orchestrator
        return orchestrator.executeWorkflow(renderedPrompt, null);
    }
    
    private String renderPrompt(String template, Map<String, Object> parameters) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }
}