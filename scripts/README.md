# Scripts Directory

This directory contains scripts for testing and deploying the Agentic Workflow Engine.

## Structure

```
scripts/
├── local/          # Local development and testing scripts
├── kubernetes/     # Kubernetes deployment and testing scripts
└── README.md       # This file
```

## Local Development Scripts

### `local/start-local.sh`
Starts the Spring Boot application locally with proper environment setup.

**Prerequisites:**
- PostgreSQL running locally
- Database `agentic_workflow` exists (script will create if missing)
- `OPENAI_API_KEY` environment variable set

**Usage:**
```bash
./scripts/local/start-local.sh
```

### `local/test-basic-api.sh`
Performs comprehensive health checks and workflow functionality tests with polling.

**Features:**
- Health endpoint verification
- Complete workflow execution test with polling
- Shows real-time status updates
- Displays final results and task completion
- Template system validation  
- Story 2 advanced validation testing

**Usage:**
```bash
# Make sure application is running first
./scripts/local/test-basic-api.sh
```




## Kubernetes Scripts

**Note:** Deployment to GKE is handled by GitHub Actions pipeline (`.github/workflows/deploy-to-gke.yml`).

### `kubernetes/test-k8s-comprehensive.sh`
Comprehensive Kubernetes testing that combines API testing with workflow monitoring.

**Features:**
- Auto-detects service endpoint (LoadBalancer, Ingress, or port-forward)
- Health check validation
- Template system testing
- Complete workflow execution with real-time monitoring
- Template execution testing
- Displays final results and task completion
- Longer timeout for K8s latency

**Usage:**
```bash
# Test against default service
./scripts/kubernetes/test-k8s-comprehensive.sh

# Test with custom configuration
export NAMESPACE="production"
export SERVICE_NAME="agentic-workflow-engine"
export INGRESS_HOST="workflow.yourdomain.com"
./scripts/kubernetes/test-k8s-comprehensive.sh
```

### `kubernetes/check-status.sh`
Checks the overall health and status of the Kubernetes deployment.

**Usage:**
```bash
./scripts/kubernetes/check-status.sh
```

## Environment Variables

### Local Development
- `OPENAI_API_KEY` - Your OpenAI API key (required)
- `DATABASE_URL` - PostgreSQL connection string (default: `jdbc:postgresql://localhost:5432/agentic_workflow`)
- `DATABASE_USERNAME` - Database username (default: `postgres`)
- `DATABASE_PASSWORD` - Database password (default: `password`)

### Kubernetes Deployment
- `PROJECT_ID` - Google Cloud Project ID
- `CLUSTER_NAME` - GKE cluster name
- `ZONE` - GKE cluster zone
- `NAMESPACE` - Kubernetes namespace (default: `default`)
- `SERVICE_NAME` - Kubernetes service name
- `INGRESS_HOST` - Ingress hostname for external access

## Quick Start

### Local Testing
```bash
# 1. Start the application
./scripts/local/start-local.sh

# 2. In another terminal, run basic tests
./scripts/local/test-basic-api.sh

```

### Kubernetes Testing
```bash
# 1. Deploy via GitHub Actions (automatic on push to main)
# 2. Run comprehensive tests
./scripts/kubernetes/test-k8s-comprehensive.sh
```

## Common Issues

### Local Development
- **Database connection errors**: Ensure PostgreSQL is running and database exists
- **OpenAI API errors**: Check that `OPENAI_API_KEY` is set correctly
- **Port 8080 in use**: Stop other applications using port 8080

### Kubernetes
- **Image pull errors**: Ensure Docker image is pushed to correct registry
- **Secret not found**: Create required secrets before deployment
- **Pod crashes**: Check logs with `kubectl logs` and verify environment variables

## Script Permissions

Make scripts executable:
```bash
chmod +x scripts/local/*.sh
chmod +x scripts/kubernetes/*.sh
```