#!/usr/bin/env bash
set -euo pipefail

if [ -z "${GCP_PROJECT:-}" ]; then
  echo "Error: GCP_PROJECT is not set."
  echo "  export GCP_PROJECT=your-gcp-project-id"
  exit 1
fi

CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || true)
if [[ "$CURRENT_CONTEXT" == "minikube" ]]; then
  echo "Error: kubectl context is 'minikube' — refusing to run GKE deploy."
  echo "  Switch with: kubectl config use-context gke_${GCP_PROJECT}_europe-west1-b_d2s-cluster"
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
# Wait for any in-progress ingress deletion to complete before applying,
# otherwise GKE's ingress controller races and the new ingress may not be created.
if kubectl get ingress d2s-ingress &>/dev/null; then
  echo "  Waiting for existing ingress deletion to settle..."
  kubectl wait --for=delete ingress/d2s-ingress --timeout=60s 2>/dev/null || true
fi

kubectl kustomize "$SCRIPT_DIR/overlays/gke" \
  | sed "s|gcr.io/PROJECT_ID|$REGISTRY|g" \
  | kubectl apply --server-side --force-conflicts -f -

# Force pod restarts so GKE pulls the updated :latest images.
# Deletes all Deployment pods simultaneously; K8s recreates them and pulls fresh images.
# Leave postgres StatefulSet pods alone — pinned upstream image, no re-pull needed.
echo "==> Restarting all pods to pick up new images..."
kubectl delete pods -l app!=postgres

echo "==> Waiting for infrastructure..."
kubectl rollout status deployment/rabbitmq --timeout=240s
kubectl rollout status statefulset/postgres --timeout=120s

echo "==> Waiting for backend services..."
kubectl rollout status deployment/utracked --timeout=240s
kubectl rollout status deployment/orchestrator --timeout=240s
kubectl rollout status deployment/sonar-backend --timeout=180s
kubectl rollout status deployment/spider --timeout=180s
kubectl rollout status deployment/storefront --timeout=180s

echo "==> Waiting for vehicle services..."
kubectl rollout status deployment/vehicle-1-whereami --timeout=180s
kubectl rollout status deployment/vehicle-1-sonar --timeout=180s
kubectl rollout status deployment/vehicle-1-brakenow --timeout=180s
kubectl rollout status deployment/vehicle-1-simulator --timeout=180s
kubectl rollout status deployment/vehicle-2-whereami --timeout=180s
kubectl rollout status deployment/vehicle-2-sonar --timeout=180s
kubectl rollout status deployment/vehicle-2-brakenow --timeout=180s
kubectl rollout status deployment/vehicle-2-simulator --timeout=180s

echo "==> Done. Current pod status:"
kubectl get pods
echo ""

echo "==> Checking for unhealthy pods..."
UNHEALTHY=$(kubectl get pods --no-headers | grep -v "Running\|Completed" || true)
if [ -n "$UNHEALTHY" ]; then
  echo "ERROR: Some pods are not healthy:"
  echo "$UNHEALTHY"
  exit 1
fi
echo "  All pods are Running."

STATIC_IP=$(gcloud compute addresses describe d2s-static-ip --global --format="get(address)" 2>/dev/null || true)
echo ""
if [ -n "$STATIC_IP" ]; then
  echo "==> Static IP  : $STATIC_IP"
fi
echo "==> Frontend   : https://sudern.lol"
echo "    (HTTPS cert provisioning takes 10-20 min after DNS propagates)"
