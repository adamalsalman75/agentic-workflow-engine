# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot application built with Java 24 that integrates Spring AI with OpenAI models to create an agentic workflow engine with dependency-aware parallel execution. The project uses Maven for build management and follows standard Spring Boot project structure.

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
        └── AgenticWorkflowEngineApplicationTests.java
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

## Configuration
- Application configuration is managed through `application.yaml`
- Spring AI OpenAI integration requires appropriate API keys and configuration
- The application name is set to "agentic-workflow-engine"

## Java Principals
- Use immutable records
- Use RestClient over RestTemplate
- Use Spring Data JDBC 
- Use postgres
- Use virtual threads
- Use Structured Task Scope for asynchronous code over reactive.