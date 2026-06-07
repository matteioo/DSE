#!/usr/bin/env bash
set -e

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

echo "==> Waiting for infra to be ready..."
kubectl rollout status deployment/rabbitmq --timeout=120s
kubectl rollout status statefulset/postgres --timeout=120s

echo "==> Done. Current pod status:"
kubectl get pods

echo ""
echo "==> Frontend available at: http://$(minikube ip)"