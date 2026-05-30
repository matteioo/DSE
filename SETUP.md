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

## RabbitMQ Messaging Structure

Management UI: http://localhost:15672 (credentials: `dse` / `dse`)

### Exchanges

| Exchange    | Type    | Description                  |
|-------------|---------|------------------------------|
| `vehicle.gps` | direct | GPS positions from WHEREAMI |

### Queues & Bindings

| Queue         | Exchange      | Routing Key | Consumer  |
|---------------|---------------|-------------|-----------|
| `utracked.gps` | `vehicle.gps` | `gps`       | UTRACKED  |

### Channel names (application.properties)

| Service   | Direction | Channel name     | Exchange      | Routing Key |
|-----------|-----------|------------------|---------------|-------------|
| WHEREAMI  | outgoing  | `vehicle-gps`    | `vehicle.gps` | `gps`       |
| UTRACKED  | incoming  | `vehicle-position` | `vehicle.gps` | `gps`       |

> Channel names are internal SmallRye identifiers (`mp.messaging.incoming/outgoing.<name>`).
> They do not need to match between services — only the exchange and routing key must align.

### Adding a new channel

1. Add to `application.properties`:
   ```properties
   mp.messaging.outgoing.<channel-name>.connector=smallrye-rabbitmq
   mp.messaging.outgoing.<channel-name>.exchange.name=<exchange>
   mp.messaging.outgoing.<channel-name>.routing-key=<key>
   ```
2. Annotate the publisher method with `@Outgoing("<channel-name>")`.
3. Update the table above.

---

## Kubernetes

_To be documented._