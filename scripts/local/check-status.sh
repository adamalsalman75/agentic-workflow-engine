#!/bin/bash
# Check the overall status of the agentic workflow engine
# Usage: ./check-status.sh

set -e

echo "🔍 Agentic Workflow Engine Status Check"
echo "====================================="
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info &>/dev/null; then
    echo "❌ Error: kubectl not configured or cluster not accessible"
    echo "Run: gcloud container clusters get-credentials agentic-workflow-cluster --zone us-west1 --project adam-466814"
    exit 1
fi

echo "☸️  Cluster Status:"
kubectl get pods
echo ""

echo "🌐 Service Status:"
kubectl get services
echo ""

# Get public IP
export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)

if [ -z "$PUBLIC_IP" ]; then
    echo "⚠️  Warning: No external IP available yet"
    echo "LoadBalancer may still be provisioning..."
    exit 0
fi

echo "🌍 Public IP: $PUBLIC_IP"
echo "🔗 API Base URL: http://$PUBLIC_IP"
echo ""

echo "❤️  API Health Check:"
HEALTH_STATUS=$(curl -s http://$PUBLIC_IP/actuator/health | jq -r '.status' 2>/dev/null || echo "UNREACHABLE")

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✅ API is healthy and responding"
    curl -s http://$PUBLIC_IP/actuator/health | jq '.components | to_entries[] | {component: .key, status: .value.status}'
else
    echo "❌ API is not responding or unhealthy"
    echo "Status: $HEALTH_STATUS"
fi

echo ""
echo "📊 Quick Stats:"
echo "   Pods Running: $(kubectl get pods --no-headers | grep Running | wc -l)"
echo "   Services: $(kubectl get services --no-headers | wc -l)"

if [ "$HEALTH_STATUS" = "UP" ]; then
    echo ""
    echo "🧪 Ready to test! Run:"
    echo "   ./test-workflow.sh \"Your workflow query here\""
fi