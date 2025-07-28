package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.domain.SimpleParameterType;
import dev.alsalman.agenticworkflowengine.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.repository.SimpleTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SimpleTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(SimpleTemplateService.class);
    
    private final SimpleTemplateRepository repository;
    private final WorkflowOrchestrator orchestrator;
    
    // Hardcoded templates for Phase 1
    private final List<SimpleParameter> TRIP_PARAMETERS = Arrays.asList(
        SimpleParameter.required("destination", "Where are you traveling to?", SimpleParameterType.STRING),
        SimpleParameter.required("duration", "Number of days", SimpleParameterType.NUMBER),
        SimpleParameter.optional("budget", "Budget level", SimpleParameterType.SELECTION, "Mid-range")
    );
    
    public SimpleTemplateService(SimpleTemplateRepository repository, WorkflowOrchestrator orchestrator) {
        this.repository = repository;
        this.orchestrator = orchestrator;
    }
    
    @PostConstruct
    public void initializeTemplates() {
        log.info("Initializing Phase 1 simple templates...");
        
        try {
            SimpleWorkflowTemplate tripTemplate = SimpleWorkflowTemplate.create(
                "Simple Trip Planner",
                "Plan a basic trip with destination and duration",
                "Travel",
                """
                Plan a {{duration}}-day trip to {{destination}} with a {{budget}} budget.
                
                Include:
                1. Flight recommendations
                2. Hotel suggestions
                3. Daily activity suggestions
                4. Transportation options
                5. Must-see attractions
                """,
                "System"
            );
            
            repository.save(tripTemplate);
            log.info("Trip planning template created successfully");
            
        } catch (Exception e) {
            log.error("Error initializing simple templates", e);
        }
    }
    
    public List<SimpleWorkflowTemplate> getAllTemplates() {
        return repository.findByIsPublicTrue();
    }
    
    public SimpleWorkflowTemplate getTemplate(UUID templateId) {
        return repository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }
    
    public List<SimpleParameter> getTemplateParameters(UUID templateId) {
        // For Phase 1, return hardcoded parameters
        // In Phase 2, this would come from database
        SimpleWorkflowTemplate template = getTemplate(templateId);
        
        if (template.name().equals("Simple Trip Planner")) {
            return TRIP_PARAMETERS;
        }
        
        return List.of();
    }
    
    public WorkflowResult executeTemplate(UUID templateId, Map<String, Object> parameters) {
        log.info("Executing template {} with parameters: {}", templateId, parameters);
        
        SimpleWorkflowTemplate template = getTemplate(templateId);
        List<SimpleParameter> templateParams = getTemplateParameters(templateId);
        
        // Basic validation
        for (SimpleParameter param : templateParams) {
            if (param.required() && !parameters.containsKey(param.name())) {
                throw new IllegalArgumentException("Required parameter missing: " + param.name());
            }
        }
        
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