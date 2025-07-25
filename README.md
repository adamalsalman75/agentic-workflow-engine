# Agentic Workflow Engine

A Spring Boot application that uses AI agents to break down complex goals into tasks, execute them, and provide comprehensive summaries. Built with Java 24, Spring AI, and OpenAI integration.

## Architecture

The system uses four main AI agents with dependency-aware parallel execution:

1. **TaskPlan Agent** - Creates task plans with dependency analysis (blocking vs informational dependencies)
2. **Task Agent** - Executes individual tasks with context from completed tasks
3. **Goal Agent** - Summarizes the entire workflow execution and results
4. **Workflow Orchestrator** - Coordinates parallel execution based on task dependencies
5. **Dependency Resolver** - Analyzes task dependencies and enables optimal parallel execution

## Features

- ✅ **Dependency-aware parallel execution** - Tasks run in parallel when dependencies allow
- ✅ **Smart task planning** with blocking and informational dependencies
- ✅ **Dynamic task planning** that adapts based on task results
- ✅ **Context-aware task execution** with completed task results
- ✅ **Circular dependency detection** prevents infinite loops
- ✅ **Virtual threads with StructuredTaskScope** for efficient async operations
- ✅ **Immutable domain records** following Java best practices
- ✅ **PostgreSQL integration** with dependency tracking
- ✅ **Comprehensive logging** for debugging parallel execution
- ✅ **OpenAI GPT-4 integration** for intelligent task planning
- ✅ **Rate limiting resilience** with exponential backoff retry logic
- ✅ **Optimized token usage** to reduce API costs and avoid limits

## Prerequisites

- Java 24 (with preview features enabled)
- PostgreSQL database
- OpenAI API key
- Maven 3.6+

## Setup

### 1. Database Setup

Start PostgreSQL using Docker Compose:

```bash
docker compose up -d
```

This will create a PostgreSQL database with:
- Database: `agentic_workflow`
- Username: `postgres`
- Password: `password`
- Port: `5432`

### 2. Environment Variables

Set your OpenAI API key:

```bash
export OPENAI_API_KEY="your-openai-api-key-here"
```

### 3. Build and Run

```bash
# Build the application
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Usage

### Execute Workflow

**Endpoint:** `POST /api/workflow/execute`

**Request Body:**
```json
{
  "query": "Your goal or task description here"
}
```

**Response:**
```json
{
  "goal": {
    "id": "uuid",
    "query": "Your original query",
    "tasks": [
      {
        "id": "uuid",
        "description": "Task description",
        "result": "Task execution result",
        "status": "COMPLETED",
        "createdAt": "2024-01-01T10:00:00Z",
        "completedAt": "2024-01-01T10:05:00Z"
      }
    ],
    "summary": "AI-generated summary of the workflow execution",
    "status": "COMPLETED",
    "createdAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:10:00Z"
  },
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T10:10:00Z",
  "duration": "PT10M",
  "success": true
}
```

## Example cURL Commands

### 1. Plan a Vacation

```bash
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Plan a 7-day vacation to Japan including flights, accommodation, itinerary, and budget"
  }'
```

### 2. Start a Business

```bash
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Help me start a coffee shop business including market research, business plan, permits, and initial setup"
  }'
```

### 3. Learn a New Skill

```bash
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Create a learning plan to become proficient in Python programming within 3 months"
  }'
```

### 4. Organize an Event

```bash
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Organize a company team building event for 50 people including venue, activities, catering, and budget"
  }'
```

### 5. Health and Fitness Goal

```bash
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Create a comprehensive plan to lose 20 pounds in 4 months including diet, exercise, and tracking"
  }'
```

## Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# OpenAI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4
spring.ai.openai.chat.options.temperature=0.7

# Database Configuration
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/agentic_workflow}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:password}

# Logging
logging.level.root=DEBUG
logging.level.dev.alsalman.agenticworkflowengine=DEBUG
```

### Environment Variables

- `OPENAI_API_KEY` - Your OpenAI API key (required)
- `DATABASE_URL` - PostgreSQL connection URL (optional, defaults to localhost)
- `DATABASE_USERNAME` - Database username (optional, defaults to postgres)
- `DATABASE_PASSWORD` - Database password (optional, defaults to password)

## Development

### Running Tests

```bash
./mvnw test
```

### Building for Production

```bash
./mvnw clean package
java --enable-preview -jar target/agentic-workflow-engine-0.0.1-SNAPSHOT.jar
```

### Database Schema

The application automatically creates the required database schema on startup:

- `goals` table - Stores workflow goals and summaries  
- `tasks` table - Stores individual tasks with dependency information
- `task_dependencies` table - Stores detailed dependency relationships

## How Parallel Execution Works

The system intelligently executes tasks in parallel based on their dependencies:

### Dependency Types
- **Blocking Dependencies**: Task cannot start until these dependencies complete
- **Informational Dependencies**: Task can start but benefits from these dependency results

### Execution Flow
1. **AI Analysis**: TaskPlan agent analyzes goals and identifies task dependencies
2. **Validation**: System checks for circular dependencies and validates relationships  
3. **Batch Execution**: Tasks with satisfied dependencies execute in parallel batches
4. **Dynamic Updates**: Plan reviews after each task may add/modify/remove tasks
5. **Continues**: Process repeats until all tasks complete

### Example: Vacation Planning
```
TASKS:
1. Research destinations
2. Check weather patterns  
3. Find flights (depends on 1 - blocking)
4. Book accommodation (depends on 1, 3 - blocking)
5. Plan daily activities (depends on 1, 2 - informational)

EXECUTION BATCHES:
Batch 1: [Task 1] (no dependencies)
Batch 2: [Task 2, Task 3] (parallel - dependencies satisfied)  
Batch 3: [Task 4, Task 5] (parallel - dependencies satisfied)
```

This approach maximizes efficiency while ensuring correct execution order.

## Logging

The application provides comprehensive logging at DEBUG level:

- Task planning and creation
- Individual task execution progress
- Plan reviews and updates
- Final summaries and execution times
- Error handling and debugging information

## Java 24 Features

This application leverages Java 24 preview features:

- **StructuredTaskScope** - For structured concurrency and virtual threads
- **Virtual Threads** - For efficient async operations
- **Preview Features** - Enabled via `--enable-preview` flag

## Architecture Principles

- **Immutable Records** - All domain models use immutable Java records
- **Virtual Threads** - Async operations use virtual threads over reactive programming
- **Spring Data JDBC** - Simple, direct database access
- **PostgreSQL** - Reliable, scalable database storage
- **Structured Concurrency** - Better async code organization and error handling

## Troubleshooting

### Common Issues

1. **Preview API warnings** - These are expected when using Java 24 preview features
2. **Database connection errors** - Ensure PostgreSQL is running and accessible
3. **OpenAI API errors** - Verify your API key is set and has sufficient credits
4. **Compilation errors** - Ensure Java 24 is installed and `--enable-preview` is configured

### Logs

Check application logs for detailed execution information:

```bash
# View recent logs
tail -f logs/application.log

# Or check console output when running with Maven
./mvnw spring-boot:run
```