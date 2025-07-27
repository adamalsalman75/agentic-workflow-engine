package dev.alsalman.agenticworkflowengine.service;

import dev.alsalman.agenticworkflowengine.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    
    private final WorkflowTemplatePersistenceService persistenceService;
    private final WorkflowOrchestrator workflowOrchestrator;
    
    public WorkflowTemplateService(
        WorkflowTemplatePersistenceService persistenceService,
        WorkflowOrchestrator workflowOrchestrator
    ) {
        this.persistenceService = persistenceService;
        this.workflowOrchestrator = workflowOrchestrator;
    }
    
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        log.info("Creating workflow template: {}", template.name());
        
        // Validate template structure
        validateTemplate(template);
        
        // Save template and parameters
        WorkflowTemplate savedTemplate = persistenceService.saveTemplate(template);
        log.info("Template created with ID: {}", savedTemplate.id());
        
        return savedTemplate;
    }
    
    public WorkflowTemplate getTemplate(UUID templateId) {
        return persistenceService.findTemplateById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
    }
    
    public List<WorkflowTemplate> searchTemplates(String category, List<String> tags, String searchText) {
        log.debug("Searching templates - category: {}, tags: {}, search: {}", category, tags, searchText);
        return persistenceService.searchTemplates(category, tags, searchText);
    }
    
    public List<String> getCategories() {
        return persistenceService.findAllCategories();
    }
    
    public WorkflowResult executeTemplate(UUID templateId, Map<String, Object> providedParameters, String userId) {
        log.info("Executing template {} with parameters: {}", templateId, providedParameters);
        
        // Load template
        WorkflowTemplate template = getTemplate(templateId);
        
        // Validate parameters
        ValidationResult validation = validateParameters(template, providedParameters);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid parameters: " + validation.getErrors());
        }
        
        // Merge with defaults
        Map<String, Object> finalParameters = mergeWithDefaults(template, providedParameters);
        
        // Render prompt
        String renderedPrompt = renderPrompt(template.promptTemplate(), finalParameters);
        log.info("Rendered prompt: {}", renderedPrompt);
        
        // Execute workflow
        WorkflowResult result = workflowOrchestrator.executeWorkflow(renderedPrompt, null);
        
        // Record execution
        if (result.status() == WorkflowResult.Status.SUCCESS) {
            TemplateExecution execution = TemplateExecution.create(
                templateId, result.goal().id(), finalParameters, userId
            );
            persistenceService.saveExecution(execution);
            persistenceService.incrementUsageCount(templateId);
        }
        
        return result;
    }
    
    private void validateTemplate(WorkflowTemplate template) {
        // Check for placeholder consistency
        Set<String> placeholders = extractPlaceholders(template.promptTemplate());
        Set<String> parameterNames = template.parameters().stream()
            .map(TemplateParameter::name)
            .collect(Collectors.toSet());
        
        Set<String> unusedParameters = new HashSet<>(parameterNames);
        unusedParameters.removeAll(placeholders);
        
        Set<String> missingParameters = new HashSet<>(placeholders);
        missingParameters.removeAll(parameterNames);
        
        if (!unusedParameters.isEmpty()) {
            log.warn("Template {} has unused parameters: {}", template.name(), unusedParameters);
        }
        
        if (!missingParameters.isEmpty()) {
            throw new IllegalArgumentException(
                "Template has placeholders without corresponding parameters: " + missingParameters
            );
        }
    }
    
    private Set<String> extractPlaceholders(String template) {
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }
    
    public ValidationResult validateParameters(WorkflowTemplate template, Map<String, Object> providedParams) {
        List<String> errors = new ArrayList<>();
        
        for (TemplateParameter param : template.parameters()) {
            Object value = providedParams.get(param.name());
            
            // Check required parameters
            if (param.required() && (value == null || value.toString().isBlank())) {
                errors.add(String.format("Required parameter '%s' is missing", param.name()));
                continue;
            }
            
            if (value == null) {
                continue; // Optional parameter not provided
            }
            
            // Type validation
            if (!validateParameterType(param.type(), value)) {
                errors.add(String.format("Parameter '%s' has invalid type. Expected: %s", 
                    param.name(), param.type()));
            }
            
            // Allowed values validation
            if (!param.allowedValues().isEmpty() && !param.allowedValues().contains(value.toString())) {
                errors.add(String.format("Parameter '%s' value '%s' is not in allowed values: %s",
                    param.name(), value, param.allowedValues()));
            }
            
            // Custom validation rules
            if (!param.validation().isEmpty()) {
                validateCustomRules(param, value, errors);
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private boolean validateParameterType(ParameterType type, Object value) {
        return switch (type) {
            case STRING, LOCATION, EMAIL, URL -> value instanceof String;
            case NUMBER, CURRENCY -> value instanceof Number;
            case DATE -> value instanceof String && isValidDate(value.toString());
            case BOOLEAN -> value instanceof Boolean;
            case SELECTION -> value instanceof String;
            case DURATION -> value instanceof Number && ((Number) value).intValue() > 0;
        };
    }
    
    private boolean isValidDate(String date) {
        // Simple date validation - could be enhanced
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
    
    private void validateCustomRules(TemplateParameter param, Object value, List<String> errors) {
        Map<String, Object> rules = param.validation();
        
        if (value instanceof Number numberValue) {
            Object min = rules.get("min");
            if (min instanceof Number minNumber && numberValue.doubleValue() < minNumber.doubleValue()) {
                errors.add(String.format("Parameter '%s' value %s is below minimum %s", 
                    param.name(), value, min));
            }
            
            Object max = rules.get("max");
            if (max instanceof Number maxNumber && numberValue.doubleValue() > maxNumber.doubleValue()) {
                errors.add(String.format("Parameter '%s' value %s is above maximum %s", 
                    param.name(), value, max));
            }
        }
        
        if (value instanceof String stringValue) {
            Object minLength = rules.get("minLength");
            if (minLength instanceof Number minLen && stringValue.length() < minLen.intValue()) {
                errors.add(String.format("Parameter '%s' is too short. Minimum length: %s", 
                    param.name(), minLength));
            }
            
            Object pattern = rules.get("pattern");
            if (pattern instanceof String regex && !stringValue.matches(regex)) {
                errors.add(String.format("Parameter '%s' does not match required pattern", param.name()));
            }
        }
    }
    
    private Map<String, Object> mergeWithDefaults(WorkflowTemplate template, Map<String, Object> provided) {
        Map<String, Object> merged = new HashMap<>(provided);
        
        for (TemplateParameter param : template.parameters()) {
            if (!merged.containsKey(param.name()) && param.defaultValue() != null) {
                merged.put(param.name(), param.defaultValue());
            }
        }
        
        return merged;
    }
    
    public String renderPrompt(String promptTemplate, Map<String, Object> parameters) {
        String rendered = promptTemplate;
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(placeholder, value);
        }
        
        // Check for any remaining placeholders
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(rendered);
        if (matcher.find()) {
            log.warn("Unresolved placeholders in rendered prompt: {}", matcher.group());
        }
        
        return rendered;
    }
    
    public record ValidationResult(boolean isValid, List<String> errors) {
        public String getErrors() {
            return String.join("; ", errors);
        }
    }
}