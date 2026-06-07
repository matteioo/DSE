# Development Setup

## Prerequisites

- Docker + Docker Compose
- Java 25 + Maven (for local service development)
- Quarkus CLI (optional, for scaffolding)

---

## Docker Compose

All commands are run from the `services/` directory.

```bash
cd services/
```

### Start infrastructure only (Postgres + RabbitMQ)

```bash
docker compose up
```

Useful when developing a single service locally with `quarkus:dev`.

### Start full stack

```bash
docker compose --profile app up --build
```

### Start full stack, excluding a service (e.g. to run it locally instead)

```bash
docker compose --profile app up --build --scale utracked=0
```

### Stop and remove volumes (required after changing DB init scripts)

```bash
docker compose down -v
```

---

## Service Ports

| Service      | Host Port | Swagger UI                              |
|--------------|-----------|-----------------------------------------|
| brakenow     | 8081      | http://localhost:8081/q/swagger-ui      |
| orchestrator | 8082      | http://localhost:8082/q/swagger-ui      |
| simulator    | 8083      | —                                       |
| sonar        | 8084      | http://localhost:8084/q/swagger-ui      |
| spider       | 8085      | http://localhost:8085/q/swagger-ui      |
| utracked     | 8086      | http://localhost:8086/q/swagger-ui      |
| whereami     | 8087      | http://localhost:8087/q/swagger-ui      |
| storefront   | 3000      | http://localhost:3000                   |
| RabbitMQ UI  | 15672     | http://localhost:15672 (dse/dse)        |
| PostgreSQL   | 5432      | —                                       |

> Swagger UI is only available if `quarkus.swagger-ui.always-include=true` is set in `application.properties`.

---

## Local Single-Service Development

Run infrastructure in Docker, your service with live reload:

```bash
# Terminal 1 — infrastructure
cd services/
docker compose up

# Terminal 2 — your service
cd services/utracked/
./mvnw quarkus:dev
```

To call a Docker service from your local service, use `localhost:<mapped-port>`.
To call your local service from a Docker container, use `host.docker.internal:<port>`.

Override service URLs without touching `application.properties` by creating
`services/docker-compose.override.yml` (gitignored):

```yaml
services:
  orchestrator:
    environment:
      UTRACKED_URL: http://host.docker.internal:8086
```

---

## Kubernetes (Minikube)

### Prerequisites

```bash
minikube start --disk-size=30g
```

---

### Deploy everything

From the repo root:

```bash
./k8s/deploy-minikube.sh
```

This will:
1. Enable the ingress addon
2. Pull public images (RabbitMQ, Postgres) into Minikube
3. Build all service images in parallel via `docker buildx bake`
4. Load built images into Minikube
5. Apply all manifests via Kustomize
6. Print the frontend URL

---

### Access the frontend

```bash
minikube ip   # e.g. 192.168.49.2
# open http://<minikube-ip> in browser
```

To inspect the RabbitMQ management UI:

```bash
kubectl port-forward deployment/rabbitmq 15672:15672
# open http://localhost:15672 (dse/dse)
```

---

### Building images

Build all images at once (true parallel):

```bash
cd <repo-root>
docker buildx bake
```

Build a single service only (faster during development):

```bash
docker buildx bake spider
docker buildx bake storefront
```

After rebuilding, reload into Minikube and restart the pod:

```bash
minikube image load spider:latest
kubectl rollout restart deployment/spider
```

---

### Managing the cluster

**Check pod status:**
```bash
kubectl get pods
kubectl get pods -w          # watch live
```

**View logs for a service:**
```bash
kubectl logs deployment/spider
kubectl logs deployment/spider -f   # follow
```

**Restart a single service** (e.g. after config change):
```bash
kubectl rollout restart deployment/spider
kubectl rollout restart deployment/vehicle-1-simulator
```

**Scale a service down/up** (effectively stop/start):
```bash
kubectl scale deployment/spider --replicas=0   # stop
kubectl scale deployment/spider --replicas=1   # start
```

**Open a shell inside a running pod:**
```bash
kubectl exec -it deployment/spider -- sh
```

**Describe a pod** (useful for debugging CrashLoopBackOff):
```bash
kubectl describe pod <pod-name>
```

---

### Tearing down

**Remove all deployed resources** (keeps images cached in Minikube):
```bash
kubectl delete -k k8s/overlays/minikube
```

> Images remain cached in Minikube after delete — the next deploy skips the pull/load steps for unchanged images.

**Remove a specific resource:**
```bash
kubectl delete deployment spider
kubectl delete statefulset postgres
kubectl delete pvc postgres-data-postgres-0   # also wipes DB data
```

**Wipe Minikube entirely** (removes cluster, all pods, all cached images):
```bash
minikube delete
```

---

### Image caching behaviour

`kubectl delete` removes pods and Kubernetes resources but **does not remove images** from Minikube's cache. This means:

| Action | Pods removed | Images removed |
|---|---|---|
| `kubectl delete -k ...` | yes | no |
| `minikube delete` | yes | yes |

So on re-deploy after `kubectl delete`, images are already present and the load step is instant. Only `minikube delete` forces a full rebuild and reload.

---

### Kustomize structure

```
k8s/
├── base/                    # shared manifests (infra + backend + ingress)
│   ├── infra/               # Postgres, RabbitMQ
│   ├── backend/             # utracked, orchestrator, spider, storefront, sonar-backend
│   └── vehicle/             # base templates (simulator, whereami, sonar, brakenow)
├── vehicles/
│   ├── vehicle-1/           # namePrefix: vehicle-1-, VIN-001 patches
│   └── vehicle-2/           # namePrefix: vehicle-2-, VIN-002 patches
└── overlays/
    ├── minikube/            # imagePullPolicy: Never + nginx ingress class
    └── gke/                 # GCR image rewrites + GKE ingress annotations + managed cert
```

Render the full manifest without applying (useful for debugging):
```bash
kubectl kustomize k8s/overlays/minikube
kubectl kustomize k8s/overlays/gke | sed "s|gcr.io/PROJECT_ID|gcr.io/$GCP_PROJECT|g"
```

---

## Kubernetes (GKE)

### Prerequisites

1. A GCP project with billing enabled and GKE API active
2. `gcloud` CLI authenticated: `gcloud auth login && gcloud auth application-default login`
3. A GKE Standard cluster (not Autopilot) named `YOUR_CLUSTER` in `europe-west1-b`:
   ```bash
   gcloud container clusters create YOUR_CLUSTER \
     --zone europe-west1-b \
     --num-nodes 3 \
     --machine-type e2-standard-2
   ```
4. A static global IP reserved:
   ```bash
   gcloud compute addresses create d2s-static-ip --global
   ```
5. `kubectl` context pointed at the cluster:
   ```bash
   gcloud container clusters get-credentials YOUR_CLUSTER --zone europe-west1-b
   ```

### Deploy everything

```bash
export GCP_PROJECT=your-gcp-project-id
./k8s/deploy-gke.sh
```

This will:
1. Authenticate Docker with GCR
2. Build and push all images to `gcr.io/$GCP_PROJECT/<service>:latest` (parallel via `docker buildx bake --push`)
3. Apply all manifests via Kustomize (with GCR image rewrites and GKE ingress annotations)
4. Restart all pods so GKE pulls the freshly pushed images
5. Wait for all Deployments and the StatefulSet to be ready in parallel
6. Print the static IP and frontend URL

### Access the frontend

```
https://example.com
```

The GKE ingress uses a Google-managed TLS certificate (`d2s-cert-v2`). Certificate provisioning
takes **10–20 minutes** after the DNS record for `example.com` points to the static IP.
HTTP is also allowed (`kubernetes.io/ingress.allow-http: "true"`) during that window.

Check certificate status:
```bash
kubectl describe managedcertificate d2s-cert-v2
```

### Inspect the cluster

```bash
kubectl get pods                          # all pod statuses
kubectl logs deployment/spider            # service logs
kubectl logs deployment/vehicle-1-sonar   # vehicle service logs
kubectl port-forward deployment/rabbitmq 15672:15672  # RabbitMQ UI → localhost:15672
```

### Tear down

**Remove all resources** (keeps the cluster running, avoids GKE costs from idle pods):
```bash
kubectl delete -k k8s/overlays/gke
```

**Delete the cluster entirely:**
```bash
gcloud container clusters delete YOUR_CLUSTER --zone europe-west1-b
```

> Tear down the cluster when not actively using it — a 3-node cluster costs ~$150/month.