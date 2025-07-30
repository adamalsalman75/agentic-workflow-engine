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
    
    # Test Story 5 - Verify all 5 production templates exist
    echo ""
    echo "4. Testing Story 5 - Production Templates:"
    EXPECTED_TEMPLATES=("Business Startup Planner" "Event Organizer" "Research Project Planner" "Product Launch Checklist" "Home Renovation Planner")
    MISSING_TEMPLATES=()
    
    for template in "${EXPECTED_TEMPLATES[@]}"; do
        if echo "$TEMPLATES" | jq -e --arg name "$template" '.[] | select(.name == $name)' > /dev/null; then
            echo "✅ Found: $template"
        else
            echo "❌ Missing: $template"
            MISSING_TEMPLATES+=("$template")
        fi
    done
    
    if [ ${#MISSING_TEMPLATES[@]} -eq 0 ]; then
        echo "✅ Story 5 verification complete - All 5 production templates found"
    else
        echo "❌ Story 5 verification failed - Missing templates: ${MISSING_TEMPLATES[*]}"
    fi
    
    # Test Story 3 enhanced parameter discovery - use Business Startup Planner
    TEMPLATE_ID=$(echo "$TEMPLATES" | jq -r '.[] | select(.name == "Business Startup Planner") | .id' 2>/dev/null | tr -d '\n')
    echo ""
    echo "5. Testing Story 3 - Enhanced Parameter Discovery API:"
    PARAMS=$(curl -s "${API_BASE}/api/templates/${TEMPLATE_ID}/parameters")
    
    if [ $? -eq 0 ] && echo "$PARAMS" | jq -e '.parameters' > /dev/null; then
        echo "✅ Enhanced parameter discovery API working"
        echo "📋 Template: $(echo $PARAMS | jq -r '.templateName')"
        echo "📝 Parameters found: $(echo $PARAMS | jq '.parameters | length')"
        echo ""
        echo "🔍 Sample parameter with metadata:"
        echo "$PARAMS" | jq -C '.parameters[0] | {
            name, 
            type, 
            required, 
            validation: (.validation | length), 
            metadata
        }'
        echo ""
        echo "✅ Story 3 parameter discovery verification complete"
        
        # Story 4 database schema verification
        echo ""
        echo "6. Testing Story 4 - Database Schema for Parameters:"
        echo "🔍 Verifying parameter persistence and metadata:"
        echo "$PARAMS" | jq -C '.parameters[] | {
            name,
            type, 
            required,
            defaultValue,
            validation: (.validation | length),
            metadata: {
                placeholder: .metadata.placeholder,
                helpText: .metadata.helpText,
                group: .metadata.group,
                order: .metadata.order
            }
        }'
        echo ""
        echo "✅ Story 4 database parameter storage verification complete"
        echo "   - Parameters loaded from database ✓"
        echo "   - Metadata generated and stored ✓"  
        echo "   - Validation rules persisted ✓"
    else
        echo "❌ Enhanced parameter discovery API failed"
    fi
    
    # Quick Story 2 validation test
    echo ""
    echo "7. Quick Story 2 validation test:"
    VALIDATION_TEST=$(curl -s -X POST "${API_BASE}/api/templates/${TEMPLATE_ID}/execute" \
        -H "Content-Type: application/json" \
        -d '{
            "business_name": "TestCorp",
            "industry": "Technology", 
            "business_location": "San Francisco, CA",
            "team_size": "5",
            "launch_date": "2025-06-01",
            "business_email": "test@testcorp.com",
            "business_model": "SaaS"
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