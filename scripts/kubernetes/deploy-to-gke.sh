#!/bin/bash

# Deploy Agentic Workflow Engine to Google Kubernetes Engine (GKE)

set -e

# Configuration
PROJECT_ID="${PROJECT_ID:-your-gcp-project}"
CLUSTER_NAME="${CLUSTER_NAME:-agentic-workflow-cluster}"
ZONE="${ZONE:-us-central1-a}"
IMAGE_NAME="gcr.io/${PROJECT_ID}/agentic-workflow-engine"
NAMESPACE="${NAMESPACE:-default}"

echo "=== Deploying Agentic Workflow Engine to GKE ==="
echo "Project: $PROJECT_ID"
echo "Cluster: $CLUSTER_NAME"
echo "Zone: $ZONE"
echo "Image: $IMAGE_NAME"
echo ""

# Check prerequisites
if ! command -v gcloud &> /dev/null; then
    echo "❌ gcloud CLI not found. Please install Google Cloud SDK."
    exit 1
fi

if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl not found. Please install kubectl."
    exit 1
fi

if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

# Build and push Docker image
echo "🏗️  Building Docker image..."
cd /Users/adam/projects/agentic-workflow-engine

# Create Dockerfile if it doesn't exist
if [ ! -f Dockerfile ]; then
    cat > Dockerfile << 'EOF'
FROM openjdk:24-jdk-slim

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

# Run application
EXPOSE 8080
CMD ["java", "-jar", "target/agentic-workflow-engine-0.0.1-SNAPSHOT.jar"]
EOF
    echo "✅ Created Dockerfile"
fi

docker build -t $IMAGE_NAME:latest .
echo "✅ Docker image built"

# Configure Docker for GCR
echo "🔐 Configuring Docker for Google Container Registry..."
gcloud auth configure-docker

# Push image
echo "📤 Pushing image to GCR..."
docker push $IMAGE_NAME:latest
echo "✅ Image pushed successfully"

# Get cluster credentials
echo "🔐 Getting cluster credentials..."
gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE --project=$PROJECT_ID

# Create Kubernetes manifests
mkdir -p k8s

# Deployment manifest
cat > k8s/deployment.yaml << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agentic-workflow-engine
  namespace: $NAMESPACE
spec:
  replicas: 2
  selector:
    matchLabels:
      app: agentic-workflow-engine
  template:
    metadata:
      labels:
        app: agentic-workflow-engine
    spec:
      containers:
      - name: agentic-workflow-engine
        image: $IMAGE_NAME:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: agentic-workflow-secrets
              key: database-url
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: agentic-workflow-secrets
              key: database-username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: agentic-workflow-secrets
              key: database-password
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: agentic-workflow-secrets
              key: openai-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
EOF

# Service manifest
cat > k8s/service.yaml << EOF
apiVersion: v1
kind: Service
metadata:
  name: agentic-workflow-engine
  namespace: $NAMESPACE
spec:
  selector:
    app: agentic-workflow-engine
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
EOF

# Ingress manifest (optional)
cat > k8s/ingress.yaml << EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: agentic-workflow-engine
  namespace: $NAMESPACE
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - workflow.yourdomain.com
    secretName: agentic-workflow-tls
  rules:
  - host: workflow.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: agentic-workflow-engine
            port:
              number: 8080
EOF

echo "✅ Kubernetes manifests created"

# Create secrets (user needs to provide values)
echo "🔐 Creating secrets..."
echo "⚠️  You need to create the secrets manually:"
echo ""
echo "kubectl create secret generic agentic-workflow-secrets \\"
echo "  --from-literal=database-url='your-database-url' \\"
echo "  --from-literal=database-username='your-db-username' \\"
echo "  --from-literal=database-password='your-db-password' \\"
echo "  --from-literal=openai-api-key='your-openai-api-key' \\"
echo "  --namespace=$NAMESPACE"
echo ""

read -p "Have you created the secrets? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Please create the secrets first, then run this script again."
    exit 1
fi

# Deploy to cluster
echo "🚀 Deploying to cluster..."
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

echo "✅ Deployment completed!"
echo ""
echo "📊 Check deployment status:"
echo "kubectl get pods -n $NAMESPACE -l app=agentic-workflow-engine"
echo ""
echo "🔍 View logs:"
echo "kubectl logs -n $NAMESPACE -l app=agentic-workflow-engine"
echo ""
echo "🧪 Test the deployment:"
echo "./scripts/kubernetes/test-k8s-api.sh"