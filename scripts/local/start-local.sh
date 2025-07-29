#!/bin/bash

# Start local Spring Boot application for testing

echo "=== Starting Agentic Workflow Engine Locally ==="
echo ""

# Check if PostgreSQL is running
if ! pgrep -x "postgres" > /dev/null; then
    echo "❌ PostgreSQL is not running. Please start PostgreSQL first:"
    echo "   brew services start postgresql@14"
    echo "   # or your preferred PostgreSQL startup method"
    exit 1
fi

# Check if database exists
if ! psql -h localhost -U postgres -lqt | cut -d \| -f 1 | grep -qw agentic_workflow; then
    echo "❌ Database 'agentic_workflow' does not exist. Creating it..."
    createdb -h localhost -U postgres agentic_workflow
    echo "✅ Database created successfully"
fi

echo "✅ PostgreSQL is running and database exists"
echo ""

# Set environment variables
export DATABASE_URL="jdbc:postgresql://localhost:5432/agentic_workflow"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="password"

if [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  OPENAI_API_KEY environment variable not set"
    echo "   Please set it: export OPENAI_API_KEY=your-api-key"
    echo ""
fi

echo "🚀 Starting Spring Boot application..."
echo "   Access at: http://localhost:8080"
echo "   Health check: http://localhost:8080/actuator/health"
echo "   API docs: Check README.md for endpoints"
echo ""

# Start the application
cd /Users/adam/projects/agentic-workflow-engine
exec ./mvnw spring-boot:run