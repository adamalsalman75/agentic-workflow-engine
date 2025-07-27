#!/bin/bash
# Test workflow execution script
# Usage: ./test-workflow.sh "Your workflow query here"
# Example: ./test-workflow.sh "Plan a weekend trip to the mountains"

set -e

# Get the public IP
export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)

if [ -z "$PUBLIC_IP" ]; then
    echo "❌ Error: Could not get public IP. Is the cluster running?"
    echo "Run: kubectl get services"
    exit 1
fi

# Get query from command line or use default
QUERY="$1"
if [ -z "$QUERY" ]; then
    QUERY="Plan a quick lunch break"
fi

echo "🚀 Starting workflow: $QUERY"
echo "📍 API Base URL: http://$PUBLIC_IP"
echo ""

# Start the workflow and capture goal ID
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"$QUERY\"}" | jq -r '.goalId')

if [ "$GOAL_ID" = "null" ] || [ -z "$GOAL_ID" ]; then
    echo "❌ Error: Failed to start workflow"
    echo "Check if the API is responding:"
    echo "curl http://$PUBLIC_IP/actuator/health"
    exit 1
fi

echo "✅ Workflow started successfully!"
echo "🆔 Goal ID: $GOAL_ID"
echo "🔗 Goal URL: http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID"
echo "📋 Tasks URL: http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks"
echo ""

# Wait a moment for initial processing
echo "⏳ Waiting for initial processing..."
sleep 5

# Show current status
echo "📊 Current Status:"
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '{
    id: .id,
    query: .query,
    status: .status,
    createdAt: .createdAt,
    completedAt: .completedAt
}'

echo ""
echo "💡 To monitor progress, run:"
echo "   watch -n 3 \"curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '.status'\""
echo ""
echo "📝 To view tasks, run:"
echo "   curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq"
echo ""
echo "🔍 To monitor token usage:"
echo "   curl -s http://$PUBLIC_IP/actuator/metrics/gen_ai.client.token.usage | jq"