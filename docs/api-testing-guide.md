# API Testing Guide for Agentic Workflow Engine

## Overview

This guide provides step-by-step instructions for testing the Agentic Workflow Engine API deployed on GKE. It includes environment setup, common API calls, and troubleshooting tips.

## Prerequisites

- `kubectl` configured with cluster access
- `curl` command-line tool
- `jq` for JSON formatting (install with `brew install jq` on macOS or `apt-get install jq` on Ubuntu)

## Environment Setup

### 1. Get GKE Cluster Credentials

```bash
gcloud container clusters get-credentials agentic-workflow-cluster --zone us-west1 --project adam-466814
```

### 2. Export Public IP

Get the LoadBalancer external IP and set it as an environment variable:

```bash
# Get the external IP
export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Verify the IP is set
echo "API Base URL: http://$PUBLIC_IP"
```

### 3. Verify Service Status

```bash
# Check all services
kubectl get services

# Check pod status
kubectl get pods

# Check deployment status
kubectl get deployments
```

## API Testing

### Health Check

Test if the application is running and healthy:

```bash
# Basic health check
curl http://$PUBLIC_IP/actuator/health | jq

# Expected response:
# {
#   "status": "UP",
#   "groups": ["liveness", "readiness"],
#   "components": {
#     "db": {"status": "UP"},
#     "diskSpace": {"status": "UP"},
#     "livenessState": {"status": "UP"},
#     "ping": {"status": "UP"},
#     "readinessState": {"status": "UP"},
#     "ssl": {"status": "UP"}
#   }
# }
```

### Workflow Execution

#### Start a New Workflow

```bash
# Start a workflow and capture the goal ID
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Plan a weekend trip to San Francisco including hotels, restaurants, and activities"}' | jq -r '.goalId')

# Display the goal ID
echo "Goal ID: $GOAL_ID"

# View the full response with formatting
curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Plan a weekend trip to San Francisco including hotels, restaurants, and activities"}' | jq

# Expected response:
# {
#   "goalId": "550e8400-e29b-41d4-a716-446655440000",
#   "message": "Workflow execution started"
# }
```

#### Check Goal Status

```bash
# Check the current status of the goal
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq

# Expected response (while processing):
# {
#   "id": "550e8400-e29b-41d4-a716-446655440000",
#   "query": "Plan a weekend trip to San Francisco...",
#   "summary": null,
#   "status": "PLANNING",
#   "createdAt": "2025-07-27T10:00:00Z",
#   "completedAt": null
# }

# Expected response (when completed):
# {
#   "id": "550e8400-e29b-41d4-a716-446655440000",
#   "query": "Plan a weekend trip to San Francisco...",
#   "summary": "Comprehensive weekend trip plan created...",
#   "status": "COMPLETED",
#   "createdAt": "2025-07-27T10:00:00Z",
#   "completedAt": "2025-07-27T10:05:30Z"
# }
```

#### View Task Details

```bash
# Get detailed task breakdown
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq

# For better readability, view specific task fields
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq '.[] | {id, description, status, completedAt}'

# View task dependencies
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq '.[] | {description, status, blockingDependencies, informationalDependencies}'
```

## Example Workflow Scenarios

### 1. Simple Task Planning

```bash
# Coffee break planning
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Plan a 15-minute coffee break"}' | jq -r '.goalId')

echo "Coffee break goal: $GOAL_ID"
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq
```

### 2. Complex Project Planning

```bash
# Business project planning
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Help me start a small online bookstore business including market research, website setup, inventory management, and marketing strategy"}' | jq -r '.goalId')

echo "Business project goal: $GOAL_ID"

# Monitor progress (run multiple times to see status changes)
watch -n 5 "curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '.status'"
```

### 3. Travel Planning

```bash
# Detailed travel planning
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Plan a 5-day trip to Tokyo including flights, hotels, daily itineraries, restaurant recommendations, and cultural activities"}' | jq -r '.goalId')

echo "Travel planning goal: $GOAL_ID"

# Wait for completion and view summary
sleep 30
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq '.summary'
```

## Monitoring and Debugging

### Real-time Task Monitoring

```bash
# Monitor task completion in real-time
watch -n 3 "curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq '.[] | {description: .description, status: .status}'"

# Count completed vs pending tasks
curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID/tasks | jq 'group_by(.status) | map({status: .[0].status, count: length})'
```

### Application Logs

```bash
# View application logs
kubectl logs -f deployment/agentic-workflow-engine

# View PostgreSQL logs
kubectl logs -f deployment/postgres

# View recent events
kubectl get events --sort-by=.metadata.creationTimestamp
```

### Performance Metrics

```bash
# Application metrics
curl -s http://$PUBLIC_IP/actuator/metrics | jq

# Specific metrics
curl -s http://$PUBLIC_IP/actuator/metrics/jvm.memory.used | jq
curl -s http://$PUBLIC_IP/actuator/metrics/http.server.requests | jq
curl -s http://$PUBLIC_IP/actuator/metrics/gen_ai.client.token.usage | jq
```

## Utility Scripts

### Quick Status Check

```bash
#!/bin/bash
# Save as check-status.sh

export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

echo "=== Cluster Status ==="
kubectl get pods
echo ""

echo "=== Service Status ==="
kubectl get services
echo ""

echo "=== API Health ==="
curl -s http://$PUBLIC_IP/actuator/health | jq '.status'
echo ""

echo "=== Public IP ==="
echo "API Base URL: http://$PUBLIC_IP"
```

### Workflow Testing Script

```bash
#!/bin/bash
# Save as test-workflow.sh

export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

QUERY="$1"
if [ -z "$QUERY" ]; then
    QUERY="Plan a quick lunch break"
fi

echo "Starting workflow: $QUERY"
export GOAL_ID=$(curl -s -X POST http://$PUBLIC_IP/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"$QUERY\"}" | jq -r '.goalId')

echo "Goal ID: $GOAL_ID"
echo "Monitor at: http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID"

# Wait and show final result
echo "Waiting for completion..."
sleep 10

curl -s http://$PUBLIC_IP/api/workflow/goal/$GOAL_ID | jq
```

## Troubleshooting

### Common Issues

#### 1. Public IP Not Available

```bash
# Check if LoadBalancer is still pending
kubectl get svc agentic-workflow-engine

# If EXTERNAL-IP shows <pending>, wait a few minutes
# For troubleshooting, check events:
kubectl describe svc agentic-workflow-engine
```

#### 2. Application Not Responding

```bash
# Check pod status
kubectl get pods

# Check logs for errors
kubectl logs deployment/agentic-workflow-engine --tail=50

# Check if database is ready
kubectl logs deployment/postgres --tail=20
```

#### 3. Database Connection Issues

```bash
# Test database connectivity from app pod
kubectl exec -it deployment/agentic-workflow-engine -- sh
# Inside pod: nc -zv postgres 5432

# Check database logs
kubectl logs deployment/postgres
```

#### 4. Reset Environment

```bash
# Delete and recreate deployments
kubectl delete deployment agentic-workflow-engine postgres
kubectl apply -f kubernetes/deployment.yaml

# Wait for pods to be ready
kubectl wait --for=condition=available --timeout=300s deployment/agentic-workflow-engine
kubectl wait --for=condition=available --timeout=300s deployment/postgres
```

## API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/health` | GET | Health check |
| `/api/workflow/execute` | POST | Start new workflow |
| `/api/workflow/goal/{goalId}` | GET | Get goal summary |
| `/api/workflow/goal/{goalId}/tasks` | GET | Get task details |
| `/actuator/metrics` | GET | Application metrics |

### Status Values

- **Goal Status**: `PLANNING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`
- **Task Status**: `PENDING`, `IN_PROGRESS`, `COMPLETED`

### Example Queries

- Simple: `"Plan a coffee break"`
- Medium: `"Organize a team meeting with agenda and follow-ups"`
- Complex: `"Start a food truck business including permits, location research, menu planning, and marketing"`

## Best Practices

1. **Always export PUBLIC_IP** before running commands
2. **Use jq for readable JSON** output
3. **Store GOAL_ID** for tracking workflows
4. **Monitor logs** during development
5. **Check health endpoint** before testing
6. **Use watch command** for real-time monitoring

## Security Notes

- The current setup uses HTTP (not HTTPS) for simplicity
- Database is only accessible within the cluster
- No authentication is configured (development setup)
- For production, add TLS certificates and authentication