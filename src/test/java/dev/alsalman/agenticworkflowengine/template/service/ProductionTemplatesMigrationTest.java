package dev.alsalman.agenticworkflowengine.template.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate the production templates migration meets Story 5 acceptance criteria
 * without requiring a full database integration test
 */
class ProductionTemplatesMigrationTest {

    private static final String MIGRATION_FILE = "db/migration/V2__production_templates.sql";

    @Test
    void testMigrationFile_ContainsAllRequiredTemplates() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify all 5 templates are created
        assertThat(migrationContent).contains("Business Startup Planner");
        assertThat(migrationContent).contains("Event Organizer");
        assertThat(migrationContent).contains("Research Project Planner");
        assertThat(migrationContent).contains("Product Launch Checklist");
        assertThat(migrationContent).contains("Home Renovation Planner");
    }

    @Test
    void testMigrationFile_ParameterTypesCoverage() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC1: Verify all required parameter types are used
        assertThat(migrationContent).contains("'TEXT'");
        assertThat(migrationContent).contains("'NUMBER'");
        assertThat(migrationContent).contains("'SELECTION'");
        assertThat(migrationContent).contains("'DATE'");
        assertThat(migrationContent).contains("'CURRENCY'");
        assertThat(migrationContent).contains("'LOCATION'");
        assertThat(migrationContent).contains("'EMAIL'");
        assertThat(migrationContent).contains("'PHONE'");
        assertThat(migrationContent).contains("'URL'");
        assertThat(migrationContent).contains("'BOOLEAN'");
        assertThat(migrationContent).contains("'PERCENTAGE'");
        assertThat(migrationContent).contains("'TIME'");
        assertThat(migrationContent).contains("'DURATION'");
    }

    @Test
    void testMigrationFile_ValidationRulesCoverage() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify validation rule types are implemented
        assertThat(migrationContent).contains("'PATTERN'");
        assertThat(migrationContent).contains("'RANGE'");
        assertThat(migrationContent).contains("'DATE_RANGE'");
        assertThat(migrationContent).contains("'ALLOWED_VALUES'");
        
        // Count validation rules (should be 70+)
        Pattern validationRulePattern = Pattern.compile("INSERT INTO parameter_validation_rules");
        Matcher matcher = validationRulePattern.matcher(migrationContent);
        int validationRuleCount = 0;
        while (matcher.find()) {
            validationRuleCount++;
        }
        
        // Updated after removing redundant/conflicting validation rules
        assertThat(validationRuleCount).isGreaterThanOrEqualTo(60);
    }

    @Test
    void testMigrationFile_ParameterCount() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC1: Verify 50+ total parameters across all templates
        Pattern parameterPattern = Pattern.compile("INSERT INTO template_parameters");
        Matcher matcher = parameterPattern.matcher(migrationContent);
        int parameterCount = 0;
        while (matcher.find()) {
            parameterCount++;
        }
        
        assertThat(parameterCount).isGreaterThanOrEqualTo(50);
    }

    @Test
    void testMigrationFile_EmailValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify EMAIL parameter has proper validation
        assertThat(migrationContent).contains("business_email");
        assertThat(migrationContent).contains("@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}");
    }

    @Test
    void testMigrationFile_PhoneValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify PHONE parameter has proper validation
        assertThat(migrationContent).contains("organizer_phone");
        assertThat(migrationContent).contains("contractor_phone");
        assertThat(migrationContent).contains("\\\\+?1?[- ]?\\\\(?([0-9]{3})\\\\)?[- ]?([0-9]{3})[- ]?([0-9]{4})");
    }

    @Test
    void testMigrationFile_URLValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify URL parameter has proper validation
        assertThat(migrationContent).contains("research_url");
        assertThat(migrationContent).contains("product_url");
        assertThat(migrationContent).contains("https?:\\\\/\\\\/");
    }

    @Test
    void testMigrationFile_PercentageValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify PERCENTAGE parameter has proper range validation
        assertThat(migrationContent).contains("equity_split");
        assertThat(migrationContent).contains("\"min\": 0, \"max\": 100");
    }

    @Test
    void testMigrationFile_DateRangeValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify DATE parameters have proper range validation
        assertThat(migrationContent).contains("DATE_RANGE");
        assertThat(migrationContent).contains("Launch date must be between today and 3 years from now");
        assertThat(migrationContent).contains("must be at least 1 month from now");
    }

    @Test
    void testMigrationFile_AllowedValuesValidation() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC2: Verify SELECTION parameters have ALLOWED_VALUES
        Pattern allowedValuesPattern = Pattern.compile("'ALLOWED_VALUES'.*\"values\":");
        Matcher matcher = allowedValuesPattern.matcher(migrationContent);
        int allowedValuesCount = 0;
        while (matcher.find()) {
            allowedValuesCount++;
        }
        
        // Product Launch template should have 4+ ALLOWED_VALUES validations
        assertThat(allowedValuesCount).isGreaterThanOrEqualTo(15); // Total across all templates
    }

    @Test
    void testMigrationFile_PromptTemplatesQuality() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC3: Verify prompt templates contain parameter references
        assertThat(migrationContent).contains("{{business_name}}");
        assertThat(migrationContent).contains("{{event_name}}");
        assertThat(migrationContent).contains("{{project_title}}");
        assertThat(migrationContent).contains("{{product_name}}");
        assertThat(migrationContent).contains("{{renovation_type}}");
        
        // Verify prompts mention task generation
        assertThat(migrationContent).contains("18-22 actionable tasks");
        assertThat(migrationContent).contains("20-25 actionable tasks");
        assertThat(migrationContent).contains("22-25 actionable tasks");
        assertThat(migrationContent).contains("16-20 actionable tasks");
    }

    @Test
    void testMigrationFile_BusinessStartupTemplate() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify Business Startup Planner has required parameters
        assertThat(migrationContent).contains("business_name");
        assertThat(migrationContent).contains("business_email");
        assertThat(migrationContent).contains("has_funding");
        assertThat(migrationContent).contains("equity_split");
        
        // Should demonstrate EMAIL, BOOLEAN, PERCENTAGE types
        assertThat(migrationContent).contains("'EMAIL'");
        assertThat(migrationContent).contains("'BOOLEAN'");
        assertThat(migrationContent).contains("'PERCENTAGE'");
    }

    @Test
    void testMigrationFile_EventOrganizerTemplate() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify Event Organizer has TIME and PHONE parameters
        assertThat(migrationContent).contains("event_start_time");
        assertThat(migrationContent).contains("event_end_time");
        assertThat(migrationContent).contains("organizer_phone");
        assertThat(migrationContent).contains("'TIME'");
        assertThat(migrationContent).contains("'PHONE'");
    }

    @Test
    void testMigrationFile_ResearchProjectTemplate() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify Research Project has URL and DURATION parameters
        assertThat(migrationContent).contains("research_url");
        assertThat(migrationContent).contains("project_duration");
        assertThat(migrationContent).contains("'URL'");
        assertThat(migrationContent).contains("'DURATION'");
    }

    @Test
    void testMigrationFile_ProductLaunchTemplate() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify Product Launch has multiple SELECTION parameters
        assertThat(migrationContent).contains("product_category");
        assertThat(migrationContent).contains("target_market");
        assertThat(migrationContent).contains("pricing_model");
        assertThat(migrationContent).contains("launch_strategy");
        assertThat(migrationContent).contains("gtm_strategy");
        assertThat(migrationContent).contains("competitive_positioning");
    }

    @Test
    void testMigrationFile_HomeRenovationTemplate() throws IOException {
        String migrationContent = readMigrationFile();
        
        // AC: Verify Home Renovation demonstrates contractor coordination
        assertThat(migrationContent).contains("contractor_phone");
        assertThat(migrationContent).contains("permits_required");
        assertThat(migrationContent).contains("living_situation");
        assertThat(migrationContent).contains("contractor coordination");
    }

    private String readMigrationFile() throws IOException {
        ClassPathResource resource = new ClassPathResource(MIGRATION_FILE);
        return Files.readString(resource.getFile().toPath());
    }
}