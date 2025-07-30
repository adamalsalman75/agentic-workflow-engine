-- V2: Production Templates - 5 comprehensive templates for Story 5
-- Creates Business Startup Planner, Event Organizer, Research Project Planner, Product Launch Checklist, Home Renovation Planner
-- Demonstrates all parameter types and advanced validation capabilities

-- 1. BUSINESS STARTUP PLANNER TEMPLATE
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Business Startup Planner',
    'Comprehensive business startup planning with legal, financial, and operational guidance',
    'Business',
    'Create a comprehensive startup plan for {{business_name}}, a {{industry}} business located in {{business_location}}.

Business Details:
- Business Name: {{business_name}}
- Industry: {{industry}}
- Location: {{business_location}}
- Team Size: {{team_size}} members
- Launch Date: {{launch_date}}
- Initial Funding: {{funding_amount}}
- Contact Email: {{business_email}}
- Funding Status: {{#if has_funding}}Secured{{else}}Seeking funding{{/if}}
- Equity Distribution: {{equity_split}}% founder equity
- Business Model: {{business_model}}

Generate a detailed startup plan with 18-22 actionable tasks covering:
1. Legal structure and business registration
2. Financial planning and funding strategy
3. Product/service development timeline
4. Marketing and customer acquisition strategy
5. Team building and hiring plan
6. Operational setup and systems
7. Compliance and regulatory requirements
8. Launch strategy and go-to-market plan

Each task should include realistic time estimates, required resources, and key milestones.',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Business Startup Planner Parameters
DO $$
DECLARE
    startup_template_uuid UUID;
    param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO startup_template_uuid FROM templates WHERE name = 'Business Startup Planner';
    
    IF startup_template_uuid IS NOT NULL THEN
        -- business_name (TEXT)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'business_name', 'What is your business name?', 'TEXT', true, null, 0, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'TechFlow Solutions', 'Enter your business or company name', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s&\\''.-]{2,100}$"}', 'Business name must be 2-100 characters, letters, numbers, spaces, and common business symbols only', NOW());
        
        -- industry (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'industry', 'What industry is your business in?', 'SELECTION', true, 'Technology', 1, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Technology', 'Select your primary industry sector', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Technology", "Healthcare", "Finance", "Retail", "Manufacturing", "Education", "Real Estate", "Food & Beverage", "Consulting", "Creative Services", "Other"]}', 'Please select a valid industry from the list', NOW());
        
        -- business_location (LOCATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'business_location', 'Where will your business be located?', 'LOCATION', true, null, 2, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'San Francisco, CA, USA', 'Enter city, state/province, country', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z\\s,.-]+$"}', 'Please enter a valid location (letters, spaces, commas, periods, and hyphens only)', NOW());
        
        -- team_size (NUMBER)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'team_size', 'How many team members (including founders)?', 'NUMBER', true, '3', 3, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '3', 'Include all founders and initial team members', 'Team', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 1, "max": 50}', 'Team size must be between 1 and 50 members', NOW());
        
        -- launch_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'launch_date', 'When do you plan to launch?', 'DATE', true, null, 4, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-06-01', 'Format: YYYY-MM-DD', 'Timeline', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', CURRENT_DATE::date::text, (CURRENT_DATE + INTERVAL '3 years')::date::text), 'Launch date must be between today and 3 years from now', NOW());
        
        -- funding_amount (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'funding_amount', 'Initial funding amount with currency', 'CURRENCY', false, '100000 USD', 5, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '100000 USD', 'Enter amount and currency (e.g., 50000 USD)', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter amount with currency (e.g., 50000 USD)', NOW());
        
        -- business_email (EMAIL)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'business_email', 'Business contact email', 'EMAIL', true, null, 6, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'info@techflow.com', 'Primary business email address', 'Contact', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"}', 'Please enter a valid email address', NOW());
        
        -- has_funding (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'has_funding', 'Do you already have funding secured?', 'BOOLEAN', false, 'false', 7, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'false', 'Select true if funding is already secured', 'Financial', NOW());
        
        -- equity_split (PERCENTAGE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'equity_split', 'Founder equity percentage', 'PERCENTAGE', false, '70', 8, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '70', 'Enter percentage (0-100)', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 0, "max": 100}', 'Equity percentage must be between 0 and 100', NOW());
        
        -- business_model (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), startup_template_uuid, 'business_model', 'What is your business model?', 'SELECTION', true, 'SaaS', 9, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'SaaS', 'Select your primary revenue model', 'Business Model', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["SaaS", "E-commerce", "Marketplace", "Subscription", "Freemium", "B2B Services", "B2C Products", "Consulting", "Advertising", "Commission-based"]}', 'Please select a valid business model', NOW());
        
        -- Additional validation rules for comprehensive coverage
        -- Business name length validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 2, "max": 100}', 'Business name must be between 2 and 100 characters', NOW()
        FROM template_parameters WHERE template_id = startup_template_uuid AND name = 'business_name';
        
        -- Launch date reasonable future validation  
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'DATE_RANGE', format('{"max": "%s"}', (CURRENT_DATE + INTERVAL '5 years')::date::text), 'Launch date cannot be more than 5 years in the future', NOW()
        FROM template_parameters WHERE template_id = startup_template_uuid AND name = 'launch_date';
        
    END IF;
END $$;

-- 2. EVENT ORGANIZER TEMPLATE
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Event Organizer',
    'Comprehensive event planning with logistics, timeline, and vendor coordination',
    'Events',
    'Create a comprehensive event plan for {{event_name}}, a {{event_type}} event.

Event Details:
- Event Name: {{event_name}}
- Event Type: {{event_type}}
- Expected Attendees: {{expected_attendees}}
- Event Date: {{event_date}}
- Start Time: {{event_start_time}}
- End Time: {{event_end_time}}
- Venue: {{venue_location}}
- Budget: {{budget}}
- Setup Date: {{setup_date}}
- Cleanup Date: {{cleanup_date}}
- Organizer Contact: {{organizer_phone}}
- Catering Required: {{#if catering_required}}Yes{{else}}No{{/if}}
- Audio/Visual: {{#if av_required}}Yes{{else}}No{{/if}}

Generate a detailed event planning checklist with 20-25 actionable tasks covering:
1. Venue booking and logistics coordination
2. Vendor management (catering, AV, security, etc.)
3. Marketing and promotion timeline
4. Registration and attendee management
5. Day-of-event execution plan
6. Setup and breakdown coordination
7. Budget tracking and expense management
8. Risk management and contingency planning

Include specific deadlines, responsible parties, and coordination requirements.',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Event Organizer Parameters
DO $$
DECLARE
    event_template_uuid UUID;
    param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO event_template_uuid FROM templates WHERE name = 'Event Organizer';
    
    IF event_template_uuid IS NOT NULL THEN
        -- event_name (TEXT)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'event_name', 'What is the name of your event?', 'TEXT', true, null, 0, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Tech Conference 2025', 'Enter your event name', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s&\\''.-]{3,100}$"}', 'Event name must be 3-100 characters', NOW());
        
        -- event_type (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'event_type', 'What type of event are you organizing?', 'SELECTION', true, 'Conference', 1, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Conference', 'Select the type of event', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Conference", "Workshop", "Seminar", "Trade Show", "Corporate Meeting", "Wedding", "Birthday Party", "Networking Event", "Product Launch", "Fundraiser", "Concert", "Festival"]}', 'Please select a valid event type', NOW());
        
        -- expected_attendees (NUMBER)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'expected_attendees', 'How many attendees do you expect?', 'NUMBER', true, null, 2, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '200', 'Estimated number of attendees', 'Capacity', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 5, "max": 10000}', 'Expected attendees must be between 5 and 10,000', NOW());
        
        -- event_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'event_date', 'When is your event?', 'DATE', true, null, 3, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-09-15', 'Format: YYYY-MM-DD', 'Schedule', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', (CURRENT_DATE + INTERVAL '1 week')::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Event date must be at least 1 week from now and within 2 years', NOW());
        
        -- event_start_time (TIME)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'event_start_time', 'What time does the event start?', 'TIME', true, '09:00', 4, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '09:00', 'Format: HH:MM (24-hour)', 'Schedule', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^([01]?[0-9]|2[0-3]):[0-5][0-9]$"}', 'Please enter a valid time in HH:MM format (24-hour)', NOW());
        
        -- event_end_time (TIME)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'event_end_time', 'What time does the event end?', 'TIME', true, '17:00', 5, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '17:00', 'Format: HH:MM (24-hour)', 'Schedule', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^([01]?[0-9]|2[0-3]):[0-5][0-9]$"}', 'Please enter a valid time in HH:MM format (24-hour)', NOW());
        
        -- venue_location (LOCATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'venue_location', 'Where is the event venue?', 'LOCATION', true, null, 6, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Convention Center, Chicago, IL', 'Enter venue name and location', 'Venue', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s,.-]+$"}', 'Please enter a valid venue location', NOW());
        
        -- budget (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'budget', 'What is your event budget?', 'CURRENCY', true, null, 7, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '50000 USD', 'Enter total budget with currency', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter budget with currency (e.g., 50000 USD)', NOW());
        
        -- setup_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'setup_date', 'When will you set up for the event?', 'DATE', false, null, 8, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-09-14', 'Usually 1-2 days before event', 'Schedule', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', CURRENT_DATE::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Setup date must be today or later', NOW());
        
        -- cleanup_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'cleanup_date', 'When will you clean up after the event?', 'DATE', false, null, 9, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-09-16', 'Usually 1-2 days after event', 'Schedule', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', CURRENT_DATE::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Cleanup date must be today or later', NOW());
        
        -- organizer_phone (PHONE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'organizer_phone', 'Event organizer contact phone', 'PHONE', true, null, 10, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '+1-555-123-4567', 'Primary contact phone number', 'Contact', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^\\+?1?[- ]?\\(?([0-9]{3})\\)?[- ]?([0-9]{3})[- ]?([0-9]{4})$"}', 'Please enter a valid phone number (e.g., +1-555-123-4567)', NOW());
        
        -- catering_required (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'catering_required', 'Will you need catering services?', 'BOOLEAN', false, 'true', 11, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'true', 'Select true if catering is needed', 'Services', NOW());
        
        -- av_required (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), event_template_uuid, 'av_required', 'Will you need audio/visual equipment?', 'BOOLEAN', false, 'true', 12, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'true', 'Select true if A/V equipment is needed', 'Services', NOW());
        
        -- Additional Event Organizer validation rules
        -- Event name length validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 3, "max": 100}', 'Event name must be between 3 and 100 characters', NOW()
        FROM template_parameters WHERE template_id = event_template_uuid AND name = 'event_name';
        
        
        -- Venue location format validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 5, "max": 200}', 'Venue location must be between 5 and 200 characters', NOW()
        FROM template_parameters WHERE template_id = event_template_uuid AND name = 'venue_location';
        
        -- Budget minimum validation  
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 100}', 'Event budget must be at least 100 in any currency', NOW()
        FROM template_parameters WHERE template_id = event_template_uuid AND name = 'budget';
        
    END IF;
END $$;

-- 3. RESEARCH PROJECT PLANNER TEMPLATE
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Research Project Planner',
    'Comprehensive academic and professional research project planning',
    'Research',
    'Create a comprehensive research project plan for "{{project_title}}" in the {{subject_area}} field.

Project Details:
- Project Title: {{project_title}}
- Subject Area: {{subject_area}}
- Methodology: {{methodology}}
- Institution: {{institution}}
- Project Duration: {{project_duration}}
- Start Date: {{start_date}}
- Team Size: {{team_size}}
- Budget: {{budget}}
- Research URL/Repository: {{research_url}}
- Ethics Approval Required: {{#if ethics_required}}Yes{{else}}No{{/if}}
- Data Collection Method: {{data_collection}}
- Publication Target: {{publication_target}}

Generate a detailed research project plan with 18-22 actionable tasks covering:
1. Literature review and background research
2. Research proposal development and approval process
3. Ethics committee submission (if required)
4. Methodology implementation and validation
5. Data collection timeline and procedures
6. Analysis framework and statistical methods
7. Documentation and reporting milestones
8. Publication and dissemination strategy
9. Budget management and resource allocation
10. Risk assessment and mitigation strategies

Include specific deadlines, deliverables, and quality checkpoints.',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Research Project Planner Parameters
DO $$
DECLARE
    research_template_uuid UUID;
    param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO research_template_uuid FROM templates WHERE name = 'Research Project Planner';
    
    IF research_template_uuid IS NOT NULL THEN
        -- project_title (TEXT)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'project_title', 'What is your research project title?', 'TEXT', true, null, 0, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'AI Impact on Healthcare Outcomes', 'Enter your research project title', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s:;,.-]{10,200}$"}', 'Project title must be 10-200 characters', NOW());
        
        -- subject_area (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'subject_area', 'What is your research subject area?', 'SELECTION', true, 'Computer Science', 1, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Computer Science', 'Select your primary research field', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Computer Science", "Medicine", "Psychology", "Engineering", "Physics", "Chemistry", "Biology", "Economics", "Sociology", "Education", "Environmental Science", "Business", "Mathematics", "Other"]}', 'Please select a valid subject area', NOW());
        
        -- methodology (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'methodology', 'What research methodology will you use?', 'SELECTION', true, 'Quantitative', 2, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Quantitative', 'Select your research approach', 'Methodology', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Quantitative", "Qualitative", "Mixed Methods", "Experimental", "Observational", "Case Study", "Survey", "Ethnographic", "Systematic Review", "Meta-Analysis"]}', 'Please select a valid research methodology', NOW());
        
        -- institution (TEXT)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'institution', 'What institution are you affiliated with?', 'TEXT', true, null, 3, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Stanford University', 'Enter your university or organization', 'Affiliation', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s&\\''.-]{2,100}$"}', 'Institution name must be 2-100 characters', NOW());
        
        -- project_duration (DURATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'project_duration', 'How long is your research project?', 'DURATION', true, '18 months', 4, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '18 months', 'Enter duration (e.g., 6 months, 2 years)', 'Timeline', NOW());
        
        
        -- start_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'start_date', 'When will your research project start?', 'DATE', true, null, 5, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-03-01', 'Format: YYYY-MM-DD', 'Timeline', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', CURRENT_DATE::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Start date must be today or later, within 2 years', NOW());
        
        -- team_size (NUMBER)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'team_size', 'How many researchers are on your team?', 'NUMBER', true, '4', 6, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '4', 'Include all researchers and assistants', 'Team', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 1, "max": 20}', 'Team size must be between 1 and 20 researchers', NOW());
        
        -- budget (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'budget', 'What is your research budget?', 'CURRENCY', true, null, 7, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '250000 USD', 'Enter total research budget', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter budget with currency (e.g., 250000 USD)', NOW());
        
        -- research_url (URL)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'research_url', 'Project repository or website URL', 'URL', false, null, 8, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'https://github.com/university/research-project', 'Optional project repository or website', 'Resources', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$"}', 'Please enter a valid URL starting with http:// or https://', NOW());
        
        -- ethics_required (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'ethics_required', 'Does your research require ethics approval?', 'BOOLEAN', false, 'true', 9, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'true', 'Select true if IRB/ethics approval needed', 'Compliance', NOW());
        
        -- data_collection (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'data_collection', 'What is your primary data collection method?', 'SELECTION', true, 'Surveys', 10, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Surveys', 'Select your data collection approach', 'Methodology', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Surveys", "Interviews", "Experiments", "Observations", "Archival Data", "Sensor Data", "Database Analysis", "Literature Review", "Focus Groups", "Case Studies"]}', 'Please select a valid data collection method', NOW());
        
        -- publication_target (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), research_template_uuid, 'publication_target', 'Where do you plan to publish your results?', 'SELECTION', false, 'Peer-reviewed Journal', 11, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Peer-reviewed Journal', 'Select your publication target', 'Dissemination', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Peer-reviewed Journal", "Conference Proceedings", "Thesis/Dissertation", "Technical Report", "Book Chapter", "White Paper", "Industry Publication", "Open Access Repository", "Not Applicable"]}', 'Please select a valid publication target', NOW());
        
        -- Additional Research Project validation rules
        -- Project title length validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 10, "max": 200}', 'Project title must be between 10 and 200 characters', NOW()
        FROM template_parameters WHERE template_id = research_template_uuid AND name = 'project_title';
        
        -- Institution name validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 2, "max": 100}', 'Institution name must be between 2 and 100 characters', NOW()
        FROM template_parameters WHERE template_id = research_template_uuid AND name = 'institution';
        
        
    END IF;
END $$;

-- 4. PRODUCT LAUNCH CHECKLIST TEMPLATE  
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Product Launch Checklist',
    'Comprehensive product launch planning with development, marketing, and operations coordination',
    'Product Management',
    'Create a comprehensive product launch plan for {{product_name}}, a {{product_category}} product.

Product Details:
- Product Name: {{product_name}}
- Product Category: {{product_category}}
- Target Market: {{target_market}}
- Pricing Model: {{pricing_model}}
- Launch Strategy: {{launch_strategy}}
- Launch Date: {{launch_date}}
- Development Budget: {{development_budget}}
- Marketing Budget: {{marketing_budget}}
- Team Size: {{team_size}}
- Product URL: {{product_url}}
- Beta Program: {{#if has_beta_program}}Yes ({{beta_duration}}){{else}}No{{/if}}
- Go-to-Market: {{gtm_strategy}}
- Competitive Analysis: {{competitive_positioning}}

Generate a detailed product launch checklist with 22-25 actionable tasks covering:
1. Product development completion and quality assurance
2. Market research and competitive analysis validation
3. Pricing strategy and business model finalization
4. Marketing campaign development and execution
5. Sales enablement and channel partner preparation
6. Technical infrastructure and scaling preparation
7. Customer support and documentation readiness
8. Legal compliance and regulatory requirements
9. Launch event planning and PR coordination
10. Post-launch monitoring and optimization strategy

Include specific deadlines, ownership assignments, and success metrics.',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Product Launch Checklist Parameters
DO $$
DECLARE
    product_template_uuid UUID;
    param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO product_template_uuid FROM templates WHERE name = 'Product Launch Checklist';
    
    IF product_template_uuid IS NOT NULL THEN
        -- product_name (TEXT)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'product_name', 'What is your product name?', 'TEXT', true, null, 0, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'CloudFlow Analytics', 'Enter your product name', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s&\\''.-]{2,80}$"}', 'Product name must be 2-80 characters', NOW());
        
        -- product_category (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'product_category', 'What category is your product in?', 'SELECTION', true, 'SaaS Software', 1, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'SaaS Software', 'Select your product category', 'Basic Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["SaaS Software", "Mobile App", "Physical Product", "Digital Service", "Hardware Device", "Platform/Marketplace", "Enterprise Software", "Consumer Electronics", "Media/Content", "E-commerce", "Gaming", "Other"]}', 'Please select a valid product category', NOW());
        
        -- target_market (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'target_market', 'Who is your target market?', 'SELECTION', true, 'B2B Enterprise', 2, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'B2B Enterprise', 'Select your primary target market', 'Market', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["B2B Enterprise", "B2B SMB", "B2C Consumer", "B2C Premium", "B2B2C", "Government", "Education", "Healthcare", "Developers", "Prosumers"]}', 'Please select a valid target market', NOW());
        
        -- pricing_model (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'pricing_model', 'What is your pricing model?', 'SELECTION', true, 'Subscription', 3, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Subscription', 'Select your pricing approach', 'Business Model', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Subscription", "One-time Purchase", "Freemium", "Usage-based", "Tiered Pricing", "Enterprise Licensing", "Pay-per-Use", "Commission-based", "Advertising-supported", "Hybrid Model"]}', 'Please select a valid pricing model', NOW());
        
        -- launch_strategy (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'launch_strategy', 'What is your launch strategy?', 'SELECTION', true, 'Soft Launch', 4, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Soft Launch', 'Select your launch approach', 'Strategy', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Soft Launch", "Hard Launch", "Beta Launch", "Phased Rollout", "Stealth Launch", "Public Beta", "Invite-only", "Geographic Rollout", "Vertical Launch", "Partner Launch"]}', 'Please select a valid launch strategy', NOW());
        
        -- launch_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'launch_date', 'When is your product launch date?', 'DATE', true, null, 5, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-08-15', 'Format: YYYY-MM-DD', 'Timeline', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', (CURRENT_DATE + INTERVAL '1 month')::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Launch date must be at least 1 month from now and within 2 years', NOW());
        
        -- development_budget (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'development_budget', 'What is your development budget?', 'CURRENCY', true, null, 6, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '500000 USD', 'Total development budget', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter budget with currency (e.g., 500000 USD)', NOW());
        
        -- marketing_budget (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'marketing_budget', 'What is your marketing budget?', 'CURRENCY', true, null, 7, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '200000 USD', 'Total marketing budget', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter budget with currency (e.g., 200000 USD)', NOW());
        
        -- team_size (NUMBER)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'team_size', 'How many people are on your product team?', 'NUMBER', true, '12', 8, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '12', 'Include all product, dev, and marketing team members', 'Team', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 2, "max": 100}', 'Team size must be between 2 and 100 members', NOW());
        
        -- product_url (URL)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'product_url', 'Product website or landing page URL', 'URL', false, null, 9, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'https://cloudflow.com', 'Product website or app store URL', 'Marketing', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$"}', 'Please enter a valid URL starting with http:// or https://', NOW());
        
        -- has_beta_program (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'has_beta_program', 'Will you run a beta program before launch?', 'BOOLEAN', false, 'true', 10, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'true', 'Select true if running beta program', 'Strategy', NOW());
        
        -- beta_duration (DURATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'beta_duration', 'How long will your beta program run?', 'DURATION', false, '8 weeks', 11, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '8 weeks', 'Enter beta program duration', 'Strategy', NOW());
        
        
        -- gtm_strategy (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'gtm_strategy', 'What is your go-to-market strategy?', 'SELECTION', true, 'Direct Sales', 12, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Direct Sales', 'Select your go-to-market approach', 'Strategy', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Direct Sales", "Channel Partners", "Self-Service", "Inside Sales", "Field Sales", "Digital Marketing", "Partner Ecosystem", "Marketplace", "Reseller Network", "Hybrid Approach"]}', 'Please select a valid go-to-market strategy', NOW());
        
        -- competitive_positioning (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), product_template_uuid, 'competitive_positioning', 'How do you position against competitors?', 'SELECTION', true, 'Premium Alternative', 13, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Premium Alternative', 'Select your competitive positioning', 'Strategy', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Premium Alternative", "Cost Leader", "Niche Specialist", "Feature Leader", "Disruptor", "Fast Follower", "Category Creator", "Enterprise Focus", "SMB Focus", "Developer-first"]}', 'Please select a valid competitive positioning', NOW());
        
        -- Additional Product Launch validation rules
        -- Product name length validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 2, "max": 80}', 'Product name must be between 2 and 80 characters', NOW()
        FROM template_parameters WHERE template_id = product_template_uuid AND name = 'product_name';
        
        
        -- Budget consistency validation for development
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 1000}', 'Development budget must be at least 1000 in any currency', NOW()
        FROM template_parameters WHERE template_id = product_template_uuid AND name = 'development_budget';
        
        -- Budget consistency validation for marketing
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 500}', 'Marketing budget must be at least 500 in any currency', NOW()
        FROM template_parameters WHERE template_id = product_template_uuid AND name = 'marketing_budget';
        
    END IF;
END $$;

-- 5. HOME RENOVATION PLANNER TEMPLATE
INSERT INTO templates (id, name, description, category, prompt_template, author, is_public, version, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Home Renovation Planner',
    'Comprehensive home renovation planning with contractors, permits, and timeline coordination',
    'Home & Garden',
    'Create a comprehensive home renovation plan for a {{renovation_type}} project.

Project Details:
- Property Location: {{property_location}}
- Renovation Type: {{renovation_type}}
- Square Footage: {{square_footage}} sq ft
- Total Budget: {{total_budget}}
- Project Duration: {{project_duration}}
- Start Date: {{start_date}}
- Target Completion: {{completion_date}}
- Style Preference: {{style_preference}}
- Contractor Phone: {{contractor_phone}}
- Permits Required: {{#if permits_required}}Yes{{else}}No{{/if}}
- Living Situation: {{living_situation}}
- Priority Level: {{priority_level}}

Generate a detailed home renovation plan with 16-20 actionable tasks covering:
1. Initial planning and design development
2. Permit applications and approvals process
3. Contractor vetting and hiring process
4. Material selection and procurement timeline
5. Construction sequencing and scheduling
6. Quality control and inspection checkpoints
7. Budget tracking and payment milestones
8. Utility and service coordination
9. Cleanup and final walkthrough procedures
10. Warranty documentation and maintenance planning

Include specific deadlines, contractor coordination requirements, and homeowner preparation tasks.',
    'System',
    true,
    1,
    NOW(),
    NOW()
);

-- Home Renovation Planner Parameters
DO $$
DECLARE
    renovation_template_uuid UUID;
    param_id UUID;
BEGIN
    -- Get template ID
    SELECT id INTO renovation_template_uuid FROM templates WHERE name = 'Home Renovation Planner';
    
    IF renovation_template_uuid IS NOT NULL THEN
        -- property_location (LOCATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'property_location', 'Where is the property being renovated?', 'LOCATION', true, null, 0, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Austin, TX, USA', 'Enter property address or city', 'Property Info', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[A-Za-z0-9\\s,.-]+$"}', 'Please enter a valid location', NOW());
        
        -- renovation_type (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'renovation_type', 'What type of renovation are you doing?', 'SELECTION', true, 'Kitchen Remodel', 1, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Kitchen Remodel', 'Select your renovation type', 'Project Scope', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Kitchen Remodel", "Bathroom Renovation", "Whole House", "Basement Finishing", "Attic Conversion", "Addition", "Master Suite", "Living Room", "Exterior Renovation", "Flooring Replacement", "Windows/Doors", "Roof Replacement"]}', 'Please select a valid renovation type', NOW());
        
        -- square_footage (NUMBER)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'square_footage', 'How many square feet are being renovated?', 'NUMBER', true, null, 2, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '300', 'Enter square footage of renovation area', 'Project Scope', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'RANGE', '{"min": 50, "max": 10000}', 'Square footage must be between 50 and 10,000', NOW());
        
        -- total_budget (CURRENCY)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'total_budget', 'What is your total renovation budget?', 'CURRENCY', true, null, 3, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '75000 USD', 'Enter total budget with currency', 'Financial', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^[0-9]+(\\.[0-9]{2})?\\s?(USD|EUR|GBP|CAD|AUD)$"}', 'Please enter budget with currency (e.g., 75000 USD)', NOW());
        
        -- project_duration (DURATION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'project_duration', 'How long do you expect the renovation to take?', 'DURATION', true, '8 weeks', 4, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '8 weeks', 'Enter expected project duration', 'Timeline', NOW());
        
        
        -- start_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'start_date', 'When do you want to start the renovation?', 'DATE', true, null, 5, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-05-01', 'Format: YYYY-MM-DD', 'Timeline', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', (CURRENT_DATE + INTERVAL '2 weeks')::date::text, (CURRENT_DATE + INTERVAL '1 year')::date::text), 'Start date must be at least 2 weeks from now and within 1 year', NOW());
        
        -- completion_date (DATE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'completion_date', 'When do you want the renovation completed?', 'DATE', false, null, 6, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '2025-07-01', 'Target completion date', 'Timeline', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'DATE_RANGE', format('{"min": "%s", "max": "%s"}', (CURRENT_DATE + INTERVAL '1 month')::date::text, (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Completion date must be at least 1 month from now', NOW());
        
        -- style_preference (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'style_preference', 'What is your preferred design style?', 'SELECTION', false, 'Modern', 7, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Modern', 'Select your design style preference', 'Design', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Modern", "Traditional", "Contemporary", "Farmhouse", "Industrial", "Scandinavian", "Mediterranean", "Craftsman", "Mid-Century", "Transitional", "Rustic", "Minimalist"]}', 'Please select a valid design style', NOW());
        
        -- contractor_phone (PHONE)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'contractor_phone', 'Contractor contact phone number', 'PHONE', false, null, 8, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, '+1-555-987-6543', 'Primary contractor phone number', 'Contractors', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'PATTERN', '{"pattern": "^\\+?1?[- ]?\\(?([0-9]{3})\\)?[- ]?([0-9]{3})[- ]?([0-9]{4})$"}', 'Please enter a valid phone number (e.g., +1-555-987-6543)', NOW());
        
        -- permits_required (BOOLEAN)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'permits_required', 'Will building permits be required?', 'BOOLEAN', false, 'true', 9, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'true', 'Select true if permits are needed', 'Legal', NOW());
        
        -- living_situation (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'living_situation', 'Will you live in the home during renovation?', 'SELECTION', false, 'Living on-site', 10, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Living on-site', 'Select your living arrangement during renovation', 'Logistics', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["Living on-site", "Temporary relocation", "Vacant property", "Partial occupancy", "Weekend project", "Seasonal work"]}', 'Please select a valid living situation', NOW());
        
        -- priority_level (SELECTION)
        INSERT INTO template_parameters (id, template_id, name, description, type, required, default_value, display_order, created_at)
        VALUES (gen_random_uuid(), renovation_template_uuid, 'priority_level', 'What is the priority level of this renovation?', 'SELECTION', false, 'Medium', 11, NOW())
        RETURNING id INTO param_id;
        
        INSERT INTO parameter_metadata (parameter_id, placeholder, help_text, display_group, created_at)
        VALUES (param_id, 'Medium', 'Select renovation priority', 'Planning', NOW());
        
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        VALUES (param_id, 'ALLOWED_VALUES', '{"values": ["High", "Medium", "Low", "Urgent", "Flexible", "Investment"]}', 'Please select a valid priority level', NOW());
        
        -- Additional Home Renovation validation rules
        
        -- Property location format validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 5, "max": 200}', 'Property location must be between 5 and 200 characters', NOW()
        FROM template_parameters WHERE template_id = renovation_template_uuid AND name = 'property_location';
        
        -- Budget minimum validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'RANGE', '{"min": 1000}', 'Renovation budget must be at least 1000 in any currency', NOW()
        FROM template_parameters WHERE template_id = renovation_template_uuid AND name = 'total_budget';
        
        -- Start date reasonable validation
        INSERT INTO parameter_validation_rules (parameter_id, validation_type, rule_value, error_message, created_at)
        SELECT param_id, 'DATE_RANGE', format('{"max": "%s"}', (CURRENT_DATE + INTERVAL '2 years')::date::text), 'Start date cannot be more than 2 years in the future', NOW()
        FROM template_parameters WHERE template_id = renovation_template_uuid AND name = 'start_date';
        
    END IF;
END $$;