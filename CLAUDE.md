# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot application built with Java 24 that integrates Spring AI with OpenAI models to create an agentic workflow engine with dependency-aware parallel execution. The project uses Maven for build management and follows standard Spring Boot project structure.

## Core Architecture
The system uses AI agents with intelligent parallel task execution:
- **TaskPlanAgent**: Creates flexible task plans, identifying independent tasks for parallel execution and creating dependencies only when logically necessary.
- **TaskDependencyResolver**: Coordinates task persistence and ensures dependency UUIDs are correctly mapped between planning and execution phases.
- **TaskAgent**: Executes tasks with context from completed dependencies
- **GoalAgent**: Summarizes workflow execution results
- **WorkflowOrchestrator**: Coordinates parallel execution based on task dependencies
- **DependencyResolver**: Analyzes and validates task dependencies for optimal execution

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
│   │   └── service/                              # Business logic
│   │       ├── WorkflowOrchestrator.java         # Coordinates parallel execution
│   │       ├── TaskDependencyResolver.java       # Coordinates task persistence and UUID mapping
│   │       ├── DependencyResolver.java           # Analyzes task dependencies
│   │       └── WorkflowPersistenceService.java   # Database persistence operations
│   └── resources/
│       ├── application.properties                # Application configuration
│       └── schema.sql                           # Database schema with dependencies
└── test/
    └── java/dev/alsalman/agenticworkflowengine/
        └── AgenticWorkflowEngineApplicationTests.java
```

## Architecture Notes
- Full-featured agentic workflow engine with dependency-aware parallel execution
- Tasks are analyzed for dependencies and executed in optimal parallel batches
- TaskDependencyResolver coordinates UUID mapping between planning and persistence phases
- Enhanced TaskPlanAgent prompts emphasize parallel execution when tasks are independent
- Spring AI integration with OpenAI GPT-4 for intelligent task planning
- PostgreSQL database stores tasks, goals, and dependency relationships
- Virtual threads with StructuredTaskScope for efficient async operations
- Comprehensive logging for debugging parallel execution flows

## Configuration
- Application configuration is managed through `application.properties`
- Spring AI OpenAI integration requires appropriate API keys and configuration
- The application name is set to "agentic-workflow-engine"

## Java Principals
- Use immutable records
- Use RestClient over RestTemplate
- Use Spring Data JDBC 
- Use postgres
- Use virtual threads
- Use Structured Task Scope for asynchronous code over reactive.