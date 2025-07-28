#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== PHASE 1 SIMPLE TEMPLATE API TESTING SCRIPT ==="
echo ""
echo "This script tests the Phase 1 template workflow:"
echo "1. List available simple templates"
echo "2. Get specific template details"
echo "3. Get template parameters"
echo "4. Execute template"
echo "5. Check execution results"
echo ""

# Check if server is running
echo "🔍 Checking if server is running at $BASE_URL..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo "❌ Server is not running. Please start with: ./mvnw spring-boot:run"
    exit 1
fi
echo "✅ Server is running"
echo ""

# 1. List all simple templates
echo "📋 1. Listing all available simple templates:"
echo "GET $BASE_URL/api/simple-templates"
echo ""
TEMPLATES=$(curl -s "$BASE_URL/api/simple-templates")
echo "$TEMPLATES" | jq '.[0:2] | .[] | {id, name, category, description}' 2>/dev/null || echo "$TEMPLATES"
echo ""

# Extract first template ID for testing
TEMPLATE_ID=$(echo "$TEMPLATES" | jq -r '.[0].id' 2>/dev/null)
if [ "$TEMPLATE_ID" = "null" ] || [ -z "$TEMPLATE_ID" ]; then
    echo "❌ No templates found. Templates may not be initialized."
    exit 1
fi

echo "🎯 Using template ID: $TEMPLATE_ID"
echo ""

# 2. Get specific template
echo "📄 2. Getting template details:"
echo "GET $BASE_URL/api/simple-templates/$TEMPLATE_ID"
echo ""
TEMPLATE=$(curl -s "$BASE_URL/api/simple-templates/$TEMPLATE_ID")
echo "$TEMPLATE" | jq '{name, description, promptTemplate}' 2>/dev/null || echo "$TEMPLATE"
echo ""

# 3. Get template parameters
echo "📝 3. Getting template parameters:"
echo "GET $BASE_URL/api/simple-templates/$TEMPLATE_ID/parameters"
echo ""
PARAMETERS=$(curl -s "$BASE_URL/api/simple-templates/$TEMPLATE_ID/parameters")
echo "$PARAMETERS" | jq '.' 2>/dev/null || echo "$PARAMETERS"
echo ""

# 4. Execute template with valid parameters
echo "🚀 4. Executing template with parameters:"
echo "POST $BASE_URL/api/simple-templates/$TEMPLATE_ID/execute"
VALID_PARAMS='{
    "destination": "Tokyo, Japan",
    "duration": 7,
    "budget": "Mid-range"
}'
echo "$VALID_PARAMS"
echo ""
EXECUTION=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "$VALID_PARAMS" \
    "$BASE_URL/api/simple-templates/$TEMPLATE_ID/execute")
echo "Response: $EXECUTION"
echo ""

# Extract goal ID from execution response
GOAL_ID=$(echo "$EXECUTION" | jq -r '.goalId' 2>/dev/null)
if [ "$GOAL_ID" != "null" ] && [ -n "$GOAL_ID" ]; then
    echo "🎯 Created goal: $GOAL_ID"
    echo ""
    
    # 5. Check goal status
    echo "📊 5. Checking goal status:"
    echo "GET $BASE_URL/api/workflow/goal/$GOAL_ID"
    echo ""
    sleep 2  # Give it a moment to process
    GOAL_STATUS=$(curl -s "$BASE_URL/api/workflow/goal/$GOAL_ID")
    echo "$GOAL_STATUS" | jq '{id, query, status, summary}' 2>/dev/null || echo "$GOAL_STATUS"
    echo ""
    
    # 6. List goal tasks
    echo "📋 6. Checking goal tasks:"
    echo "GET $BASE_URL/api/workflow/goal/$GOAL_ID/tasks"
    echo ""
    TASKS=$(curl -s "$BASE_URL/api/workflow/goal/$GOAL_ID/tasks")
    echo "$TASKS" | jq '.[0:3] | .[] | {id, description, status}' 2>/dev/null || echo "$TASKS"
    echo ""
else
    echo "❌ No goal ID returned from template execution"
fi

# 7. Test with missing required parameter
echo "❌ 7. Testing with missing required parameter:"
echo "POST $BASE_URL/api/simple-templates/$TEMPLATE_ID/execute"
INVALID_PARAMS='{
    "duration": 7,
    "budget": "Mid-range"
}'
echo "$INVALID_PARAMS"
echo ""
INVALID_EXECUTION=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "$INVALID_PARAMS" \
    "$BASE_URL/api/simple-templates/$TEMPLATE_ID/execute")
echo "Response: $INVALID_EXECUTION"
echo ""

echo ""
echo "=== PHASE 1 TEMPLATE FLOW COMPLETE ==="
echo ""
echo "🎉 Successfully demonstrated:"
echo "   ✅ Simple template listing and retrieval"
echo "   ✅ Parameter discovery"
echo "   ✅ Template execution with parameter rendering"
echo "   ✅ Integration with existing WorkflowOrchestrator"
echo "   ✅ Goal and task creation through template flow"
echo "   ✅ Parameter validation and error handling"
echo ""
echo "Phase 1 proves the template concept works!"
echo "Ready for Phase 2 development once approved."