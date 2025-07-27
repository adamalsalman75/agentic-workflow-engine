# Agentic Workflow Engine

A Spring Boot application that uses AI agents to break down complex goals into tasks, execute them, and provide comprehensive summaries. Built with Java 24, Spring AI, and OpenAI integration.

## Architecture

The system uses AI agents and service components with dependency-aware parallel execution:

1. **TaskPlanAgent** - Creates task plans with dependency analysis (blocking vs informational dependencies)
2. **TaskDependencyResolver** - Coordinates task persistence and ensures dependency UUIDs are correctly mapped between planning and execution phases
3. **TaskAgent** - Executes individual tasks with context from completed tasks
4. **GoalAgent** - Summarizes the entire workflow execution and results
5. **WorkflowOrchestrator** - Coordinates parallel execution based on task dependencies
6. **DependencyResolver** - Analyzes task dependencies and enables optimal parallel execution

## Features

### 🚀 **Async Workflow Engine**
- ✅ **Instant API responses** - Get goal ID immediately, workflow runs in background
- ✅ **Real-time progress tracking** - Monitor task execution as it happens
- ✅ **Virtual thread execution** - Efficient async processing with Java 24 virtual threads
- ✅ **REST API endpoints** - Start workflows and track progress via simple HTTP calls

### 🧠 **Intelligent Task Management**
- ✅ **Dependency-aware parallel execution** - Tasks run in parallel when dependencies allow
- ✅ **Smart task planning** with blocking and informational dependencies  
- ✅ **Dynamic task planning** that adapts based on task results
- ✅ **Context-aware task execution** with completed task results
- ✅ **Circular dependency detection** prevents infinite loops

### 🏗️ **Technical Excellence**
- ✅ **PostgreSQL persistence** - Tasks persist immediately for real-time tracking
- ✅ **Immutable domain records** following Java best practices
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

The workflow engine provides an async API with instant response and real-time progress tracking.

### 1. Start Workflow Execution

**Endpoint:** `POST /api/workflow/execute`

**Request Body:**
```json
{
  "query": "Your goal or task description here"
}
```

**Response (Instant):**
```json
{
  "goalId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Workflow execution started"
}
```

The workflow executes asynchronously in virtual threads. Use the returned `goalId` to track progress.

### 2. Check Goal Status

**Endpoint:** `GET /api/workflow/goal/{goalId}`

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "query": "Your original query",
  "tasks": [
    {
      "id": "task-uuid-1",
      "description": "Task description",
      "result": "Task execution result",
      "status": "COMPLETED",
      "blockingDependencies": [],
      "informationalDependencies": [],
      "createdAt": "2024-01-01T10:00:00Z",
      "completedAt": "2024-01-01T10:05:00Z"
    }
  ],
  "summary": "AI-generated summary of the workflow execution",
  "status": "COMPLETED",
  "createdAt": "2024-01-01T10:00:00Z",
  "completedAt": "2024-01-01T10:10:00Z"
}
```

### 3. Check Task Progress

**Endpoint:** `GET /api/workflow/goal/{goalId}/tasks`

**Response:**
```json
[
  {
    "id": "task-uuid-1",
    "description": "Research coffee shop market",
    "result": "Market research completed...",
    "status": "COMPLETED",
    "blockingDependencies": [],
    "informationalDependencies": [],
    "createdAt": "2024-01-01T10:00:00Z",
    "completedAt": "2024-01-01T10:02:00Z"
  },
  {
    "id": "task-uuid-2", 
    "description": "Develop business plan",
    "result": null,
    "status": "PENDING",
    "blockingDependencies": ["task-uuid-1"],
    "informationalDependencies": [],
    "createdAt": "2024-01-01T10:00:00Z",
    "completedAt": null
  }
]
```

### Task Status Values
- `PENDING` - Task created but not yet started
- `IN_PROGRESS` - Task currently executing (internal use)
- `COMPLETED` - Task finished successfully

## Complete Workflow Example

Here's a complete example showing the async workflow process:

### Step 1: Start a Workflow
```bash
# Start the workflow
curl -X POST http://localhost:8080/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Help me start a coffee shop business including market research, business plan, permits, and initial setup"
  }'

# Response (immediate):
# {
#   "goalId": "550e8400-e29b-41d4-a716-446655440000", 
#   "message": "Workflow execution started"
# }
```

### Step 2: Track Task Progress
```bash
# Check task progress in real-time
GOAL_ID="550e8400-e29b-41d4-a716-446655440000"

curl http://localhost:8080/api/workflow/goal/$GOAL_ID/tasks

# Response shows current task status:
# [
#   {
#     "id": "task-1",
#     "description": "Conduct Market Research",
#     "status": "COMPLETED",  
#     "result": "Market research shows strong demand...",
#     "createdAt": "2024-01-01T10:00:00Z",
#     "completedAt": "2024-01-01T10:02:30Z"
#   },
#   {
#     "id": "task-2", 
#     "description": "Develop Business Plan",
#     "status": "PENDING",
#     "blockingDependencies": ["task-1"],
#     "createdAt": "2024-01-01T10:00:00Z"
#   }
# ]
```

### Step 3: Check Final Results
```bash
# Check complete goal status and summary
curl http://localhost:8080/api/workflow/goal/$GOAL_ID

# Response when workflow completes:
# {
#   "id": "550e8400-e29b-41d4-a716-446655440000",
#   "query": "Help me start a coffee shop business...",
#   "status": "COMPLETED",
#   "summary": "Successfully created comprehensive coffee shop business plan...",
#   "tasks": [...], // All completed tasks
#   "createdAt": "2024-01-01T10:00:00Z",
#   "completedAt": "2024-01-01T10:15:45Z"
# }
```

## Additional Examples

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
1. **AI Analysis**: TaskPlanAgent analyzes goals and identifies task dependencies
2. **UUID Coordination**: TaskDependencyResolver coordinates task persistence and maps planning UUIDs to database UUIDs
3. **Validation**: System checks for circular dependencies and validates relationships  
4. **Batch Execution**: Tasks with satisfied dependencies execute in parallel batches
5. **Dynamic Updates**: Plan reviews after each task may add/modify/remove tasks
6. **Continues**: Process repeats until all tasks complete

### Example: Vacation Planning
```
TASKS:
1. Research and create a daily itinerary for a 3-day trip to Paris.
2. Research and compile a list of 5 highly-rated French restaurants in Paris.
3. Research and summarize the public transportation options available in Paris.
4. Create a final, integrated travel plan document. (depends on 1, 2, 3 - blocking)

EXECUTION BATCHES:
Batch 1: [Task 1, Task 2, Task 3] (parallel - no dependencies)
Batch 2: [Task 4] (depends on all previous tasks)
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