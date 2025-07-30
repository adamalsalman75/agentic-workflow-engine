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
            ParameterType.LOCATION,
            true,
            null,
            0
        );
        
        TemplateParameter param2 = TemplateParameter.create(
            testTemplate.id(),
            "budget",
            "Travel budget",
            ParameterType.CURRENCY,
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
            ParameterType.DATE,
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
            ParameterType.EMAIL,
            true,
            null,
            0
        ));
        
        ParameterValidationRule rule1 = ParameterValidationRule.create(
            parameter.id(),
            "PATTERN",
            Map.of("pattern", "^[\\w.-]+@[\\w.-]+\\.\\w+$"),
            "Invalid email format"
        );
        
        ParameterValidationRule rule2 = ParameterValidationRule.create(
            parameter.id(),
            "MIN_MAX",
            Map.of("min", 5, "max", 100),
            "Email must be between 5 and 100 characters"
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
            "PATTERN",
            Map.of("pattern", ".*"),
            "Error"
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
            "PATTERN",
            Map.of("pattern", "^test.*", "flags", "i"),
            "Test validation"
        );
        
        // When
        ParameterValidationRule saved = validationRuleRepository.save(rule);
        
        // Then
        assertNotNull(saved.ruleValue());
        assertTrue(saved.ruleValue().contains("pattern"));
        assertTrue(saved.ruleValue().contains("test.*"));
        
        // Test JSON deserialization
        var ruleMap = saved.getRuleValueAsMap();
        assertEquals("^test.*", ruleMap.get("pattern"));
        assertEquals("i", ruleMap.get("flags"));
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
            "RANGE",
            Map.of("min", 0, "max", 100, "step", 1),
            "Value must be between 0 and 100"
        );
        
        ParameterValidationRule saved = validationRuleRepository.save(rule);
        
        // When
        var ruleMap = saved.getRuleValueAsMap();
        
        // Then
        assertEquals(0, ruleMap.get("min"));
        assertEquals(100, ruleMap.get("max"));
        assertEquals(1, ruleMap.get("step"));
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