# GKE Deployment Guide for Agentic Workflow Engine

## Overview

This guide explains how to deploy the Agentic Workflow Engine to Google Kubernetes Engine (GKE) using the automated CI/CD pipeline with GitHub Actions. This is a **development environment setup** with PostgreSQL running as a pod in the same cluster for simplicity.

## Architecture

The deployment includes:
- **GKE Autopilot Cluster**: Managed Kubernetes cluster
- **In-Cluster PostgreSQL**: PostgreSQL pod running in the same cluster
- **Container Registry**: Docker image storage
- **Load Balancer**: External access
- **GitHub Actions**: CI/CD pipeline automation

## Prerequisites

### 1. Google Cloud Setup

1. **Enable Required APIs**:
   ```bash
   gcloud services enable container.googleapis.com
   gcloud services enable sqladmin.googleapis.com
   gcloud services enable containerregistry.googleapis.com
   ```

2. **Create Service Account**:
   ```bash
   gcloud iam service-accounts create gke-deploy-sa \
     --description="Service account for GKE deployment" \
     --display-name="GKE Deploy SA"
   ```

3. **Grant Required Permissions**:
   ```bash
   gcloud projects add-iam-policy-binding adam-466814 \
     --member="serviceAccount:gke-deploy-sa@adam-466814.iam.gserviceaccount.com" \
     --role="roles/container.admin"
   
   gcloud projects add-iam-policy-binding adam-466814 \
     --member="serviceAccount:gke-deploy-sa@adam-466814.iam.gserviceaccount.com" \
     --role="roles/storage.admin"
   ```

4. **Generate Service Account Key**:
   ```bash
   gcloud iam service-accounts keys create gke-deploy-key.json \
     --iam-account=gke-deploy-sa@adam-466814.iam.gserviceaccount.com
   ```

### 2. GitHub Secrets Configuration

Add the following secrets to your GitHub repository (`Settings > Secrets and variables > Actions`):

| Secret Name | Description | Value |
|-------------|-------------|-------|
| `GCP_SA_KEY` | Base64 encoded service account JSON | `cat gke-deploy-key.json \| base64` |
| `OPENAI_API_KEY` | OpenAI API key for AI agents | Your OpenAI API key |
| `DATABASE_PASSWORD` | PostgreSQL password for in-cluster database | `password` (same as local dev) |

### 3. DNS Configuration (Optional)

If using the custom domain `agentic-workflow.alsalman.dev`:

1. **Reserve Static IP**:
   ```bash
   gcloud compute addresses create agentic-workflow-ip --global
   ```

2. **Get IP Address**:
   ```bash
   gcloud compute addresses describe agentic-workflow-ip --global
   ```

3. **Update DNS**: Point your domain to the reserved IP address.

## Deployment Components

### 1. Docker Configuration

**File**: `Dockerfile`
- Multi-stage build using Maven and OpenJDK 24
- Alpine Linux base for minimal image size
- Non-root user for security
- Health checks via actuator endpoint
- Java preview features enabled

### 2. Kubernetes Manifests

**File**: `kubernetes/deployment.yaml`

#### PostgreSQL Database:
- **Single Replica**: Development setup with one PostgreSQL pod
- **Alpine Image**: Lightweight PostgreSQL 15 Alpine
- **In-Memory Storage**: Using emptyDir volume (data lost on restart)
- **Resource Limits**: 512MB memory, 200m CPU

#### Application Deployment:
- **Single Replica**: Development setup with one app instance
- **Init Container**: Waits for PostgreSQL to be ready
- **Resource Limits**: 1GB memory, 500m CPU
- **Health Checks**: Liveness and readiness probes
- **Environment Variables**: Injected from Kubernetes secrets

#### Services:
- **PostgreSQL Service**: ClusterIP for internal database access
- **LoadBalancer**: Exposes application externally

#### Secrets Management:
- OpenAI API key
- Database password
- All secrets managed via Kubernetes secrets

### 3. CI/CD Pipeline

**File**: `.github/workflows/deploy-to-gke.yml`

#### Pipeline Steps:
1. **Checkout Code**: Latest commit from main branch
2. **Authenticate**: Google Cloud authentication
3. **Build Image**: Docker multi-stage build
4. **Push Image**: To Google Container Registry
5. **Setup Infrastructure**: 
   - Create/verify GKE cluster
6. **Deploy Application**:
   - Update Kubernetes secrets
   - Deploy PostgreSQL and application pods
   - Wait for deployments readiness

#### Automatic Infrastructure Creation:
- **GKE Cluster**: `agentic-workflow-cluster` (Autopilot)
- **PostgreSQL Pod**: In-cluster database
- **Database**: `agentic_workflow` (auto-created)

## Deployment Process

### Automatic Deployment

1. **Push to Main Branch**:
   ```bash
   git push origin main
   ```

2. **Monitor Pipeline**: 
   - Go to GitHub Actions tab
   - Watch the "Deploy Agentic Workflow Engine to GKE" workflow

3. **Verify Deployment**:
   ```bash
   # Get cluster credentials
   gcloud container clusters get-credentials agentic-workflow-cluster --zone us-west1
   
   # Check deployment status
   kubectl get deployments
   kubectl get pods
   kubectl get services
   ```

### Manual Deployment

If you need to deploy manually:

1. **Build and Push Image**:
   ```bash
   docker build -t gcr.io/adam-466814/agentic-workflow-engine:manual .
   docker push gcr.io/adam-466814/agentic-workflow-engine:manual
   ```

2. **Update Deployment**:
   ```bash
   # Update image in deployment.yaml
   sed -i 's/:latest/:manual/' kubernetes/deployment.yaml
   
   # Apply changes
   kubectl apply -f kubernetes/deployment.yaml
   ```

## Monitoring and Troubleshooting

### Health Checks

The application exposes several health endpoints:

- **Liveness**: `http://[service-ip]/actuator/health`
- **Readiness**: `http://[service-ip]/actuator/health`
- **Metrics**: `http://[service-ip]/actuator/metrics`

### Logging

1. **View Application Logs**:
   ```bash
   kubectl logs -f deployment/agentic-workflow-engine
   ```

2. **View Pod Events**:
   ```bash
   kubectl describe pod [pod-name]
   ```

### Common Issues

#### Database Connection Issues:
```bash
# Check secrets
kubectl get secrets agentic-workflow-secrets -o yaml

# Check PostgreSQL pod
kubectl get pods -l app=postgres
kubectl logs -l app=postgres

# Test database connection
kubectl exec -it deployment/postgres -- psql -U postgres -d agentic_workflow
```

#### Image Pull Issues:
```bash
# Check if image exists
gcloud container images list --repository=gcr.io/adam-466814

# Check deployment events
kubectl describe deployment agentic-workflow-engine
```

#### Service Access Issues:
```bash
# Get external IP
kubectl get services agentic-workflow-engine

# Check ingress status
kubectl get ingress agentic-workflow-engine-ingress
```

## Scaling and Updates

### Horizontal Scaling:
```bash
kubectl scale deployment agentic-workflow-engine --replicas=5
```

### Rolling Updates:
The pipeline automatically performs rolling updates when new code is pushed to main.

### Manual Update:
```bash
kubectl set image deployment/agentic-workflow-engine \
  agentic-workflow-engine=gcr.io/adam-466814/agentic-workflow-engine:new-tag
```

## Security Considerations

1. **Non-root Container**: Application runs as non-root user
2. **Secret Management**: All sensitive data in Kubernetes secrets
3. **Network Policies**: Consider implementing network policies for production
4. **RBAC**: Use role-based access control for cluster access
5. **Image Scanning**: Consider enabling vulnerability scanning

## Cost Optimization

1. **Autopilot Cluster**: Pay only for running pods
2. **f1-micro Database**: Minimal cost for development/testing
3. **Resource Limits**: Set appropriate CPU/memory limits
4. **Scheduled Scaling**: Scale down during off-hours if needed

## Environment Management

### Development Environment (Current Setup):
- **Application**: Single replica deployment
- **Database**: In-cluster PostgreSQL pod with emptyDir storage
- **Resources**: Minimal CPU/memory allocation
- **Data Persistence**: ⚠️ Data lost on pod restart (development only)

### Production Environment:
Consider upgrading to:
- **Database**: Cloud SQL PostgreSQL or persistent volume
- **Replicas**: 3+ instances for high availability
- **Storage**: Persistent volumes for database
- **Resources**: Higher CPU/memory limits
- **Monitoring**: Enable Google Cloud Monitoring
- **Backup**: Configure automated database backups
- **Security**: Network policies and RBAC

## API Access

Once deployed, access the API at:
- **LoadBalancer IP**: `http://[external-ip]/api/workflow`
- **Custom Domain**: `https://agentic-workflow.alsalman.dev/api/workflow` (if configured)

Example API calls:
```bash
# Start workflow
curl -X POST http://[external-ip]/api/workflow/execute \
  -H "Content-Type: application/json" \
  -d '{"query": "Plan a 3-day trip to Paris"}'

# Check status
curl http://[external-ip]/api/workflow/goal/[goal-id]
```

## Next Steps

1. **Custom Domain**: Configure SSL certificate and domain
2. **Monitoring**: Set up Google Cloud Monitoring and alerting
3. **Backup Strategy**: Configure database backups
4. **CI/CD Enhancements**: Add staging environment
5. **Security Hardening**: Implement network policies and RBAC