# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot application built with Java 24 that integrates Spring AI with OpenAI models to create an agentic workflow engine with dependency-aware parallel execution. The project uses Maven for build management and follows standard Spring Boot project structure.

## Key Git Rules

### Branch Permissions
- **develop branch**: Claude must ask permission before pushing
- **main branch**: Claude must NEVER push directly
- **feature branches**: Claude may commit and push freely

### Commit Guidelines
- Feature branches should reference GitHub issue: `feature/story-{number}-{description}`
  - Example: `feature/story-5-advanced-validation`
  - Example: `feature/story-12-chat-interface`
- Commits should be atomic and well-described
- Use conventional commit format (feat:, fix:, docs:, chore:, test:)

## Ways of Working

### Feature Development Process
1. **Ideation & Planning**
   - Brainstorm ideas and discuss options
   - Select one idea for development
   - Create comprehensive PRD document (Product Requirements Document)
   - Place PRD in `docs/proposals/in-review/` for evaluation

2. **PRD Approval & Epic Creation**
   - Review and approve PRD 
   - Move PRD to `docs/proposals/in-progress/`
   - Create GitHub Epic linked to the PRD

3. **Implementation Planning**
   - Break PRD into incremental deliverables as GitHub Issues/Stories
   - Define clear acceptance criteria for each issue
   - Link issues to Epic and PRD sections for traceability
   - Implement phase-by-phase approach as defined in PRD

4. **Story Development & Testing**
   - Create feature branch from `develop` branch for each story (naming: `feature/[story-name]`)
   - Implement story according to acceptance criteria
   - Full testing required before PR
   - Create PR back to `develop` after review and testing
   - Close story after PR is merged
   - Maintain comprehensive test coverage

5. **Story Refinement Session** (After Each Story Completion)
   - Review lessons learned from completed story
   - Assess impact on upcoming stories in the Epic
   - Update GitHub issues for subsequent stories based on:
     - Architecture changes or refactoring from completed story
     - New technical insights or requirements discovered
     - API endpoint changes, class renames, or schema updates
   - Refine acceptance criteria and technical details as needed
   - Ensure all upcoming stories align with current system state

5. **Epic Completion & Integration**
   - Complete all stories in Epic through individual PRs
   - Final Epic testing on `develop` branch
   - Update PRD with phase completion status
   - Move completed PRD to `docs/proposals/completed/` when all phases done

6. **Production Deployment**
   - Manual testing on `develop` branch
   - Merge `develop` to `main` branch
   - Deploy to GKE production environment

### Acceptance Criteria Guidelines
- **Focus on OUTCOMES, not implementation details**
- **Use business language, not technical jargon**
- **Define measurable success criteria**
- **Avoid specifying specific technologies unless absolutely required**

**❌ Poor A/C (too technical):**
- "Use PostgreSQL with JSONB for flexible metadata storage"
- "Create two database indexes for performance"
- "Develop domain model `TemplateParameter`"

**✅ Good A/C (outcome-focused):**
- "Template parameters can be dynamically configured without code changes"
- "Parameter validation rules can be modified through the UI"
- "System supports at least 100 templates with 10 parameters each with sub-second response times"

### Definition of Done (Each Story)
- [ ] **Acceptance criteria validated** - Each A/C is outcome-focused and measurable
- [ ] Feature implemented according to acceptance criteria
- [ ] Unit tests written and passing
- [ ] Integration tests updated if needed
- [ ] Manual testing completed
- [ ] No breaking changes to existing functionality
- [ ] **Technical decisions documented** - Any deviations from original technical approach explained
- [ ] PR created and reviewed
- [ ] PR merged to `develop` branch
- [ ] Story closed on GitHub with detailed completion comment including:
  - Summary of implemented features
  - How each acceptance criterion was met
  - Technical changes made (files, classes, endpoints)
  - Test coverage added
  - Any architectural decisions or trade-offs
  - **Explanation of any deviations from original technical approach**
- [ ] Documentation updated (CLAUDE.md, README.md) if needed

### Definition of Done (Each Epic/PRD)
- [ ] All phases and stories completed
- [ ] Full end-to-end testing
- [ ] Performance testing if applicable
- [ ] Security review if applicable
- [ ] Documentation comprehensive and up-to-date
- [ ] Ready for production deployment

## Core Architecture
The system uses a pure orchestration pattern with service-based architecture:

### Orchestration Layer
- **WorkflowOrchestrator**: Pure orchestrator that coordinates workflow steps through service calls. Focuses only on orchestration flow without business logic.

### Service Layer (Business Logic)
- **GoalService**: Manages goal lifecycle and persistence
- **TaskPlanService**: Encapsulates task plan creation (wraps TaskPlanAgent)
- **TaskPersistenceService**: Coordinates task persistence operations
- **TaskPreparationService**: Handles dependency validation and cleanup
- **TaskExecutionService**: Manages parallel task execution and dependency resolution
- **PlanReviewService**: Handles plan reviews and task state updates
- **WorkflowSummaryService**: Generates workflow summaries

### AI Agent Layer
- **TaskPlanAgent**: Creates flexible task plans with dependency analysis
- **TaskAgent**: Executes individual tasks with context from completed tasks
- **GoalAgent**: Summarizes workflow execution results

### Infrastructure Layer
- **TaskDependencyResolver**: Coordinates task persistence and UUID mapping
- **DependencyResolver**: Analyzes and validates task dependencies
- **WorkflowPersistenceService**: Database persistence operations

## Key Technologies
- **Spring Boot 3.5.4**: Main application framework
- **Spring AI 1.0.0**: AI integration framework with OpenAI support
- **Java 24**: Programming language version
- **Maven**: Build tool and dependency management
- **JUnit 5**: Testing framework

## Common Commands

### Build and Run
```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Run the application
./mvnw spring-boot:run

# Package the application
./mvnw clean package

# Run a single test class
./mvnw test -Dtest=AgenticWorkflowEngineApplicationTests

# Clean build artifacts
./mvnw clean
```

### Development
```bash
# Compile without running tests
./mvnw compile

# Run in debug mode
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## Project Structure
```
src/
├── main/
│   ├── java/dev/alsalman/agenticworkflowengine/
│   │   ├── AgenticWorkflowEngineApplication.java  # Main Spring Boot application
│   │   ├── agent/                                 # AI agents
│   │   │   ├── TaskPlanAgent.java                # Creates task plans with dependencies
│   │   │   ├── TaskAgent.java                    # Executes individual tasks
│   │   │   └── GoalAgent.java                    # Summarizes workflow results
│   │   ├── controller/                           # REST controllers
│   │   │   └── WorkflowController.java           # Main workflow API endpoint
│   │   ├── domain/                               # Domain models (immutable records)
│   │   │   ├── Task.java                         # Task with dependency support
│   │   │   ├── Goal.java                         # Workflow goal
│   │   │   ├── TaskDependency.java               # Dependency relationships
│   │   │   ├── TaskPlan.java                     # Task plan with dependencies
│   │   │   └── WorkflowResult.java               # Execution results
│   │   └── service/                              # Business logic services
│   │       ├── WorkflowOrchestrator.java         # Pure orchestration coordinator
│   │       ├── GoalService.java                  # Goal lifecycle management
│   │       ├── TaskPlanService.java              # Task plan creation service
│   │       ├── TaskPersistenceService.java       # Task persistence coordination
│   │       ├── TaskPreparationService.java       # Task validation and cleanup
│   │       ├── TaskExecutionService.java         # Parallel execution and dependency resolution
│   │       ├── PlanReviewService.java            # Plan review and updates
│   │       ├── WorkflowSummaryService.java       # Summary generation
│   │       ├── TaskDependencyResolver.java       # Task persistence and UUID mapping
│   │       ├── DependencyResolver.java           # Dependency analysis
│   │       └── WorkflowPersistenceService.java   # Database persistence operations
│   └── resources/
│       ├── application.yaml                     # Application configuration
│       └── schema.sql                           # Database schema with dependencies
└── test/
    └── java/dev/alsalman/agenticworkflowengine/
        ├── AgenticWorkflowEngineApplicationTests.java
        ├── controller/
        │   └── WorkflowControllerTest.java           # REST API endpoint tests
        └── service/                                  # Comprehensive service test suite
            ├── WorkflowOrchestratorTest.java         # Orchestration flow tests
            ├── GoalServiceTest.java                  # Goal lifecycle tests
            ├── TaskPlanServiceTest.java              # Task plan creation tests
            ├── TaskPersistenceServiceTest.java       # Task persistence tests
            ├── TaskPreparationServiceTest.java       # Dependency validation tests
            ├── TaskExecutionServiceTest.java         # Parallel execution tests
            ├── PlanReviewServiceTest.java            # Plan review and update tests
            ├── WorkflowSummaryServiceTest.java       # Summary generation tests
            ├── WorkflowPersistenceServiceTest.java   # Database operations tests
            ├── DependencyResolverTest.java           # Dependency analysis tests
            └── ResilientChatClientTest.java          # AI integration resilience tests
```

## Architecture Notes
- **Pure Orchestration Pattern**: WorkflowOrchestrator contains no business logic, only coordinates service calls
- **Service-Based Architecture**: Each service has single responsibility and encapsulates related business logic
- **AI Agent Encapsulation**: Agents are wrapped by services, not directly used by orchestrator
- **Dependency-Aware Parallel Execution**: Tasks analyzed and executed in optimal parallel batches
- **Enterprise-Ready Layering**: Clear separation between orchestration, business logic, and infrastructure
- **Spring AI Integration**: OpenAI GPT-4o for superior task dependency analysis and planning through service layer
- **PostgreSQL Persistence**: Shared infrastructure layer for data consistency
- **Virtual Threads**: StructuredTaskScope for efficient async operations
- **Comprehensive Testing**: Each service independently testable with full coverage
- **Immutable Domain Models**: Records-based domain design for thread safety

## Testing Strategy
The project follows enterprise testing practices with comprehensive coverage:

### Service Layer Tests
- **Independent Testing**: Each service can be tested in isolation with mocked dependencies
- **Behavior Verification**: Tests verify service interactions and business logic correctness
- **Edge Case Coverage**: Comprehensive testing of error scenarios and boundary conditions
- **Mockito Integration**: Clean mocking of dependencies for focused unit testing

### Test Categories
- **Unit Tests**: All service classes have dedicated test suites with 90%+ coverage
- **Integration Tests**: WorkflowOrchestratorTest verifies end-to-end service coordination
- **Controller Tests**: REST API endpoint testing with proper error handling
- **Infrastructure Tests**: Database operations and AI client resilience testing

### Key Testing Features
- **Parallel Execution Testing**: Verification of concurrent task execution with virtual threads
- **Dependency Resolution Testing**: Complex dependency scenarios and circular dependency detection
- **AI Integration Testing**: Resilient chat client with retry logic and error handling
- **Database Testing**: Comprehensive persistence operations with transaction management

## Configuration
- Application configuration is managed through `application.yaml`
- Spring AI OpenAI integration requires appropriate API keys and configuration
- The application name is set to "agentic-workflow-engine"

## Java Principles

### Core Design Principles
- **Immutable Records**: Use Java records for all domain models to ensure thread safety and immutability
  ```java
  public record Task(UUID id, String description, String status) {}
  ```
- **Constructor-based Dependency Injection**: Prefer constructor injection over field injection
  ```java
  public SimpleTemplateService(SimpleTemplateRepository repository) {
      this.repository = repository;
  }
  ```
- **Service Layer Pattern**: Encapsulate business logic in service classes, keep controllers thin
- **Repository Pattern**: Use Spring Data JDBC repositories for data access

### Technology Choices
- **RestClient over RestTemplate**: Use the modern RestClient for HTTP calls
- **Spring Data JDBC over JPA**: Simpler, more predictable than JPA for our use cases
- **PostgreSQL Database**: 
  - Use UUID primary keys with `gen_random_uuid()`
  - Store complex data as JSONB when appropriate
  - Use proper indexes for performance
- **Virtual Threads**: Leverage Java 21+ virtual threads for better concurrency
- **Structured Concurrency**: Use StructuredTaskScope for parallel execution instead of reactive patterns

### Testing Practices
- **Comprehensive Test Coverage**: Aim for 90%+ coverage on service layers
- **Mock External Dependencies**: Use Mockito for unit tests
- **Test Naming**: Use descriptive test names that explain the scenario
  ```java
  @Test
  void executeWorkflow_WithInvalidParameters_ShouldReturnValidationError() {
      // test implementation
  }
  ```

### Validation and Error Handling
- **Parameter Validation**: Create dedicated validator classes for complex validation
- **Explicit Error Messages**: Provide clear, actionable error messages
- **Result Pattern**: Use custom Result types for operations that can fail
  ```java
  public record ValidationResult(boolean success, String errorMessage) {}
  ```

### Spring Boot Best Practices
- **Configuration**: Use `@ConfigurationProperties` for grouped settings
- **Profiles**: Use Spring profiles for environment-specific configuration
- **Actuator**: Include health checks and metrics endpoints
- **Logging**: Use SLF4J with descriptive log messages at appropriate levels

### Code Organization
- **Package by Feature**: Group related classes by feature, not by layer
- **Clear Naming**: Use descriptive names that convey intent
- **Small, Focused Classes**: Each class should have a single responsibility
- **Avoid Primitive Obsession**: Use domain types instead of primitives when it adds clarity