#!/usr/bin/env bash
set -euo pipefail

CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || true)
if [[ "$CURRENT_CONTEXT" != "minikube" ]]; then
  echo "Error: kubectl context is '$CURRENT_CONTEXT' — refusing to run Minikube deploy."
  echo "  Switch with: kubectl config use-context minikube"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."
K8S_DIR="$SCRIPT_DIR"

echo "==> Enabling Minikube ingress addon..."
minikube addons enable ingress

echo "==> Tearing down existing deployment..."
kubectl delete -k "$K8S_DIR/overlays/minikube" 2>/dev/null || true

echo "==> Pulling public images into Minikube..."
minikube image pull rabbitmq:3-management &
minikube image pull postgres:18-alpine &
wait

echo "==> Building all service images in parallel..."
(cd "$ROOT_DIR" && docker buildx bake)
echo "==> All images built."

echo "==> Loading images into Minikube in parallel..."
IMAGES=(brakenow orchestrator simulator sonar spider utracked whereami storefront)
for IMAGE in "${IMAGES[@]}"; do
  minikube image load "$IMAGE:latest" &
done
wait
echo "==> All images loaded."

echo "==> Applying Kubernetes manifests..."
kubectl apply -k "$K8S_DIR/overlays/minikube"

echo "==> Waiting for all services to be ready..."
kubectl rollout status deployment/rabbitmq         --timeout=120s &
kubectl rollout status statefulset/postgres        --timeout=120s &
kubectl rollout status deployment/utracked         --timeout=240s &
kubectl rollout status deployment/orchestrator     --timeout=240s &
kubectl rollout status deployment/sonar-backend    --timeout=180s &
kubectl rollout status deployment/spider           --timeout=180s &
kubectl rollout status deployment/storefront       --timeout=180s &
kubectl rollout status deployment/vehicle-1-whereami  --timeout=180s &
kubectl rollout status deployment/vehicle-1-sonar     --timeout=180s &
kubectl rollout status deployment/vehicle-1-brakenow  --timeout=180s &
kubectl rollout status deployment/vehicle-1-simulator --timeout=180s &
kubectl rollout status deployment/vehicle-2-whereami  --timeout=180s &
kubectl rollout status deployment/vehicle-2-sonar     --timeout=180s &
kubectl rollout status deployment/vehicle-2-brakenow  --timeout=180s &
kubectl rollout status deployment/vehicle-2-simulator --timeout=180s &
wait

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

echo ""
echo "==> Frontend available at: http://$(minikube ip)"