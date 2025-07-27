# Scripts Directory

This directory contains utility scripts for testing and monitoring the Agentic Workflow Engine deployed on GKE.

## Prerequisites

- `kubectl` configured with cluster access
- `curl` command-line tool  
- `jq` for JSON formatting
- Bash shell

## Scripts

### 🧪 `test-workflow.sh`
Start and test a new workflow execution.

```bash
# Test with default query
./test-workflow.sh

# Test with custom query
./test-workflow.sh "Plan a weekend trip to the mountains"

# Test complex workflow
./test-workflow.sh "Help me start a small restaurant business including permits, location, menu planning, and marketing strategy"
```

**Features:**
- Automatically gets cluster public IP
- Creates workflow and captures goal ID
- Shows monitoring commands
- Error handling for common issues

### 🔍 `check-status.sh`
Check the overall health and status of the deployment.

```bash
./check-status.sh
```

**Shows:**
- Kubernetes pod status
- Service status and external IP
- API health check
- Component health status
- Quick deployment stats

### 📊 `monitor-workflow.sh`
Real-time monitoring of a specific workflow execution.

```bash
# Monitor a workflow (get goal ID from test-workflow.sh)
./monitor-workflow.sh 550e8400-e29b-41d4-a716-446655440000
```

**Features:**
- Real-time status updates
- Task completion tracking
- Token usage monitoring
- Automatic completion detection
- Clean formatted output

## Quick Start

1. **Check if everything is running:**
   ```bash
   ./check-status.sh
   ```

2. **Start a test workflow:**
   ```bash
   ./test-workflow.sh "Plan a coffee break"
   ```

3. **Monitor the workflow (use goal ID from step 2):**
   ```bash
   ./monitor-workflow.sh <goal-id-from-step-2>
   ```

## Environment Setup

The scripts automatically handle environment setup, but you can manually set:

```bash
# Get cluster credentials
gcloud container clusters get-credentials agentic-workflow-cluster --zone us-west1 --project adam-466814

# Export public IP (done automatically by scripts)
export PUBLIC_IP=$(kubectl get svc agentic-workflow-engine -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
```

## Example Output

### test-workflow.sh
```
🚀 Starting workflow: Plan a weekend trip to the mountains
📍 API Base URL: http://35.247.57.3

✅ Workflow started successfully!
🆔 Goal ID: 550e8400-e29b-41d4-a716-446655440000
🔗 Goal URL: http://35.247.57.3/api/workflow/goal/550e8400-e29b-41d4-a716-446655440000
📋 Tasks URL: http://35.247.57.3/api/workflow/goal/550e8400-e29b-41d4-a716-446655440000/tasks

⏳ Waiting for initial processing...

📊 Current Status:
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "query": "Plan a weekend trip to the mountains",
  "status": "PLANNING",
  "createdAt": "2025-07-27T10:00:00Z",
  "completedAt": null
}
```

### check-status.sh
```
🔍 Agentic Workflow Engine Status Check
=====================================

☸️  Cluster Status:
NAME                                       READY   STATUS    RESTARTS   AGE
agentic-workflow-engine-74476d5574-h7zmt   1/1     Running   0          10m
postgres-576fbb8bfc-lr6gc                  1/1     Running   0          10m

🌐 Service Status:
NAME                      TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
agentic-workflow-engine   LoadBalancer   34.118.239.165   35.247.57.3   80:32759/TCP   10m

🌍 Public IP: 35.247.57.3
🔗 API Base URL: http://35.247.57.3

❤️  API Health Check:
✅ API is healthy and responding
```

## Troubleshooting

### Common Issues

1. **"kubectl not configured"**
   ```bash
   gcloud container clusters get-credentials agentic-workflow-cluster --zone us-west1 --project adam-466814
   ```

2. **"No external IP available"**
   - LoadBalancer may still be provisioning
   - Wait a few minutes and try again

3. **"API not responding"**
   - Check pod status: `kubectl get pods`
   - Check logs: `kubectl logs deployment/agentic-workflow-engine`

4. **Permission denied on scripts**
   ```bash
   chmod +x scripts/*.sh
   ```

## Integration with CI/CD

These scripts can be integrated into CI/CD pipelines for automated testing:

```bash
# In your CI pipeline
./scripts/check-status.sh
./scripts/test-workflow.sh "Integration test workflow"
```