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
kubectl rollout status deployment/orchestrator --timeout=240s
kubectl rollout status deployment/utracked --timeout=240s
kubectl rollout status deployment/spider --timeout=180s
kubectl rollout status deployment/storefront --timeout=180s

echo "==> Done. Current pod status:"
kubectl get pods
echo ""

IP=""
echo "==> Waiting for ingress external IP (GKE load balancer provisioning takes 2-5 min)..."
for i in $(seq 1 24); do
  IP=$(kubectl get ingress d2s-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)
  if [ -n "$IP" ]; then
    echo ""
    echo "==> Frontend available at: http://$IP"
    break
  fi
  printf "  Still provisioning... (%d/24, ~%ds elapsed)\r" "$i" "$((i * 15))"
  sleep 15
done

if [ -z "$IP" ]; then
  echo ""
  echo "  Load balancer IP not yet assigned. Check later with:"
  echo "  kubectl get ingress d2s-ingress"
fi
