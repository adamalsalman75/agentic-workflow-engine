-- V1: Complete schema for agentic workflow engine
-- Creates all core tables: goals, tasks, task_dependencies, templates, parameters

-- Core workflow tables
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query TEXT NOT NULL,
    summary TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    result TEXT,
    status VARCHAR(20) NOT NULL,
    blocking_dependencies UUID[] DEFAULT '{}',
    informational_dependencies UUID[] DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    depends_on_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    dependency_type VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(task_id, depends_on_task_id)
);

-- Template system tables
CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    prompt_template TEXT NOT NULL,
    author VARCHAR(255),
    is_public BOOLEAN DEFAULT true,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create trigger to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_templates_updated_at BEFORE UPDATE ON templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Template parameters table
CREATE TABLE template_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- TEXT, NUMBER, DATE, BOOLEAN, SELECTION, LOCATION, CURRENCY, etc.
    required BOOLEAN NOT NULL DEFAULT false,
    default_value TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(template_id, name)
);

-- Parameter metadata table for UI hints (using TEXT instead of JSONB)
CREATE TABLE parameter_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_id UUID NOT NULL REFERENCES template_parameters(id) ON DELETE CASCADE,
    placeholder TEXT,
    help_text TEXT,
    display_group VARCHAR(100),
    ui_component VARCHAR(50), -- For future UI customization
    additional_properties TEXT, -- JSON as TEXT for Spring Data JDBC compatibility
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(parameter_id)
);

-- Parameter validation rules table (using TEXT instead of JSONB)
CREATE TABLE parameter_validation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parameter_id UUID NOT NULL REFERENCES template_parameters(id) ON DELETE CASCADE,
    validation_type VARCHAR(50) NOT NULL, -- PATTERN, RANGE, DATE_RANGE, ALLOWED_VALUES, REQUIRED
    rule_value TEXT NOT NULL, -- JSON as TEXT for Spring Data JDBC compatibility
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Performance indexes
CREATE INDEX idx_tasks_goal_id ON tasks(goal_id);
CREATE INDEX idx_goals_status ON goals(status);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_goals_created_at ON goals(created_at);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);
CREATE INDEX idx_tasks_blocking_dependencies ON tasks USING GIN(blocking_dependencies);
CREATE INDEX idx_tasks_informational_dependencies ON tasks USING GIN(informational_dependencies);
CREATE INDEX idx_task_dependencies_task_id ON task_dependencies(task_id);
CREATE INDEX idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);
CREATE INDEX idx_task_dependencies_type ON task_dependencies(dependency_type);

-- Template parameter indexes
CREATE INDEX idx_template_parameters_template_id ON template_parameters(template_id);
CREATE INDEX idx_template_parameters_name ON template_parameters(name);
CREATE INDEX idx_template_parameters_type ON template_parameters(type);
CREATE INDEX idx_template_parameters_display_order ON template_parameters(display_order);
CREATE INDEX idx_parameter_metadata_parameter_id ON parameter_metadata(parameter_id);
CREATE INDEX idx_parameter_metadata_group ON parameter_metadata(display_group);
CREATE INDEX idx_validation_rules_parameter_id ON parameter_validation_rules(parameter_id);
CREATE INDEX idx_validation_rules_type ON parameter_validation_rules(validation_type);

-- Insert the Trip Planner template with parameters
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Simple Trip Planner',
    'Plan a comprehensive trip with dates, budget, and style preferences',
    'Travel',
    'Plan a {{duration}}-day trip to {{destination}} starting on {{startDate}} with a budget of {{budget}}.
Travel style preference: {{travelStyle}}.

Include:
1. Flight recommendations from major US cities
2. Hotel suggestions matching the {{travelStyle}} preference
3. Daily activity itinerary with time estimates
4. Local transportation options and costs
5. Must-see attractions with ticket prices
6. Restaurant recommendations for each day
7. Weather considerations for {{startDate}}
8. Budget breakdown in {{budget}} currency',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Get the template ID and insert parameters
DO $$
DECLARE
    template_uuid UUID;
    dest_param_id UUID;
    start_param_id UUID;
    duration_param_id UUID;
    budget_param_id UUID;
    style_param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO template_uuid FROM templates WHERE name = 'Simple Trip Planner';
    
    IF template_uuid IS NOT NULL THEN
        -- Insert destination parameter
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), template_uuid, 'destination', 'Where are you traveling to?', 'LOCATION', true, null, 0, NOW())
        RETURNING id INTO dest_param_id;
        
        -- Insert destination metadata
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (dest_param_id, 'Paris, France', 'Enter a city, state, or country', 'Location', NOW());
        
        -- Insert destination validation rule
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (dest_param_id, 'PATTERN', '{"pattern": "^[A-Za-z\\s,.-]+$"}', 'Please enter a valid location (letters, spaces, commas, periods, and hyphens only)', NOW());
        
        -- Insert startDate parameter
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), template_uuid, 'startDate', 'Departure date', 'DATE', true, null, 1, NOW())
        RETURNING id INTO start_param_id;
        
        -- Insert startDate metadata
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (start_param_id, '2025-12-25', 'Format: YYYY-MM-DD', 'Dates', NOW());
        
        -- Insert startDate validation rule (no past dates)
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (start_param_id, 'DATE_RANGE', format('{"min": "%s"}', CURRENT_DATE::text), 'Departure date cannot be in the past', NOW());
        
        -- Insert duration parameter
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), template_uuid, 'duration', 'Number of days', 'NUMBER', true, null, 2, NOW())
        RETURNING id INTO duration_param_id;
        
        -- Insert duration metadata
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (duration_param_id, '5', 'Enter a numeric value', 'Dates', NOW());
        
        -- Insert duration validation rule
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (duration_param_id, 'RANGE', '{"min": 1, "max": 365}', 'Duration must be between 1 and 365 days', NOW());
        
        -- Insert budget parameter (optional)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), template_uuid, 'budget', 'Total budget with currency', 'CURRENCY', false, '1000 USD', 3, NOW())
        RETURNING id INTO budget_param_id;
        
        -- Insert budget metadata
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (budget_param_id, '1000 USD', 'Amount with currency code (e.g., 1000 USD)', 'Budget', NOW());
        
        -- Insert budget validation rule
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (budget_param_id, 'PATTERN', '{"pattern": "^\\d+\\s*(USD|EUR|GBP|JPY|CAD|AUD)$"}', 'Budget must be in format: amount + currency code (e.g., 1000 USD)', NOW());
        
        -- Insert travelStyle parameter (optional)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), template_uuid, 'travelStyle', 'Travel style preference', 'SELECTION', false, 'Mid-range', 4, NOW())
        RETURNING id INTO style_param_id;
        
        -- Insert travelStyle metadata
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (style_param_id, 'Select an option', 'Choose from available options', 'Preferences', NOW());
        
        -- Insert travelStyle validation rule
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (style_param_id, 'ALLOWED_VALUES', '{"values": ["Budget", "Mid-range", "Luxury"]}', 'Travel style must be Budget, Mid-range, or Luxury', NOW());
        
    END IF;
END $$;