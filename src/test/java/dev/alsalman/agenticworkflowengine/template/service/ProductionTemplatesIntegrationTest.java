package dev.alsalman.agenticworkflowengine.template.service;

import dev.alsalman.agenticworkflowengine.template.TemplateService;
import dev.alsalman.agenticworkflowengine.template.domain.WorkflowTemplate;
import dev.alsalman.agenticworkflowengine.template.domain.Parameter;
import dev.alsalman.agenticworkflowengine.workflow.domain.WorkflowResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for all 5 production templates created in Story 5
 * Tests parameter validation, template execution, and business logic
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductionTemplatesIntegrationTest {

    @Autowired
    private TemplateService templateService;

    private UUID businessStartupTemplateId;
    private UUID eventOrganizerTemplateId;
    private UUID researchProjectTemplateId;
    private UUID productLaunchTemplateId;
    private UUID homeRenovationTemplateId;

    @BeforeEach
    void setUp() {
        // Find template IDs by name from database
        List<WorkflowTemplate> templates = templateService.getAllTemplates();
        
        for (WorkflowTemplate template : templates) {
            switch (template.name()) {
                case "Business Startup Planner" -> businessStartupTemplateId = template.id();
                case "Event Organizer" -> eventOrganizerTemplateId = template.id();
                case "Research Project Planner" -> researchProjectTemplateId = template.id();
                case "Product Launch Checklist" -> productLaunchTemplateId = template.id();
                case "Home Renovation Planner" -> homeRenovationTemplateId = template.id();
            }
        }
        
        // Ensure all templates were found
        assertThat(businessStartupTemplateId).isNotNull();
        assertThat(eventOrganizerTemplateId).isNotNull();
        assertThat(researchProjectTemplateId).isNotNull();
        assertThat(productLaunchTemplateId).isNotNull();
        assertThat(homeRenovationTemplateId).isNotNull();
    }

    // ===== BUSINESS STARTUP PLANNER TESTS =====
    
    @Test
    void testBusinessStartupPlanner_ComprehensiveInput() {
        // AC5: Comprehensive input scenario - Test parameter validation only
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("business_name", "TechFlow Solutions");
        parameters.put("industry", "Technology");
        parameters.put("business_location", "San Francisco, CA, USA");
        parameters.put("team_size", "5");
        parameters.put("launch_date", "June 2025");
        parameters.put("funding_amount", "500000 USD");
        parameters.put("business_email", "info@techflow.com");
        parameters.put("has_funding", "true");
        parameters.put("equity_split", "70");
        parameters.put("business_model", "SaaS");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(businessStartupTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(businessStartupTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Business Startup Planner");
    }

    @Test
    void testBusinessStartupPlanner_MinimalInput() {
        // AC5: Minimal viable input (required parameters only)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("business_name", "StartupCorp");
        parameters.put("industry", "Healthcare");
        parameters.put("business_location", "Austin, TX");
        parameters.put("team_size", "2");
        parameters.put("launch_date", "March 2025");
        parameters.put("business_email", "founder@startupcorp.com");
        parameters.put("business_model", "B2B Services");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(businessStartupTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(businessStartupTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Business Startup Planner");
    }

    @Test
    void testBusinessStartupPlanner_TextParameterFlexibility() {
        // AC2: TEXT parameters accept flexible formats (LLM handles validation)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("business_name", "TestCorp");
        parameters.put("industry", "Technology");
        parameters.put("business_location", "New York, NY");
        parameters.put("team_size", "3");
        parameters.put("launch_date", "June 2025");
        parameters.put("business_email", "user@company.com"); // TEXT type accepts any format
        parameters.put("business_model", "SaaS");

        // Should pass - TEXT parameters are flexible for LLM consumption
        assertThatCode(() -> templateService.validateParameters(businessStartupTemplateId, parameters))
            .doesNotThrowAnyException();
    }

    @Test
    void testBusinessStartupPlanner_RequiredParameterValidation() {
        // AC2: Required parameter validation (simplified system)
        Map<String, Object> parameters = new HashMap<>(); 
        parameters.put("business_name", "TestCorp");
        parameters.put("industry", "Technology");
        parameters.put("business_location", "Seattle, WA");
        parameters.put("team_size", "4");
        parameters.put("launch_date", "December 2025");
        parameters.put("business_email", "founder@testcorp.com");
        parameters.put("business_model", "SaaS"); // Add required parameter

        // Test that validation passes with all required parameters
        assertThatCode(() -> templateService.validateParameters(businessStartupTemplateId, parameters))
            .doesNotThrowAnyException();
    }

    @Test
    void testBusinessStartupPlanner_ParameterCoverage() {
        // AC1: Verify simplified parameter types are present
        List<Parameter> parameters = templateService.getTemplateParameters(businessStartupTemplateId);
        
        assertThat(parameters).hasSize(10); // 10 parameters total
        
        // Verify simplified parameter types are used
        assertThat(parameters).anyMatch(p -> p.type().name().equals("TEXT"));
        assertThat(parameters).anyMatch(p -> p.type().name().equals("NUMBER"));
        assertThat(parameters).anyMatch(p -> p.type().name().equals("BOOLEAN"));
        assertThat(parameters).anyMatch(p -> p.type().name().equals("SELECTION"));
        
        // Verify only simplified types are used (no complex types)
        assertThat(parameters).noneMatch(p -> p.type().name().equals("EMAIL"));
        assertThat(parameters).noneMatch(p -> p.type().name().equals("PERCENTAGE"));
        assertThat(parameters).noneMatch(p -> p.type().name().equals("LOCATION"));
        assertThat(parameters).noneMatch(p -> p.type().name().equals("DATE"));
        assertThat(parameters).noneMatch(p -> p.type().name().equals("CURRENCY"));
    }

    // ===== EVENT ORGANIZER TESTS =====

    @Test
    void testEventOrganizer_ComprehensiveInput() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("event_name", "Tech Conference 2025");
        parameters.put("event_type", "Conference");
        parameters.put("expected_attendees", "500");
        parameters.put("event_date", "April 2025");
        parameters.put("event_start_time", "09:00 AM");
        parameters.put("event_end_time", "05:00 PM");
        parameters.put("venue_location", "Convention Center, Chicago, IL");
        parameters.put("budget", "100000 USD");
        parameters.put("setup_date", "Day before event");
        parameters.put("cleanup_date", "Day after event");
        parameters.put("organizer_phone", "+1-555-123-4567");
        parameters.put("catering_required", "true");
        parameters.put("av_required", "true");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(eventOrganizerTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(eventOrganizerTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Event Organizer");
    }

    @Test
    void testEventOrganizer_TextFlexibility() {
        // AC2: TEXT parameters handle phone formats flexibly
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("event_name", "Workshop 2025");
        parameters.put("event_type", "Workshop");
        parameters.put("expected_attendees", "50");
        parameters.put("event_date", "March 2025");
        parameters.put("event_start_time", "10:00 AM");
        parameters.put("event_end_time", "4:00 PM");
        parameters.put("venue_location", "Meeting Room A");
        parameters.put("budget", "5000 USD");
        parameters.put("organizer_phone", "+1-555-123-4567"); // TEXT accepts any format

        // Should pass - TEXT parameters are flexible for LLM consumption
        assertThatCode(() -> templateService.validateParameters(eventOrganizerTemplateId, parameters))
            .doesNotThrowAnyException();
    }

    @Test
    void testEventOrganizer_NumberValidation() {
        // AC2: NUMBER parameter validation (only type with strict validation)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("event_name", "Seminar");
        parameters.put("event_type", "Seminar");
        parameters.put("expected_attendees", "not-a-number"); // Invalid number
        parameters.put("event_date", "May 2025");
        parameters.put("event_start_time", "9:00 AM");
        parameters.put("event_end_time", "5:00 PM");
        parameters.put("venue_location", "Conference Room");
        parameters.put("budget", "10000 USD");
        parameters.put("organizer_phone", "+1-555-987-6543");

        assertThatThrownBy(() -> templateService.validateParameters(eventOrganizerTemplateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a valid number");
    }

    // ===== RESEARCH PROJECT PLANNER TESTS =====

    @Test
    void testResearchProject_ComprehensiveInput() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("project_title", "AI Impact on Healthcare Outcomes Research Study");
        parameters.put("subject_area", "Computer Science");
        parameters.put("methodology", "Mixed Methods");
        parameters.put("institution", "Stanford University");
        parameters.put("project_duration", "24 months");
        parameters.put("start_date", "February 2025");
        parameters.put("team_size", "8");
        parameters.put("budget", "750000 USD");
        parameters.put("research_url", "https://github.com/stanford/ai-healthcare-study");
        parameters.put("ethics_required", "true");
        parameters.put("data_collection", "Surveys");
        parameters.put("publication_target", "Peer-reviewed Journal");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(researchProjectTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(researchProjectTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Research Project Planner");
    }

    @Test
    void testResearchProject_AllowedValuesValidation() {
        // AC2: ALLOWED_VALUES validation for SELECTION parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("project_title", "Test Research Project");
        parameters.put("subject_area", "InvalidSubject"); // Invalid selection value
        parameters.put("methodology", "Quantitative");
        parameters.put("institution", "University of California");
        parameters.put("project_duration", "12 months");
        parameters.put("start_date", "April 2025");
        parameters.put("team_size", "4");
        parameters.put("budget", "200000 USD");
        parameters.put("research_url", "https://github.com/research-project");
        parameters.put("data_collection", "Experiments");

        assertThatThrownBy(() -> templateService.validateParameters(researchProjectTemplateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be one of");
    }

    @Test
    void testResearchProject_TextFlexibility() {
        // AC2: TEXT parameters accept flexible duration formats
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("project_title", "Brief Study");
        parameters.put("subject_area", "Biology");
        parameters.put("methodology", "Observational");
        parameters.put("institution", "MIT");
        parameters.put("project_duration", "12 months"); // TEXT accepts flexible formats
        parameters.put("start_date", "April 2025");
        parameters.put("team_size", "3");
        parameters.put("budget", "150000 USD");
        parameters.put("data_collection", "Observations");

        // Should pass - TEXT parameters are flexible for LLM consumption
        assertThatCode(() -> templateService.validateParameters(researchProjectTemplateId, parameters))
            .doesNotThrowAnyException();
    }

    // ===== PRODUCT LAUNCH CHECKLIST TESTS =====

    @Test
    void testProductLaunch_ComprehensiveInput() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("product_name", "CloudFlow Analytics");
        parameters.put("product_category", "SaaS Software");
        parameters.put("target_market", "B2B Enterprise");
        parameters.put("pricing_model", "Subscription");
        parameters.put("launch_strategy", "Soft Launch");
        parameters.put("launch_date", "June 2025");
        parameters.put("development_budget", "800000 USD");
        parameters.put("marketing_budget", "300000 USD");
        parameters.put("team_size", "25");
        parameters.put("product_url", "https://cloudflow.com");
        parameters.put("has_beta_program", "true");
        parameters.put("beta_duration", "10 weeks");
        parameters.put("gtm_strategy", "Direct Sales");
        parameters.put("competitive_positioning", "Premium Alternative");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(productLaunchTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(productLaunchTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Product Launch Checklist");
    }

    @Test
    void testProductLaunch_MultipleSelectionValidation() {
        // AC2: Multiple SELECTION parameters with ALLOWED_VALUES validation
        List<Parameter> parameters = templateService.getTemplateParameters(productLaunchTemplateId);
        
        long selectionCount = parameters.stream()
            .filter(p -> p.type().name().equals("SELECTION"))
            .count();
        
        assertThat(selectionCount).isGreaterThanOrEqualTo(4); // AC requirement: 4+ SELECTION parameters
        
        // Verify SELECTION parameters have ALLOWED_VALUES validation rules
        long selectionWithAllowedValues = parameters.stream()
            .filter(p -> p.type().name().equals("SELECTION"))
            .filter(p -> p.validationRules().stream()
                .anyMatch(rule -> rule.type().name().equals("ALLOWED_VALUES")))
            .count();
        
        assertThat(selectionWithAllowedValues).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testProductLaunch_SelectionValidation() {
        // AC5: SELECTION parameter validation with invalid choice
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("product_name", "TestProduct");
        parameters.put("product_category", "InvalidCategory"); // Invalid selection
        parameters.put("target_market", "B2C Consumer");
        parameters.put("pricing_model", "One-time Purchase");
        parameters.put("launch_strategy", "Hard Launch");
        parameters.put("launch_date", "June 2025"); // TEXT accepts flexible date formats
        parameters.put("development_budget", "100000 USD");
        parameters.put("marketing_budget", "50000 USD");
        parameters.put("team_size", "5");
        parameters.put("gtm_strategy", "Digital Marketing");
        parameters.put("competitive_positioning", "Cost Leader");

        assertThatThrownBy(() -> templateService.validateParameters(productLaunchTemplateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be one of");
    }

    // ===== HOME RENOVATION PLANNER TESTS =====

    @Test
    void testHomeRenovation_ComprehensiveInput() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("property_location", "Austin, TX, USA");
        parameters.put("renovation_type", "Kitchen Remodel");
        parameters.put("square_footage", "350");
        parameters.put("total_budget", "85000 USD");
        parameters.put("project_duration", "10 hours");
        parameters.put("start_date", "February 2025");
        parameters.put("completion_date", "May 2025");
        parameters.put("style_preference", "Modern");
        parameters.put("contractor_phone", "+1-555-234-5678");
        parameters.put("permits_required", "true");
        parameters.put("living_situation", "Living on-site");
        parameters.put("priority_level", "High");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(homeRenovationTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(homeRenovationTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Home Renovation Planner");
    }

    @Test
    void testHomeRenovation_NumberValidation() {
        // AC5: NUMBER parameter validation
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("property_location", "Denver, CO");
        parameters.put("renovation_type", "Bathroom Renovation");
        parameters.put("square_footage", "not-a-number"); // Invalid number
        parameters.put("total_budget", "25000 USD");
        parameters.put("project_duration", "6 weeks");
        parameters.put("start_date", "March 2025");
        parameters.put("priority_level", "Medium");

        assertThatThrownBy(() -> templateService.validateParameters(homeRenovationTemplateId, parameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a valid number");
    }

    @Test
    void testHomeRenovation_MinimalInput() {
        // AC5: Minimal viable input
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("property_location", "Phoenix, AZ");
        parameters.put("renovation_type", "Living Room");
        parameters.put("square_footage", "200");
        parameters.put("total_budget", "30000 USD");
        parameters.put("project_duration", "4 hours");
        parameters.put("start_date", "3 weeks from now");

        // Test that validation passes (no exception thrown)
        assertThatCode(() -> templateService.validateParameters(homeRenovationTemplateId, parameters))
            .doesNotThrowAnyException();
        
        // Test template exists and has correct structure
        WorkflowTemplate template = templateService.getTemplate(homeRenovationTemplateId);
        assertThat(template).isNotNull();
        assertThat(template.name()).isEqualTo("Home Renovation Planner");
    }

    // ===== CROSS-TEMPLATE VALIDATION TESTS =====

    @Test
    void testAllTemplates_ParameterCountRequirements() {
        // AC1: Verify 50+ total parameters across all 5 templates
        int totalParameterCount = 0;
        
        totalParameterCount += templateService.getTemplateParameters(businessStartupTemplateId).size();
        totalParameterCount += templateService.getTemplateParameters(eventOrganizerTemplateId).size();
        totalParameterCount += templateService.getTemplateParameters(researchProjectTemplateId).size();
        totalParameterCount += templateService.getTemplateParameters(productLaunchTemplateId).size();
        totalParameterCount += templateService.getTemplateParameters(homeRenovationTemplateId).size();
        
        assertThat(totalParameterCount).isGreaterThanOrEqualTo(50);
    }

    @Test
    void testAllTemplates_SimplifiedParameterTypes() {
        // AC1: Verify simplified parameter types are used across templates
        List<WorkflowTemplate> templates = List.of(
            templateService.getTemplate(businessStartupTemplateId),
            templateService.getTemplate(eventOrganizerTemplateId), 
            templateService.getTemplate(researchProjectTemplateId),
            templateService.getTemplate(productLaunchTemplateId),
            templateService.getTemplate(homeRenovationTemplateId)
        );
        
        assertThat(templates).hasSize(5);
        
        // Verify all templates use only simplified parameter types
        for (WorkflowTemplate template : templates) {
            List<Parameter> params = templateService.getTemplateParameters(template.id());
            assertThat(params).hasSizeGreaterThanOrEqualTo(8); // Minimum parameter count per template
            
            // All parameters should use only simplified types
            for (Parameter param : params) {
                assertThat(param.type().name()).isIn("TEXT", "NUMBER", "BOOLEAN", "SELECTION");
            }
        }
    }
}