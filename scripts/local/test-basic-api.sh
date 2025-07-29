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

# Test basic workflow endpoint
echo "2. Testing basic workflow endpoint:"
RESPONSE=$(curl -s -X POST "${API_BASE}/api/workflow/execute" \
    -H "Content-Type: application/json" \
    -d '{"query": "Create a simple test task"}')

if [ $? -eq 0 ]; then
    echo "✅ Workflow endpoint is responding"
    echo "Response: $(echo $RESPONSE | jq -C '.')"
else
    echo "❌ Workflow endpoint failed"
fi
echo ""

# Test template system
echo "3. Testing template system:"
TEMPLATES=$(curl -s "${API_BASE}/api/simple-templates")
if [ $? -eq 0 ] && [ "$(echo $TEMPLATES | jq length)" -gt 0 ]; then
    echo "✅ Template system is working"
    echo "Available templates: $(echo $TEMPLATES | jq -r '.[].name' | paste -sd, -)"
else
    echo "❌ Template system failed or no templates available"
fi
echo ""

echo "=== Basic API test completed ==="