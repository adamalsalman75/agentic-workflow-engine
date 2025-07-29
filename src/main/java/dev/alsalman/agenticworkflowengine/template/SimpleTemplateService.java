package dev.alsalman.agenticworkflowengine.template;

import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameter;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.SimpleWorkflowTemplate;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.template.repository.SimpleTemplateRepository;
import dev.alsalman.agenticworkflowengine.template.validation.ParameterValidator;
import dev.alsalman.agenticworkflowengine.template.validation.AdvancedParameterValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SimpleTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(SimpleTemplateService.class);
    
    private final SimpleTemplateRepository repository;
    private final dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator orchestrator;
    private final AdvancedParameterValidator advancedValidator;
    
    // Hardcoded templates for Phase 1 & 2 with validation rules
    private final List<SimpleParameter> TRIP_PARAMETERS = Arrays.asList(
        SimpleParameter.required("destination", "Where are you traveling to?", SimpleParameterType.LOCATION),
        SimpleParameter.requiredWithValidation("startDate", "Departure date", SimpleParameterType.DATE, 
            List.of(ValidationRule.dateRange(LocalDate.now(), null, "Departure date cannot be in the past"))),
        SimpleParameter.requiredWithValidation("duration", "Number of days", SimpleParameterType.NUMBER,
            List.of(ValidationRule.range(new BigDecimal("1"), new BigDecimal("365"), "Duration must be between 1 and 365 days"))),
        SimpleParameter.optional("budget", "Total budget with currency", SimpleParameterType.CURRENCY, "1000 USD"),
        SimpleParameter.optionalWithValidation("travelStyle", "Travel style preference", SimpleParameterType.SELECTION, "Mid-range",
            List.of(ValidationRule.allowedValues(List.of("Budget", "Mid-range", "Luxury"), "Travel style must be Budget, Mid-range, or Luxury")))
    );
    
    public SimpleTemplateService(SimpleTemplateRepository repository, 
                                dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator orchestrator,
                                AdvancedParameterValidator advancedValidator) {
        this.repository = repository;
        this.orchestrator = orchestrator;
        this.advancedValidator = advancedValidator;
    }
    
    @PostConstruct
    public void initializeTemplates() {
        log.info("Initializing Phase 1 simple templates...");
        
        try {
            // Check if template already exists by exact name match
            List<SimpleWorkflowTemplate> existingTemplates = repository.findByName("Simple Trip Planner");
            
            if (!existingTemplates.isEmpty()) {
                log.info("Trip planning template already exists ({} found), skipping initialization", existingTemplates.size());
                // Clean up any duplicates if more than one exists
                if (existingTemplates.size() > 1) {
                    log.warn("Found {} duplicate templates, keeping only the first one", existingTemplates.size());
                    for (int i = 1; i < existingTemplates.size(); i++) {
                        repository.deleteById(existingTemplates.get(i).id());
                        log.info("Deleted duplicate template with ID: {}", existingTemplates.get(i).id());
                    }
                }
                return;
            }
            
            SimpleWorkflowTemplate tripTemplate = SimpleWorkflowTemplate.create(
                "Simple Trip Planner",
                "Plan a comprehensive trip with dates, budget, and style preferences",
                "Travel",
                """
                Plan a {{duration}}-day trip to {{destination}} starting on {{startDate}} with a budget of {{budget}}.
                Travel style preference: {{travelStyle}}.
                
                Include:
                1. Flight recommendations from major US cities
                2. Hotel suggestions matching the {{travelStyle}} preference
                3. Daily activity itinerary with time estimates
                4. Local transportation options and costs
                5. Must-see attractions with ticket prices
                6. Restaurant recommendations for each day
                7. Weather considerations for {{startDate}}
                8. Budget breakdown in {{budget}} currency
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
        
        // Validate all parameters
        List<String> validationErrors = new ArrayList<>();
        for (SimpleParameter param : templateParams) {
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