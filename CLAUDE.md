# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Spring Boot application built with Java 24 that integrates Spring AI with OpenAI models to create an agentic workflow engine with dependency-aware parallel execution. The project uses Maven for build management and follows standard Spring Boot project structure.

## Available Agents
The following specialized agents help enforce project standards:

- **[Code Standards](.claude/agents/code.md)**: Reviews Java principles and Spring Boot best practices
- **[Architecture](.claude/agents/architecture.md)**: Ensures enterprise-ready design patterns and contains project structure
- **[Test Guardian](.claude/agents/test-guardian.md)**: Enforces comprehensive testing standards  
- **[Story Manager](.claude/agents/story-manager.md)**: Guides story implementation per Ways of Working
- **[Git Guardian](.claude/agents/git-guardian.md)**: Enforces git workflow and branch protections
- **[Ways of Working](.claude/agents/ways-of-working.md)**: Enforces development process and acceptance criteria

## Key Technologies
- **Spring Boot 3.5.4**: Main application framework
- **Spring AI 1.0.0**: AI integration framework with OpenAI support
- **Java 24**: Programming language version
- **Maven**: Build tool and dependency management
- **JUnit 5**: Testing framework

## Common Commands

### Testing (Run before handoff)
```bash
# Run all tests - MUST PASS before user review
./mvnw test

# Run a single test class
./mvnw test -Dtest=AgenticWorkflowEngineApplicationTests

# Clean and compile
./mvnw clean compile
```

### For User Review (Commands for manual testing)
```bash
# Run the application locally
./mvnw spring-boot:run

# Run test scripts
./test-local.sh
./test-kubernetes.sh

# Package the application
./mvnw clean package

# Run in debug mode
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## Configuration
- Application configuration is managed through `application.yaml`
- Spring AI OpenAI integration requires appropriate API keys and configuration
- The application name is set to "agentic-workflow-engine"