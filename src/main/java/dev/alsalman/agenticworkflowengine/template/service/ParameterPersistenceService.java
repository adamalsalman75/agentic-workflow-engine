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
            case TEXT -> determineTextPlaceholder(paramName);
            case NUMBER -> "5";
            case BOOLEAN -> "false";
            case SELECTION -> "Select an option";
        };
    }
    
    private String determineTextPlaceholder(String paramName) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("email")) return "user@example.com";
        if (lowerName.contains("date")) return "June 2025";
        if (lowerName.contains("location") || lowerName.contains("address")) return "San Francisco, CA";
        if (lowerName.contains("budget") || lowerName.contains("amount") || lowerName.contains("cost")) return "1000 USD";
        if (lowerName.contains("phone")) return "+1 234-567-8900";
        if (lowerName.contains("url") || lowerName.contains("website")) return "https://example.com";
        return "Enter " + paramName.toLowerCase().replace("_", " ");
    }
    
    private String generateHelpText(String paramName, ParameterType type) {
        return switch (type) {
            case TEXT -> determineTextHelpText(paramName);
            case NUMBER -> "Enter a numeric value";
            case BOOLEAN -> "Select true or false";
            case SELECTION -> "Choose from available options";
        };
    }
    
    private String determineTextHelpText(String paramName) {
        String lowerName = paramName.toLowerCase();
        if (lowerName.contains("email")) return "Enter email address in any format";
        if (lowerName.contains("date")) return "Enter date in any format (e.g., June 2025, 2025-06-01)";
        if (lowerName.contains("location") || lowerName.contains("address")) return "Enter city, state, country in any format";
        if (lowerName.contains("budget") || lowerName.contains("amount") || lowerName.contains("cost")) return "Enter amount in any format (e.g., 1000 USD, $1,000)";
        if (lowerName.contains("phone")) return "Enter phone number in any format";
        if (lowerName.contains("url") || lowerName.contains("website")) return "Enter website URL";
        return "Enter " + paramName.toLowerCase().replace("_", " ");
    }
    
    private String determineGroup(String paramName, ParameterType type) {
        String lowerName = paramName.toLowerCase();
        
        if (lowerName.contains("location") || lowerName.contains("destination") || 
            lowerName.contains("address")) {
            return "Location";
        }
        if (lowerName.contains("date") || lowerName.contains("time")) {
            return "Dates";
        }
        if (lowerName.contains("budget") || lowerName.contains("cost") || 
            lowerName.contains("price") || lowerName.contains("amount")) {
            return "Budget";
        }
        if (lowerName.contains("style") || lowerName.contains("preference") || 
            type == ParameterType.SELECTION) {
            return "Preferences";
        }
        
        return "General";
    }
}