package dev.alsalman.agenticworkflowengine.template.service;

import dev.alsalman.agenticworkflowengine.template.domain.*;
import dev.alsalman.agenticworkflowengine.template.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling parameter persistence operations
 */
@Service
public class ParameterPersistenceService {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterPersistenceService.class);
    
    private final TemplateParameterRepository parameterRepository;
    private final ParameterMetadataRepository metadataRepository;
    private final ParameterValidationRuleRepository validationRuleRepository;
    
    public ParameterPersistenceService(
            TemplateParameterRepository parameterRepository,
            ParameterMetadataRepository metadataRepository,
            ParameterValidationRuleRepository validationRuleRepository) {
        this.parameterRepository = parameterRepository;
        this.metadataRepository = metadataRepository;
        this.validationRuleRepository = validationRuleRepository;
    }
    
    /**
     * Save a list of parameters for a template
     */
    @Transactional
    public void saveTemplateParameters(UUID templateId, List<Parameter> parameters) {
        log.info("Saving {} parameters for template {}", parameters.size(), templateId);
        
        // Delete existing parameters for this template
        parameterRepository.deleteByTemplateId(templateId);
        
        int order = 0;
        for (Parameter param : parameters) {
            // Save the parameter
            TemplateParameter templateParam = TemplateParameter.create(
                templateId,
                param.name(),
                param.description(),
                param.type(),
                param.required(),
                param.defaultValue(),
                order++
            );
            TemplateParameter savedParam = parameterRepository.save(templateParam);
            
            // Save metadata (generate default metadata based on type)
            ParameterMetadata metadata = generateDefaultMetadata(savedParam);
            metadataRepository.save(metadata);
            
            // Save validation rules
            if (param.validationRules() != null && !param.validationRules().isEmpty()) {
                saveValidationRules(savedParam.id(), param.validationRules());
            }
        }
        
        log.info("Successfully saved parameters for template {}", templateId);
    }
    
    /**
     * Load parameters for a template with all related data
     */
    @Transactional(readOnly = true)
    public List<Parameter> loadTemplateParameters(UUID templateId) {
        List<TemplateParameter> templateParams = parameterRepository.findByTemplateIdOrderByDisplayOrder(templateId);
        List<Parameter> parameters = new ArrayList<>();
        
        for (TemplateParameter templateParam : templateParams) {
            // Load validation rules
            List<ParameterValidationRule> validationRules = validationRuleRepository.findByParameterId(templateParam.id());
            List<ValidationRule> domainRules = validationRules.stream()
                .map(ParameterValidationRule::toDomainValidationRule)
                .toList();
            
            // Create domain parameter with validation rules
            Parameter param = new Parameter(
                templateParam.name(),
                templateParam.description(),
                ParameterType.valueOf(templateParam.type()),
                templateParam.required(),
                templateParam.defaultValue(),
                domainRules
            );
            parameters.add(param);
        }
        
        return parameters;
    }
    
    /**
     * Load parameter with metadata for API response
     */
    @Transactional(readOnly = true)
    public List<dev.alsalman.agenticworkflowengine.template.dto.ParameterResponseDto> loadParametersWithMetadata(UUID templateId) {
        List<TemplateParameter> templateParams = parameterRepository.findByTemplateIdOrderByDisplayOrder(templateId);
        List<dev.alsalman.agenticworkflowengine.template.dto.ParameterResponseDto> responses = new ArrayList<>();
        
        for (TemplateParameter templateParam : templateParams) {
            // Load validation rules
            List<ParameterValidationRule> validationRules = validationRuleRepository.findByParameterId(templateParam.id());
            List<dev.alsalman.agenticworkflowengine.template.dto.ValidationRuleDto> ruleDtos = validationRules.stream()
                .map(rule -> {
                    ValidationRule domainRule = rule.toDomainValidationRule();
                    return dev.alsalman.agenticworkflowengine.template.dto.ValidationRuleDto.fromValidationRule(domainRule);
                })
                .toList();
            
            // Load metadata
            ParameterMetadata metadata = metadataRepository.findByParameterId(templateParam.id()).orElse(null);
            dev.alsalman.agenticworkflowengine.template.dto.ParameterMetadataDto metadataDto = null;
            if (metadata != null) {
                metadataDto = metadata.toDto(templateParam.displayOrder() + 1);
            }
            
            // Create response DTO
            dev.alsalman.agenticworkflowengine.template.dto.ParameterResponseDto responseDto = 
                new dev.alsalman.agenticworkflowengine.template.dto.ParameterResponseDto(
                    templateParam.name(),
                    templateParam.description(),
                    ParameterType.valueOf(templateParam.type()),
                    templateParam.required(),
                    templateParam.defaultValue(),
                    ruleDtos,
                    metadataDto
                );
            responses.add(responseDto);
        }
        
        return responses;
    }
    
    private void saveValidationRules(UUID parameterId, List<ValidationRule> rules) {
        for (ValidationRule rule : rules) {
            ParameterValidationRule dbRule = ParameterValidationRule.fromDomainValidationRule(parameterId, rule);
            validationRuleRepository.save(dbRule);
        }
    }
    
    private ParameterMetadata generateDefaultMetadata(TemplateParameter param) {
        ParameterType type = ParameterType.valueOf(param.type());
        String placeholder = generatePlaceholder(param.name(), type);
        String helpText = generateHelpText(param.name(), type);
        String group = determineGroup(param.name(), type);
        
        return ParameterMetadata.create(
            param.id(),
            placeholder,
            helpText,
            group,
            null, // UI component can be determined by frontend
            null  // No additional properties for now
        );
    }
    
    private String generatePlaceholder(String paramName, ParameterType type) {
        return switch (type) {
            case LOCATION -> "Paris, France";
            case DATE -> "2025-12-25";
            case NUMBER -> "5";
            case CURRENCY -> "1000 USD";
            case SELECTION -> "Select an option";
            case ParameterType.EMAIL -> "user@example.com";
            case ParameterType.URL -> "https://example.com";
            case ParameterType.PERCENTAGE -> "75";
            case ParameterType.PHONE -> "+1 234-567-8900";
            case ParameterType.TIME -> "14:30";
            case ParameterType.DURATION -> "2 hours";
            default -> "Enter " + paramName.toLowerCase();
        };
    }
    
    private String generateHelpText(String paramName, ParameterType type) {
        return switch (type) {
            case LOCATION -> "Enter a city, state, or country";
            case DATE -> "Format: YYYY-MM-DD";
            case NUMBER -> "Enter a numeric value";
            case CURRENCY -> "Amount with currency code (e.g., 1000 USD)";
            case SELECTION -> "Choose from available options";
            case ParameterType.EMAIL -> "Valid email address";
            case ParameterType.URL -> "Full URL including https://";
            case ParameterType.PERCENTAGE -> "Value between 0 and 100";
            case ParameterType.PHONE -> "Include country code";
            case ParameterType.TIME -> "24-hour format (HH:MM)";
            case ParameterType.DURATION -> "e.g., '2 hours', '30 minutes'";
            default -> "Provide " + paramName.toLowerCase();
        };
    }
    
    private String determineGroup(String paramName, ParameterType type) {
        String lowerName = paramName.toLowerCase();
        
        if (lowerName.contains("location") || lowerName.contains("destination") || 
            lowerName.contains("address") || type == ParameterType.LOCATION) {
            return "Location";
        }
        if (lowerName.contains("date") || lowerName.contains("time") || 
            type == ParameterType.DATE || type == ParameterType.TIME) {
            return "Dates";
        }
        if (lowerName.contains("budget") || lowerName.contains("cost") || 
            lowerName.contains("price") || type == ParameterType.CURRENCY) {
            return "Budget";
        }
        if (lowerName.contains("style") || lowerName.contains("preference") || 
            type == ParameterType.SELECTION) {
            return "Preferences";
        }
        
        return "General";
    }
}