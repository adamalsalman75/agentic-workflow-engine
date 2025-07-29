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
Performs basic health checks and API functionality tests.

**Usage:**
```bash
# Make sure application is running first
./scripts/local/test-basic-api.sh
```

### `local/test-phase2-api.sh`
Comprehensive testing of the Template System Phase 2 features including:
- Template listing
- Parameter discovery
- Extended parameter types (DATE, CURRENCY, LOCATION)
- Template execution

**Usage:**
```bash
./scripts/local/test-phase2-api.sh
```

### `local/test-workflow.sh`
Tests the core workflow engine functionality.

**Usage:**
```bash
./scripts/local/test-workflow.sh
```


## Kubernetes Scripts

### `kubernetes/deploy-to-gke.sh`
Complete deployment script for Google Kubernetes Engine (GKE).

**Features:**
- Builds and pushes Docker image to Google Container Registry
- Creates Kubernetes manifests
- Deploys application with proper resource limits
- Sets up health checks and secrets

**Prerequisites:**
- `gcloud` CLI installed and authenticated
- `kubectl` installed
- Docker installed
- GKE cluster created

**Usage:**
```bash
# Set environment variables
export PROJECT_ID="your-gcp-project"
export CLUSTER_NAME="your-cluster"
export ZONE="us-central1-a"

./scripts/kubernetes/deploy-to-gke.sh
```

### `kubernetes/test-k8s-api.sh`
Tests API endpoints against Kubernetes deployment.

**Features:**
- Auto-detects service endpoint or uses Ingress
- Sets up port-forwarding if needed
- Tests core functionality in K8s environment

**Usage:**
```bash
# Test against default service
./scripts/kubernetes/test-k8s-api.sh

# Test with custom configuration
export NAMESPACE="production"
export SERVICE_NAME="agentic-workflow-engine"
export INGRESS_HOST="workflow.yourdomain.com"
./scripts/kubernetes/test-k8s-api.sh
```

### `kubernetes/check-status.sh`
Checks the overall health and status of the Kubernetes deployment.

**Usage:**
```bash
./scripts/kubernetes/check-status.sh
```

### `kubernetes/monitor-workflow.sh`
Real-time monitoring of a specific workflow execution in Kubernetes.

**Usage:**
```bash
./scripts/kubernetes/monitor-workflow.sh <goal-id>
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

# 3. Run comprehensive template system tests
./scripts/local/test-phase2-api.sh
```

### Kubernetes Testing
```bash
# 1. Deploy to GKE
./scripts/kubernetes/deploy-to-gke.sh

# 2. Test the deployment
./scripts/kubernetes/test-k8s-api.sh
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