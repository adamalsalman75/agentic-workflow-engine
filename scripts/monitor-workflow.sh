#!/bin/bash
# Monitor a specific workflow in real-time
# Usage: ./monitor-workflow.sh <goal-id>
# Example: ./monitor-workflow.sh 550e8400-e29b-41d4-a716-446655440000

set -e

GOAL_ID="$1"
if [ -z "$GOAL_ID" ]; then
    echo "❌ Error: Goal ID required"
    echo "Usage: $0 <goal-id>"
    echo "Example: $0 550e8400-e29b-41d4-a716-446655440000"
    exit 1
fi

# Get the public IP
export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)

if [ -z "$PUBLIC_IP" ]; then
    echo "❌ Error: Could not get public IP. Is the cluster running?"
    exit 1
fi

echo "🔍 Monitoring Workflow: $GOAL_ID"
echo "📍 API Base URL: http://$PUBLIC_IP"
echo "Press Ctrl+C to stop monitoring"
echo ""

# Function to show workflow status
show_status() {
    echo "📊 Goal Status ($(date)):"
    curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '{
        status: .status,
        query: .query,
        createdAt: .createdAt,
        completedAt: .completedAt
    }' 2>/dev/null || echo "❌ Failed to fetch goal status"
    
    echo ""
    echo "📋 Task Summary:"
    curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq -r '
        group_by(.status) | 
        map({status: .[0].status, count: length}) | 
        .[] | "\(.status): \(.count)"
    ' 2>/dev/null || echo "❌ Failed to fetch task summary"
    
    echo ""
    echo "🔄 Recent Task Completions:"
    curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq -r '
        map(select(.status == "COMPLETED")) |
        sort_by(.completedAt) |
        reverse |
        .[0:3] |
        .[] |
        "✅ \(.description) (completed: \(.completedAt // "unknown"))"
    ' 2>/dev/null || echo "❌ Failed to fetch completed tasks"
    
    echo ""
    echo "⏳ Pending Tasks:"
    curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq -r '
        map(select(.status == "PENDING")) |
        .[0:3] |
        .[] |
        "⏸️  \(.description)"
    ' 2>/dev/null || echo "❌ Failed to fetch pending tasks"
    
    echo ""
    echo "💰 Token Usage:"
    curl -s http://$PUBLIC_IP/actuator/metrics/gen_ai.client.token.usage | jq -r '
        .measurements[] | 
        select(.statistic == "TOTAL") | 
        "🪙 Total Tokens: \(.value)"
    ' 2>/dev/null || echo "❌ Failed to fetch token usage"
    
    echo ""
    echo "----------------------------------------"
}

# Check if workflow exists
if ! curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq -e . &>/dev/null; then
    echo "❌ Error: Workflow $GOAL_ID not found"
    exit 1
fi

# Monitor loop
while true; do
    clear
    show_status
    
    # Check if completed
    STATUS=$(curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq -r '.status' 2>/dev/null)
    if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "FAILED" ]; then
        echo "🏁 Workflow finished with status: $STATUS"
        echo ""
        echo "📋 Final Summary:"
        curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '.summary' 2>/dev/null
        break
    fi
    
    sleep 5
done