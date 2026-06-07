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
  echo "  Switch with: kubectl config use-context gke_${GCP_PROJECT}_europe-west1-b_YOUR_CLUSTER"
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
  | kubectl apply --server-side --force-conflicts -f -

# Delete all non-postgres pods so GKE recreates them and pulls fresh images.
# imagePullPolicy: Always (set in the GKE overlay) ensures each new pod fetches
# the image we just pushed rather than a node-cached layer.
echo "==> Restarting all pods to pick up new images..."
kubectl delete pods -l app!=postgres

echo "==> Waiting for all pods to be Ready..."
kubectl wait pod --for=condition=Ready --all --timeout=300s

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
echo "==> Frontend   : https://example.com"
echo "    (HTTPS cert provisioning takes 10-20 min after DNS propagates)"
