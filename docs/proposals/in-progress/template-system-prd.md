# Workflow Template System - Product Requirements Document

## Executive Summary

The Workflow Template System transforms our generic agentic workflow engine into a user-friendly platform that delivers immediate real-world value through pre-configured, expert-designed workflow recipes.

**Vision**: Enable users to leverage sophisticated AI workflows without needing to craft complex queries from scratch.

**Value Proposition**: Templates bridge the gap between powerful generic capabilities and practical everyday use cases.

## Problem Statement

### Current State
- Generic workflow engine requires users to write detailed, complex queries
- Users struggle to leverage the full power of dependency-aware parallel execution
- High barrier to entry for non-technical users
- No standardized patterns for common use cases

### User Pain Points
1. **Blank Canvas Problem**: Users don't know how to structure complex multi-step workflows
2. **Query Quality**: Manual queries often miss important considerations or lack detail
3. **Inconsistent Results**: Different users get varying quality based on their query-writing skills
4. **Learning Curve**: Users need deep understanding of workflow orchestration to get value

## Solution Overview

### Core Concept
**Workflow Templates** are pre-built, expert-designed workflow recipes with:
- **Guided Input Collection**: Structured parameters with validation
- **Professional Query Generation**: Comprehensive prompts that leverage full engine capabilities
- **Consistent Quality**: Reproducible, high-quality workflow execution
- **Domain Expertise**: Templates encode best practices for specific use cases

### Key Benefits
- **Immediate Value**: Users get sophisticated workflows on day one
- **Quality Assurance**: Professional-grade prompts generate better task plans
- **Reduced Complexity**: Parameter forms vs. free-text query writing
- **Scalability**: Template library grows over time with community contributions

## User Stories

### Primary Users

**End Users (Workflow Consumers)**
- As a user, I want to plan a complete vacation without missing important details
- As a user, I want guided help starting a business with regulatory compliance
- As a user, I want to organize complex events with vendor coordination

**Power Users (Template Creators)**
- As a domain expert, I want to create reusable templates for my field
- As a template creator, I want to validate parameters and provide helpful constraints
- As an administrator, I want to manage template visibility and permissions

**Developers (System Integrators)**  
- As a developer, I want to programmatically execute templates via API
- As a developer, I want to track template usage and performance metrics
- As a developer, I want to extend templates with custom parameter types

## Technical Architecture

### Integration Pattern
Templates **enhance** the existing workflow engine rather than replacing it:

```
User Input → Template Parameter Validation → Query Generation → Existing WorkflowOrchestrator
```

### Core Components

1. **Template Definition System**
   - Template metadata (name, description, category)
   - Parameter definitions with types and validation
   - Prompt templates with placeholder substitution

2. **Parameter System** *(Simplified for LLM Flexibility)*
   - Core parameter types (TEXT, NUMBER, BOOLEAN, SELECTION)
   - Basic validation (required/optional, allowed values for selections)
   - Default values and user guidance through metadata
   - **Design Philosophy**: Trust LLM's natural language processing capabilities over rigid validation

3. **Template Execution Engine**
   - Parameter validation and merging with defaults
   - Prompt rendering with placeholder substitution
   - Integration with existing WorkflowOrchestrator

4. **Template Management**
   - Template discovery and search
   - Usage tracking and analytics
   - Version management and updates

## Architectural Evolution: LLM-First Design

### Key Insight: Over-Engineering for LLM Consumption
During Phase 2 implementation, we discovered that our parameter validation system was over-engineered for LLM consumption. The initial approach included:

**Complex System (Stories 1-5)**:
- 13 parameter types: TEXT, NUMBER, SELECTION, DATE, CURRENCY, LOCATION, EMAIL, PHONE, URL, BOOLEAN, PERCENTAGE, TIME, DURATION
- 4 validation rule types: PATTERN, RANGE, DATE_RANGE, ALLOWED_VALUES
- Rigid format enforcement (e.g., exact email formats, currency parsing)

**LLM Reality Check**:
LLMs naturally handle flexible inputs and don't require strict validation:
- ✅ "Send email to john.doe@company" vs ❌ requiring `john.doe@company.com`
- ✅ "Budget is around $50k" vs ❌ requiring `50000 USD` format
- ✅ "Launch sometime next summer" vs ❌ requiring `2025-06-15` format

**Simplified Approach (Story 7)**:
- 4 core parameter types: TEXT, NUMBER, BOOLEAN, SELECTION
- 1 validation rule: ALLOWED_VALUES (for selections only)
- Trust LLM's natural language processing capabilities
- Focus on user guidance through rich metadata rather than rigid validation

### Benefits of LLM-First Design
- **Code Reduction**: ~70% less validation logic
- **Better User Experience**: Accept natural language inputs
- **Reduced Complexity**: Fewer failure points and edge cases
- **Faster Development**: Simpler parameter system accelerates template creation
- **AI Alignment**: System architecture matches AI capabilities

## Implementation Phases

### Phase 1: Foundation (MVP) 🎯 **Start Here**

**Goal**: Prove the template concept with minimal viable functionality

**Scope**:
- Simple template domain model (name, description, prompt template)
- Basic parameter types (TEXT, NUMBER, SELECTION)
- Single hardcoded template for validation
- REST API for template execution
- Integration with existing WorkflowOrchestrator

**Success Criteria**:
- One working template (e.g., "Simple Trip Planner")
- Parameter validation and prompt generation
- Successful execution through existing workflow engine
- REST API responds with goal ID

**Technical Tasks**:
1. Create minimal domain models without complex data types
2. Build basic template service with validation
3. Create simple REST endpoint for template execution
4. Add one template as proof of concept
5. Test end-to-end flow

**Estimated Effort**: 1-2 days

### Phase 2: Core Parameter System

**Goal**: LLM-optimized parameter handling with user experience focus

**Scope**:
- Simplified parameter types (TEXT, NUMBER, BOOLEAN, SELECTION) optimized for LLM consumption
- Basic validation focused on required/optional and selection constraints
- Rich metadata system for user guidance (placeholders, help text, grouping)
- Parameter discovery API for UI builders
- Multiple production templates across different categories

**Success Criteria**:
- 5 production-quality templates with flexible LLM-friendly parameters
- User-friendly metadata and guidance without rigid validation
- Template search and categorization
- Frontend can dynamically generate forms from parameter definitions
- LLM can process natural language inputs effectively

**Technical Tasks**:
1. ✅ Implement comprehensive parameter types with validation *(Completed - Story 1-5)*
2. 🔄 **Simplify parameter system for LLM flexibility** *(Story 7 - High Priority)*
3. Add template search and filtering *(Story 6)*
4. ✅ Build 5 comprehensive templates *(Completed - Story 5)*
5. Add template categorization *(Story 6)*

**Estimated Effort**: 2-3 days *(Reduced due to simplified approach)*

**⚠️ Architectural Insight**: Initial implementation used complex validation (13 parameter types, 4 validation rules) which is over-engineered for LLM consumption. Story 7 simplifies this approach to align with LLM capabilities.


## Database Design

### Incremental Schema Strategy

**Phase 1**: Single table approach
```sql
CREATE TABLE workflow_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    prompt_template TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

**Phase 2**: Add parameter support
```sql
CREATE TABLE template_parameters (
    id UUID PRIMARY KEY,
    template_id UUID REFERENCES workflow_templates(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT false,
    validation_rules JSONB,
    order_index INT NOT NULL
);
```


## API Design

### Template Discovery
```
GET /api/workflow/templates
GET /api/workflow/templates/categories  
GET /api/workflow/templates/{id}
```

### Template Execution
```
POST /api/workflow/templates/{id}/validate
POST /api/workflow/templates/{id}/execute
```


## Success Metrics

### User Adoption
- Template execution rate vs. direct workflow usage
- User retention after first template execution
- Average templates used per user

### Quality Metrics
- Template execution success rate
- User satisfaction scores for template results
- Time to workflow completion

### Business Metrics
- Reduced support requests for workflow creation
- Increased platform engagement
- Template library growth rate

## Risk Mitigation

### Technical Risks
- **Database Complexity**: Start with simple schemas, evolve incrementally
- **Parameter Validation**: Begin with basic types, add complexity gradually
- **Performance**: Monitor template execution overhead, optimize if needed

### Product Risks
- **Template Quality**: Start with carefully curated templates
- **User Confusion**: Clear documentation and examples for each template
- **Scope Creep**: Strict phase boundaries, resist feature bloat

## Non-Functional Requirements

### Performance
- Template parameter validation: < 100ms
- Template execution overhead: < 5% of total workflow time
- Template discovery: < 200ms for catalog browsing

### Scalability
- Support 1000+ templates in catalog
- Handle 100+ concurrent template executions
- Parameter validation scales with template complexity

### Usability
- Parameter forms auto-generate from template definitions
- Clear error messages for validation failures
- Template descriptions include usage examples

## Future Enhancements

### Template Management (Previously Phase 3)
- Template creation and editing APIs
- Template versioning and updates
- Usage analytics and tracking
- Template permissions and visibility
- Template execution history

### Advanced Templates
- Multi-step templates with conditional logic
- Template composition and inheritance
- Dynamic parameter dependencies

### Community Features
- Public template marketplace
- User-contributed templates
- Template ratings and reviews

### AI-Powered Features
- Auto-suggest templates based on user query
- Template optimization based on usage patterns
- Natural language template creation

## Conclusion

The Template System represents a strategic evolution from a powerful generic engine to a user-friendly platform that delivers immediate value. By implementing Phase 1 (Foundation) and Phase 2 (Core Parameter System), we achieve a complete and valuable template system that provides sophisticated workflow templates with robust parameter validation.

**Recommendation**: Implement Phase 1 and Phase 2 to deliver a comprehensive template system. Phase 3 (Template Management) has been removed from scope as the core functionality provides sufficient value without the additional complexity of dynamic template creation and administration.