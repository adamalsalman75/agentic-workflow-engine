package dev.alsalman.agenticworkflowengine.template.repository;

import dev.alsalman.agenticworkflowengine.template.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
// Note: Requires Docker PostgreSQL to be running (docker-compose up -d postgres)
class ParameterRepositoryIntegrationTest {
    
    @Autowired
    private TemplateRepository templateRepository;
    
    @Autowired
    private TemplateParameterRepository parameterRepository;
    
    @Autowired
    private ParameterMetadataRepository metadataRepository;
    
    @Autowired
    private ParameterValidationRuleRepository validationRuleRepository;
    
    private WorkflowTemplate testTemplate;
    
    @BeforeEach
    void setUp() {
        // Clean up
        validationRuleRepository.deleteAll();
        metadataRepository.deleteAll();
        parameterRepository.deleteAll();
        templateRepository.deleteAll();
        
        // Create test template
        testTemplate = templateRepository.save(WorkflowTemplate.create(
            "Test Template",
            "Test Description",
            "Test Category",
            "Test prompt {{param1}}",
            "Test Author"
        ));
    }
    
    @Test
    void shouldSaveAndLoadTemplateParameters() {
        // Given
        TemplateParameter param1 = TemplateParameter.create(
            testTemplate.id(),
            "destination",
            "Travel destination",
            ParameterType.TEXT,
            true,
            null,
            0
        );
        
        TemplateParameter param2 = TemplateParameter.create(
            testTemplate.id(),
            "budget",
            "Travel budget",
            ParameterType.TEXT,
            false,
            "1000 USD",
            1
        );
        
        // When
        TemplateParameter saved1 = parameterRepository.save(param1);
        TemplateParameter saved2 = parameterRepository.save(param2);
        
        // Then
        assertNotNull(saved1.id());
        assertNotNull(saved2.id());
        
        List<TemplateParameter> loaded = parameterRepository.findByTemplateIdOrderByDisplayOrder(testTemplate.id());
        assertEquals(2, loaded.size());
        assertEquals("destination", loaded.get(0).name());
        assertEquals("budget", loaded.get(1).name());
    }
    
    @Test
    void shouldSaveAndLoadParameterMetadata() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "startDate",
            "Start date",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterMetadata metadata = ParameterMetadata.create(
            parameter.id(),
            "2025-12-25",
            "Format: YYYY-MM-DD",
            "Dates",
            "datepicker",
            Map.of("minDate", "today")
        );
        
        // When
        ParameterMetadata saved = metadataRepository.save(metadata);
        
        // Then
        assertNotNull(saved.id());
        
        ParameterMetadata loaded = metadataRepository.findByParameterId(parameter.id()).orElse(null);
        assertNotNull(loaded);
        assertEquals("2025-12-25", loaded.placeholder());
        assertEquals("Dates", loaded.displayGroup());
        assertEquals("today", loaded.getAdditionalPropertiesAsMap().get("minDate"));
    }
    
    @Test
    void shouldSaveAndLoadValidationRules() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "email",
            "Email address",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterValidationRule rule1 = ParameterValidationRule.create(
            parameter.id(),
            "REQUIRED",
            Map.of(),
            "Email is required"
        );
        
        ParameterValidationRule rule2 = ParameterValidationRule.create(
            parameter.id(),
            "ALLOWED_VALUES",
            Map.of("allowedValues", List.of("user@example.com", "admin@company.com")),
            "Email must be valid"
        );
        
        // When
        validationRuleRepository.save(rule1);
        validationRuleRepository.save(rule2);
        
        // Then
        List<ParameterValidationRule> loaded = validationRuleRepository.findByParameterId(parameter.id());
        assertEquals(2, loaded.size());
        assertEquals(2, validationRuleRepository.countByParameterId(parameter.id()));
    }
    
    @Test
    void shouldDeleteParametersWhenTemplateDeleted() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "test",
            "Test param",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterMetadata metadata = metadataRepository.save(ParameterMetadata.create(
            parameter.id(),
            "placeholder",
            "help",
            "group",
            null,
            null
        ));
        
        ParameterValidationRule rule = validationRuleRepository.save(ParameterValidationRule.create(
            parameter.id(),
            "REQUIRED",
            Map.of(),
            "Parameter is required"
        ));
        
        // When
        templateRepository.deleteById(testTemplate.id());
        
        // Then
        assertEquals(0, parameterRepository.findByTemplateIdOrderByDisplayOrder(testTemplate.id()).size());
        assertFalse(metadataRepository.existsByParameterId(parameter.id()));
        assertEquals(0, validationRuleRepository.countByParameterId(parameter.id()));
    }
    
    @Test
    void shouldFindParameterByTemplateIdAndName() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "uniqueName",
            "Unique parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        // When
        TemplateParameter found = parameterRepository.findByTemplateIdAndName(testTemplate.id(), "uniqueName");
        
        // Then
        assertNotNull(found);
        assertEquals(parameter.id(), found.id());
        assertTrue(parameterRepository.existsByTemplateIdAndName(testTemplate.id(), "uniqueName"));
        assertFalse(parameterRepository.existsByTemplateIdAndName(testTemplate.id(), "nonexistent"));
    }
    
    @Test
    void shouldHandleJsonConversionInValidationRules() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "testParam",
            "Test parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterValidationRule rule = ParameterValidationRule.create(
            parameter.id(),
            "REQUIRED",
            Map.of("customMessage", "Test validation"),
            "Test validation"
        );
        
        // When
        ParameterValidationRule saved = validationRuleRepository.save(rule);
        
        // Then
        assertNotNull(saved.ruleValue());
        assertTrue(saved.ruleValue().contains("customMessage"));
        
        // Test JSON deserialization
        var ruleMap = saved.getRuleValueAsMap();
        assertEquals("Test validation", ruleMap.get("customMessage"));
    }
    
    @Test
    void shouldHandleJsonConversionInMetadata() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "testParam",
            "Test parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterMetadata metadata = ParameterMetadata.create(
            parameter.id(),
            "Test placeholder",
            "Test help",
            "Test group",
            "input",
            Map.of("minLength", 5, "maxLength", 100, "pattern", "text")
        );
        
        // When
        ParameterMetadata saved = metadataRepository.save(metadata);
        
        // Then
        assertNotNull(saved.additionalProperties());
        assertTrue(saved.additionalProperties().contains("minLength"));
        
        // Test JSON deserialization
        var propsMap = saved.getAdditionalPropertiesAsMap();
        assertEquals(5, propsMap.get("minLength"));
        assertEquals(100, propsMap.get("maxLength"));
        assertEquals("text", propsMap.get("pattern"));
    }
    
    @Test
    void shouldHandleEmptyAdditionalProperties() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "testParam",
            "Test parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterMetadata metadata = ParameterMetadata.create(
            parameter.id(),
            "Test placeholder",
            "Test help",
            "Test group",
            "input",
            null // Empty additional properties
        );
        
        // When
        ParameterMetadata saved = metadataRepository.save(metadata);
        
        // Then
        var propsMap = saved.getAdditionalPropertiesAsMap();
        assertTrue(propsMap.isEmpty());
    }
    
    @Test
    void shouldHandleValidationRuleJsonDeserialization() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "testParam",
            "Test parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterValidationRule rule = ParameterValidationRule.create(
            parameter.id(),
            "ALLOWED_VALUES",
            Map.of("allowedValues", List.of("0", "50", "100")),
            "Value must be 0, 50, or 100"
        );
        
        ParameterValidationRule saved = validationRuleRepository.save(rule);
        
        // When
        var ruleMap = saved.getRuleValueAsMap();
        
        // Then
        var allowedValues = (List<?>) ruleMap.get("allowedValues");
        assertEquals(3, allowedValues.size());
        assertTrue(allowedValues.contains("0"));
        assertTrue(allowedValues.contains("50"));
        assertTrue(allowedValues.contains("100"));
    }
    
    @Test
    void shouldHandleMetadataJsonDeserialization() {
        // Given
        TemplateParameter parameter = parameterRepository.save(TemplateParameter.create(
            testTemplate.id(),
            "testParam",
            "Test parameter",
            ParameterType.TEXT,
            true,
            null,
            0
        ));
        
        ParameterMetadata metadata = ParameterMetadata.create(
            parameter.id(),
            "Test placeholder",
            "Test help",
            "Test group",
            "select",
            Map.of("options", List.of("A", "B", "C"), "multiple", true)
        );
        
        ParameterMetadata saved = metadataRepository.save(metadata);
        
        // When
        var propsMap = saved.getAdditionalPropertiesAsMap();
        
        // Then
        var options = (List<?>) propsMap.get("options");
        assertEquals(3, options.size());
        assertTrue((Boolean) propsMap.get("multiple"));
    }
}