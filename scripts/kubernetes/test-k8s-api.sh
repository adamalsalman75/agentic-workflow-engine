#!/bin/bash

# Test API endpoints against Kubernetes deployment

# Configuration - Update these based on your K8s setup
NAMESPACE="${NAMESPACE:-default}"
SERVICE_NAME="${SERVICE_NAME:-agentic-workflow-engine}"
INGRESS_HOST="${INGRESS_HOST:-}"

echo "=== Kubernetes API Test Script ==="
echo ""

# Determine API base URL
if [ -n "$INGRESS_HOST" ]; then
    API_BASE="https://${INGRESS_HOST}"
    echo "🌐 Using Ingress: $API_BASE"
elif kubectl get service $SERVICE_NAME -n $NAMESPACE > /dev/null 2>&1; then
    # Use port-forward for testing
    echo "🔗 Setting up port-forward to service $SERVICE_NAME..."
    kubectl port-forward -n $NAMESPACE service/$SERVICE_NAME 8080:8080 &
    PORT_FORWARD_PID=$!
    sleep 5
    API_BASE="http://localhost:8080"
    echo "✅ Port-forward established: $API_BASE"
else
    echo "❌ Cannot find service '$SERVICE_NAME' in namespace '$NAMESPACE'"
    echo "   Available services:"
    kubectl get services -n $NAMESPACE
    exit 1
fi

# Cleanup function
cleanup() {
    if [ -n "$PORT_FORWARD_PID" ]; then
        echo "🧹 Cleaning up port-forward..."
        kill $PORT_FORWARD_PID 2>/dev/null
    fi
}
trap cleanup EXIT

echo ""

# Health check
echo "1. Health check:"
if curl -s --max-time 10 "${API_BASE}/actuator/health" | jq -e '.status == "UP"' > /dev/null; then
    echo "✅ Application is healthy"
else
    echo "❌ Application is not healthy or not accessible"
    exit 1
fi
echo ""

# Test template system in Kubernetes
echo "2. Testing template system:"
TEMPLATES=$(curl -s --max-time 10 "${API_BASE}/api/simple-templates")
if [ $? -eq 0 ] && [ "$(echo $TEMPLATES | jq length)" -gt 0 ]; then
    echo "✅ Template system is working in Kubernetes"
    echo "Available templates: $(echo $TEMPLATES | jq -r '.[].name' | paste -sd, -)"
    
    # Test template execution
    TEMPLATE_ID=$(echo $TEMPLATES | jq -r '.[0].id')
    echo ""
    echo "3. Testing template execution with ID: $TEMPLATE_ID"
    EXECUTION_RESULT=$(curl -s --max-time 30 -X POST "${API_BASE}/api/simple-templates/${TEMPLATE_ID}/execute" \
        -H "Content-Type: application/json" \
        -d '{
            "destination": "Tokyo, Japan",
            "startDate": "2024-12-01",
            "duration": "5",
            "budget": "2000 USD",
            "travelStyle": "Mid-range"
        }')
    
    if echo $EXECUTION_RESULT | jq -e '.goalId' > /dev/null; then
        echo "✅ Template execution successful"
        GOAL_ID=$(echo $EXECUTION_RESULT | jq -r '.goalId')
        echo "   Goal ID: $GOAL_ID"
    else
        echo "❌ Template execution failed"
        echo "   Response: $(echo $EXECUTION_RESULT | jq -C '.')"
    fi
else
    echo "❌ Template system failed or no templates available"
fi
echo ""

echo "=== Kubernetes API test completed ==="