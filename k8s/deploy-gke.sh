#!/usr/bin/env bash
set -euo pipefail

if [ -z "${GCP_PROJECT:-}" ]; then
  echo "Error: GCP_PROJECT is not set."
  echo "  export GCP_PROJECT=your-gcp-project-id"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."
REGISTRY="gcr.io/$GCP_PROJECT"
TAG="${TAG:-latest}"

echo "==> Project : $GCP_PROJECT"
echo "==> Registry: $REGISTRY"
echo "==> Tag     : $TAG"
echo ""

echo "==> Authenticating Docker with GCR..."
gcloud auth configure-docker gcr.io --quiet

echo "==> Building and pushing all images to GCR..."
(cd "$ROOT_DIR" && REGISTRY="$REGISTRY" TAG="$TAG" docker buildx bake --push)
echo "==> All images pushed."

echo "==> Applying Kubernetes manifests..."
kubectl kustomize "$SCRIPT_DIR/overlays/gke" \
  | sed "s|gcr.io/PROJECT_ID|$REGISTRY|g" \
  | kubectl apply -f -

echo "==> Waiting for infrastructure..."
kubectl rollout status deployment/rabbitmq --timeout=120s
kubectl rollout status statefulset/postgres --timeout=120s

echo "==> Waiting for backend services..."
kubectl rollout status deployment/orchestrator --timeout=180s
kubectl rollout status deployment/utracked --timeout=180s
kubectl rollout status deployment/spider --timeout=120s
kubectl rollout status deployment/storefront --timeout=120s

echo "==> Done. Current pod status:"
kubectl get pods
echo ""

echo "==> Ingress (external IP may take a few minutes to provision):"
kubectl get ingress d2s-ingress
echo ""
echo "  Access the frontend at: http://<EXTERNAL-IP>"
echo "  (run 'kubectl get ingress d2s-ingress' again if ADDRESS is still pending)"