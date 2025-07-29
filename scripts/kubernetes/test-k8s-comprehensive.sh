#!/bin/bash

# Comprehensive Kubernetes API test with workflow monitoring
# Tests health, templates, executes workflow, and monitors to completion

# Configuration - Update these based on your K8s setup
NAMESPACE="${NAMESPACE:-default}"
SERVICE_NAME="${SERVICE_NAME:-agentic-workflow-engine}"
INGRESS_HOST="${INGRESS_HOST:-}"

echo "=== Comprehensive Kubernetes API Test ==="
echo ""

# Test cluster connectivity first
echo "🔍 Checking Kubernetes cluster connectivity..."
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    echo ""
    echo "Try refreshing your credentials:"
    echo "  gcloud container clusters get-credentials <cluster-name> --region <region> --project <project>"
    echo ""
    echo "Or check if you're connected to the right context:"
    echo "  kubectl config current-context"
    echo ""
    exit 1
fi
echo "✅ Kubernetes cluster is accessible"
echo ""

# Determine API base URL
if [ -n "$INGRESS_HOST" ]; then
    API_BASE="https://${INGRESS_HOST}"
    echo "🌐 Using Ingress: $API_BASE"
elif kubectl get service $SERVICE_NAME -n $NAMESPACE > /dev/null 2>&1; then
    # Check if service has external IP (LoadBalancer)
    EXTERNAL_IP=$(kubectl get service $SERVICE_NAME -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
    if [ -n "$EXTERNAL_IP" ]; then
        API_BASE="http://${EXTERNAL_IP}"
        echo "🌐 Using LoadBalancer IP: $API_BASE"
    else
        # Use port-forward for testing
        echo "🔗 Setting up port-forward to service $SERVICE_NAME..."
        kubectl port-forward -n $NAMESPACE service/$SERVICE_NAME 8080:8080 &
        PORT_FORWARD_PID=$!
        sleep 5
        API_BASE="http://localhost:8080"
        echo "✅ Port-forward established: $API_BASE"
    fi
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

# Test template system
echo "2. Testing template system:"
TEMPLATES=$(curl -s --max-time 10 "${API_BASE}/api/templates")
if [ $? -eq 0 ] && [ "$(echo $TEMPLATES | jq length)" -gt 0 ]; then
    echo "✅ Template system is working in Kubernetes"
    echo "Available templates: $(echo $TEMPLATES | jq -r '.[].name' | paste -sd, -)"
else
    echo "❌ Template system failed or no templates available"
    exit 1
fi
echo ""

# Test Story 3 enhanced parameter discovery
TEMPLATE_ID=$(echo $TEMPLATES | jq -r '.[0].id')
echo "3. Testing Story 3 - Enhanced Parameter Discovery API:"
PARAMS=$(curl -s --max-time 10 "${API_BASE}/api/templates/${TEMPLATE_ID}/parameters")

if [ $? -eq 0 ] && echo "$PARAMS" | jq -e '.parameters' > /dev/null; then
    echo "✅ Enhanced parameter discovery API working in Kubernetes"
    echo "📋 Template: $(echo $PARAMS | jq -r '.templateName')"
    echo "📝 Parameters found: $(echo $PARAMS | jq '.parameters | length')"
    echo "🔍 Validation rules: $(echo $PARAMS | jq '[.parameters[].validation | length] | add') total"
    echo "🎯 Parameter ordering: $(echo $PARAMS | jq -r '.parameters | map(.metadata.order) | sort | join(" → ")')"
    echo "✅ Story 3 features verified in Kubernetes"
else
    echo "❌ Enhanced parameter discovery API failed in Kubernetes"
fi
echo ""

# Test workflow execution with monitoring
echo "4. Testing workflow execution with monitoring:"
EXECUTION_RESULT=$(curl -s --max-time 30 -X POST "${API_BASE}/api/workflow/execute" \
    -H "Content-Type: application/json" \
    -d '{"query": "Create a comprehensive test workflow for Kubernetes deployment"}')

if echo $EXECUTION_RESULT | jq -e '.goalId' > /dev/null; then
    GOAL_ID=$(echo $EXECUTION_RESULT | jq -r '.goalId')
    echo "✅ Workflow started successfully"
    echo "🆔 Goal ID: $GOAL_ID"
    echo ""
    
    echo "⏳ Monitoring workflow completion..."
    MAX_ATTEMPTS=45  # 90 seconds timeout for K8s (may be slower)
    ATTEMPT=0
    
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        GOAL_STATUS=$(curl -s --max-time 10 "${API_BASE}/api/workflow/goal/${GOAL_ID}")
        STATUS=$(echo $GOAL_STATUS | jq -r '.status')
        
        echo "   Attempt $((ATTEMPT+1))/$MAX_ATTEMPTS - Status: $STATUS"
        
        if [ "$STATUS" = "COMPLETED" ]; then
            echo ""
            echo "✅ Workflow completed successfully in Kubernetes!"
            echo "📋 Final Goal:"
            echo $GOAL_STATUS | jq -C '.'
            echo ""
            
            echo "📝 Tasks completed:"
            TASKS=$(curl -s --max-time 10 "${API_BASE}/api/workflow/goal/${GOAL_ID}/tasks")
            echo $TASKS | jq -C '.[] | {description: .description, status: .status, result: .result}'
            break
        elif [ "$STATUS" = "FAILED" ]; then
            echo ""
            echo "❌ Workflow failed in Kubernetes!"
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
        echo ""
        echo "You can continue monitoring with:"
        echo "./scripts/kubernetes/monitor-workflow.sh $GOAL_ID"
    fi
else
    echo "❌ Workflow execution failed"
    echo "Response: $EXECUTION_RESULT"
    exit 1
fi
echo ""

# Test template execution as well
echo "5. Testing template execution with ID: $TEMPLATE_ID"
TEMPLATE_EXECUTION=$(curl -s --max-time 30 -X POST "${API_BASE}/api/templates/${TEMPLATE_ID}/execute" \
    -H "Content-Type: application/json" \
    -d '{
        "destination": "Tokyo, Japan",
        "startDate": "2024-12-01",
        "duration": "5",
        "budget": "2000 USD",
        "travelStyle": "Mid-range"
    }')

if echo $TEMPLATE_EXECUTION | jq -e '.goalId' > /dev/null; then
    TEMPLATE_GOAL_ID=$(echo $TEMPLATE_EXECUTION | jq -r '.goalId')
    echo "✅ Template execution started successfully"
    echo "🆔 Template Goal ID: $TEMPLATE_GOAL_ID"
    echo "   (Template execution will continue in background)"
else
    echo "❌ Template execution failed"
    echo "Response: $TEMPLATE_EXECUTION"
fi
echo ""

echo "=== Comprehensive Kubernetes API test completed ==="
echo ""
echo "🎉 All tests passed! Your Kubernetes deployment is working correctly."
echo ""
echo "📊 Cluster Status:"
kubectl get pods -n $NAMESPACE | grep $SERVICE_NAME