#!/bin/bash

# Basic API health check and simple workflow test

API_BASE="http://localhost:8080"

echo "=== Basic API Test Script ==="
echo ""

# Health check
echo "1. Health check:"
if curl -s "${API_BASE}/actuator/health" | jq -e '.status == "UP"' > /dev/null; then
    echo "✅ Application is healthy"
else
    echo "❌ Application is not healthy or not running"
    echo "   Make sure to run: ./scripts/local/start-local.sh"
    exit 1
fi
echo ""

# Test basic workflow endpoint with polling
echo "2. Testing basic workflow endpoint:"
RESPONSE=$(curl -s -X POST "${API_BASE}/api/workflow/execute" \
    -H "Content-Type: application/json" \
    -d '{"query": "Create a simple test task"}')

if [ $? -eq 0 ] && echo "$RESPONSE" | jq -e '.goalId' > /dev/null; then
    GOAL_ID=$(echo $RESPONSE | jq -r '.goalId')
    echo "✅ Workflow started successfully"
    echo "🆔 Goal ID: $GOAL_ID"
    echo ""
    
    echo "⏳ Waiting for workflow completion..."
    MAX_ATTEMPTS=60  # Increased to 120 seconds for AI workflows
    ATTEMPT=0
    
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        GOAL_STATUS=$(curl -s "${API_BASE}/api/workflow/goal/${GOAL_ID}")
        STATUS=$(echo $GOAL_STATUS | jq -r '.status')
        
        echo "   Attempt $((ATTEMPT+1))/$MAX_ATTEMPTS - Status: $STATUS"
        
        if [ "$STATUS" = "COMPLETED" ]; then
            echo ""
            echo "✅ Workflow completed successfully!"
            echo "📋 Final Goal:"
            echo $GOAL_STATUS | jq -C '.'
            echo ""
            
            echo "📝 Tasks completed:"
            TASKS=$(curl -s "${API_BASE}/api/workflow/goal/${GOAL_ID}/tasks")
            echo $TASKS | jq -C '.[] | {description: .description, status: .status, result: .result}'
            break
        elif [ "$STATUS" = "FAILED" ]; then
            echo ""
            echo "❌ Workflow failed!"
            echo "📋 Goal details:"
            echo $GOAL_STATUS | jq -C '.'
            break
        elif [ "$STATUS" = "null" ]; then
            echo "❌ Could not retrieve goal status"
            break
        fi
        
        ATTEMPT=$((ATTEMPT+1))
        sleep 2
    done
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        echo ""
        echo "⚠️  Workflow did not complete within $((MAX_ATTEMPTS*2)) seconds"
        echo "📋 Current status:"
        echo $GOAL_STATUS | jq -C '.'
    fi
else
    echo "❌ Workflow endpoint failed or returned invalid response"
    echo "Response: $RESPONSE"
fi
echo ""

# Test template system
echo "3. Testing template system:"
TEMPLATES=$(curl -s "${API_BASE}/api/templates")
if [ $? -eq 0 ] && [ "$(echo $TEMPLATES | jq length)" -gt 0 ]; then
    echo "✅ Template system is working"
    echo "Available templates: $(echo $TEMPLATES | jq -r '.[].name' | paste -sd, -)"
    
    # Test Story 3 enhanced parameter discovery
    TEMPLATE_ID=$(echo $TEMPLATES | jq -r '.[0].id')
    echo ""
    echo "4. Testing Story 3 - Enhanced Parameter Discovery API:"
    PARAMS=$(curl -s "${API_BASE}/api/templates/${TEMPLATE_ID}/parameters")
    
    if [ $? -eq 0 ] && echo "$PARAMS" | jq -e '.parameters' > /dev/null; then
        echo "✅ Enhanced parameter discovery API working"
        echo "📋 Template: $(echo $PARAMS | jq -r '.templateName')"
        echo "📝 Parameters found: $(echo $PARAMS | jq '.parameters | length')"
        echo ""
        echo "🔍 Sample parameter with metadata:"
        echo $PARAMS | jq -C '.parameters[0] | {
            name, 
            type, 
            required, 
            validation: (.validation | length), 
            metadata: {placeholder, helpText, order, group}
        }'
        echo ""
        echo "✅ Story 3 parameter discovery verification complete"
    else
        echo "❌ Enhanced parameter discovery API failed"
    fi
    
    # Quick Story 2 validation test
    echo ""
    echo "5. Quick Story 2 validation test:"
    VALIDATION_TEST=$(curl -s -X POST "${API_BASE}/api/templates/${TEMPLATE_ID}/execute" \
        -H "Content-Type: application/json" \
        -d '{
            "destination": "Test City",
            "startDate": "2023-01-01",
            "duration": "1000"
        }')
    
    if echo "$VALIDATION_TEST" | jq -e '.success == false' > /dev/null; then
        echo "✅ Story 2 validation is working (rejected invalid input)"
        echo "   Error: $(echo $VALIDATION_TEST | jq -r '.message' | head -c 100)..."
    else
        echo "⚠️  Story 2 validation may not be working (accepted invalid input)"
        echo "   Response: $(echo $VALIDATION_TEST | jq -C '.')"
    fi
else
    echo "❌ Template system failed or no templates available"
fi
echo ""

echo "=== Basic API test completed ==="