package dev.alsalman.agenticworkflowengine.template;

import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.template.domain.ParameterType;
import dev.alsalman.agenticworkflowengine.template.domain.WorkflowTemplate;
import dev.alsalman.agenticworkflowengine.template.domain.ValidationRule;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import dev.alsalman.agenticworkflowengine.template.repository.TemplateRepository;
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
public class TemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
    
    private final TemplateRepository repository;
    private final dev.alsalman.agenticworkflowengine.workflow.WorkflowOrchestrator orchestrator;
    private final AdvancedParameterValidator advancedValidator;
    
    // Hardcoded templates for Phase 1 & 2 with validation rules
    private final List<Parameter> TRIP_PARAMETERS = Arrays.asList(
        Parameter.requiredWithValidation("destination", "Where are you traveling to?", ParameterType.LOCATION,
            List.of(ValidationRule.pattern("^[A-Za-z\\s,.-]+$", "Please enter a valid location (letters, spaces, commas, periods, and hyphens only)"))),
        Parameter.requiredWithValidation("startDate", "Departure date", ParameterType.DATE, 
            List.of(ValidationRule.dateRange(LocalDate.now(), null, "Departure date cannot be in the past"))),
        Parameter.requiredWithValidation("duration", "Number of days", ParameterType.NUMBER,
            List.of(ValidationRule.range(new BigDecimal("1"), new BigDecimal("365"), "Duration must be between 1 and 365 days"))),
        Parameter.optionalWithValidation("budget", "Total budget with currency", ParameterType.CURRENCY, "1000 USD",
            List.of(ValidationRule.pattern("^\\d+\\s*(USD|EUR|GBP|JPY|CAD|AUD)$", "Budget must be in format: amount + currency code (e.g., 1000 USD)"))),
        Parameter.optionalWithValidation("travelStyle", "Travel style preference", ParameterType.SELECTION, "Mid-range",
            List.of(ValidationRule.allowedValues(List.of("Budget", "Mid-range", "Luxury"), "Travel style must be Budget, Mid-range, or Luxury")))
    );
    
    public TemplateService(TemplateRepository repository, 
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
            List<WorkflowTemplate> existingTemplates = repository.findByName("Simple Trip Planner");
            
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
            
            WorkflowTemplate tripTemplate = WorkflowTemplate.create(
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
    
    public List<WorkflowTemplate> getAllTemplates() {
        return repository.findByIsPublicTrue();
    }
    
    public WorkflowTemplate getTemplate(UUID templateId) {
        return repository.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }
    
    public List<Parameter> getTemplateParameters(UUID templateId) {
        // For Phase 1, return hardcoded parameters
        // In Phase 2, this would come from database
        WorkflowTemplate template = getTemplate(templateId);
        
        if (template.name().equals("Simple Trip Planner")) {
            return TRIP_PARAMETERS;
        }
        
        return List.of();
    }
    
    public WorkflowResult executeTemplate(UUID templateId, Map<String, Object> parameters) {
        log.info("Executing template {} with parameters: {}", templateId, parameters);
        
        WorkflowTemplate template = getTemplate(templateId);
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