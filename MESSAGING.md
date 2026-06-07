# RabbitMQ Messaging Schema

This document defines the naming conventions and topology for all RabbitMQ exchanges,
queues, routing keys, and Quarkus channel names used across the D2S services.

---

## Message Flows

| Publisher | Exchange | Consumers | Content |
|---|---|---|---|
| WHEREAMI | `d2s.vehicle.gps` | UTRACKED | GPS readings (per VIN, on schedule) |
| SONAR (vehicle) | `d2s.vehicle.distance` | BRAKENOW (same vehicle), ORCHESTRATOR, SONAR (backend) | Distance + change-rate readings |
| BRAKENOW | `d2s.vehicle.brake` | ORCHESTRATOR | Brake state changes |
| BRAKENOW | `d2s.simulator.brake` | SIMULATOR | Brake commands to update simulated speed |
| ORCHESTRATOR | `d2s.vehicle.brake` | BRAKENOW, ORCHESTRATOR itself | Backend-triggered brake events |
| SIMULATOR | `d2s.simulator.vehicle-state` | SIMULATOR (peer) | Vehicle state for peer distance calculation |
| Storefront (STOMP/WS) | `d2s.simulator.scenario` | SIMULATOR, BRAKENOW, ORCHESTRATOR, UTRACKED | Scenario control commands |

Spider has no RabbitMQ traffic. Storefront publishes scenario commands directly to RabbitMQ
via STOMP over WebSocket (`ws://<host>/stomp`), proxied by nginx to `rabbitmq:15674/ws`.
This requires the `rabbitmq_web_stomp` plugin to be enabled on the broker.

ORCHESTRATOR does **not** consume GPS readings from RabbitMQ. It calls UTRACKED's REST API
synchronously when evaluating condition 4 (plausibility check).

---

## Naming Conventions

| Artifact | Pattern | Example |
|---|---|---|
| Exchange | `d2s.<domain>.<entity>` | `d2s.vehicle.gps` |
| Queue | `<consumer-service>.<entity>[.<vehicle-id>]` | `utracked.gps`, `brakenow.sonar.vehicle-1` |
| Routing key | `<entity>` or `<entity>.<vin>` | `gps`, `distance.D2S-DEMO-VIN-001` |

All vehicle exchanges are **topic** type. Simulator exchanges use **fanout** (scenario) or default (state/brake).

---

## Sign Convention for changeRateMps

**Positive = vehicles approaching (distance decreasing)**
**Negative = vehicles moving apart (distance increasing)**

```
condition 1: distance < 30 AND changeRateMps > 4
condition 2: distance < 15 AND changeRateMps > 2
condition 3: distance < 5  AND changeRateMps > 0
```

---

## Exchanges

### `d2s.vehicle.gps` — direct

Published by: **WHEREAMI** · Routing key: `gps`

| Queue | Consumer | Binding | Channel |
|---|---|---|---|
| `utracked.gps` | UTRACKED | `gps` | `vehicle-position` |

---

### `d2s.vehicle.distance` — topic

Published by: **SONAR (vehicle instances)** · Routing key: `distance.<vin>`

| Queue | Consumer | Binding | Channel |
|---|---|---|---|
| `brakenow.sonar.<vehicle-id>` | BRAKENOW | `distance.<own-vin>` | `vehicle-sonar` |
| `orchestrator.distance` | ORCHESTRATOR | `distance.#` | `vehicle-distance` |
| `sonar.distance.backend` | SONAR (backend) | `distance.#` | `backend-vehicle-distance` |

Each BRAKENOW instance binds only its own VIN's distance readings.
SONAR backend consumes all distance readings (no VIN filter) for aggregation.

---

### `d2s.vehicle.brake` — topic

Published by: **BRAKENOW** and **ORCHESTRATOR** · Routing key: `brake`

| Queue | Consumer | Binding | Channel |
|---|---|---|---|
| `orchestrator.brake` | ORCHESTRATOR | `brake.#` | `brake-in` |
| `brakenow.brake.<vehicle-id>` | BRAKENOW | `${BRAKENOW_BRAKE_ROUTING_KEY}` | `brake-in` |

BRAKENOW publishes (`brake-out`) when brake conditions 1–3 are met locally.
ORCHESTRATOR publishes (`brake-out`) when condition 4 (plausibility check) is met.
Both ORCHESTRATOR and BRAKENOW also consume from this exchange to stay in sync.

---

### `d2s.simulator.brake` — default

Published by: **BRAKENOW** · Routing key: `brake`

| Queue | Consumer | Binding | Channel |
|---|---|---|---|
| `simulator.brakenow.brake.<vehicle-id>` | SIMULATOR | `${SIMULATOR_BRAKENOW_BRAKE_ROUTING_KEY}` | `brakenow-brake-in` |

Separate from `d2s.vehicle.brake` — used specifically to signal the simulator to
update the simulated vehicle's speed (stop on brake, resume on clear).

---

### `d2s.simulator.vehicle-state` — default

Published by: **SIMULATOR** · Routing key: `state`

| Queue | Consumer | Binding | Channel |
|---|---|---|---|
| `simulator.state.<vehicle-id>` | SIMULATOR (peer) | `state` | `simulator-state-in` |

Both simulator instances publish their state and consume the peer's state.
Used to calculate relative distance between vehicles.

---

### `d2s.simulator.scenario` — fanout

Published by: Spider REST endpoint (via RabbitMQ publish).

| Queue | Consumer | Channel |
|---|---|---|
| `simulator.scenario.<vehicle-id>` | SIMULATOR | `scenario-command` |
| `brakenow.scenario.<vehicle-id>` | BRAKENOW | `scenario-command` |
| `orchestrator.scenario` | ORCHESTRATOR | `scenario-command` |
| `utracked.scenario` | UTRACKED | `scenario-command` |

Fanout — all bound queues receive every scenario command regardless of routing key.

---

## Quarkus Channel Mapping

### WHEREAMI

```properties
mp.messaging.outgoing.vehicle-gps.connector=smallrye-rabbitmq
mp.messaging.outgoing.vehicle-gps.exchange.name=d2s.vehicle.gps
mp.messaging.outgoing.vehicle-gps.routing-key=gps
```

### UTRACKED

```properties
mp.messaging.incoming.vehicle-position.connector=smallrye-rabbitmq
mp.messaging.incoming.vehicle-position.exchange.name=d2s.vehicle.gps
mp.messaging.incoming.vehicle-position.queue.name=utracked.gps
mp.messaging.incoming.vehicle-position.routing-key=gps

mp.messaging.incoming.scenario-command.connector=smallrye-rabbitmq
mp.messaging.incoming.scenario-command.exchange.name=d2s.simulator.scenario
mp.messaging.incoming.scenario-command.exchange.type=fanout
mp.messaging.incoming.scenario-command.queue.name=${UTRACKED_SCENARIO_QUEUE_NAME:utracked.scenario}
mp.messaging.incoming.scenario-command.routing-key=scenario
```

### SONAR (vehicle mode)

```properties
mp.messaging.outgoing.vehicle-sonar.connector=smallrye-rabbitmq
mp.messaging.outgoing.vehicle-sonar.exchange.name=d2s.vehicle.distance
mp.messaging.outgoing.vehicle-sonar.exchange.type=topic
mp.messaging.outgoing.vehicle-sonar.routing-key=distance.${sonar.vin}
```

### SONAR (backend mode)

```properties
mp.messaging.incoming.backend-vehicle-distance.enabled=${SONAR_BACKEND_DISTANCE_CONSUMER_ENABLED:false}
mp.messaging.incoming.backend-vehicle-distance.connector=smallrye-rabbitmq
mp.messaging.incoming.backend-vehicle-distance.exchange.name=d2s.vehicle.distance
mp.messaging.incoming.backend-vehicle-distance.queue.name=${SONAR_BACKEND_DISTANCE_QUEUE_NAME:sonar.distance.backend}
mp.messaging.incoming.backend-vehicle-distance.routing-key=distance.#
```

### BRAKENOW

```properties
# Incoming — distance readings for own vehicle only
mp.messaging.incoming.vehicle-sonar.connector=smallrye-rabbitmq
mp.messaging.incoming.vehicle-sonar.exchange.name=d2s.vehicle.distance
mp.messaging.incoming.vehicle-sonar.exchange.type=topic
mp.messaging.incoming.vehicle-sonar.queue.name=${BRAKENOW_SONAR_QUEUE_NAME:brakenow.sonar.local}
mp.messaging.incoming.vehicle-sonar.routing-key=distance.${brakenow.vin}

# Outgoing — brake state changes to orchestrator
mp.messaging.outgoing.brake-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.brake-out.exchange.name=d2s.vehicle.brake
mp.messaging.outgoing.brake-out.exchange.type=topic
mp.messaging.outgoing.brake-out.routing-key=brake

# Incoming — brake events (own and backend-triggered) for sync
mp.messaging.incoming.brake-in.connector=smallrye-rabbitmq
mp.messaging.incoming.brake-in.exchange.name=d2s.vehicle.brake
mp.messaging.incoming.brake-in.exchange.type=topic
mp.messaging.incoming.brake-in.queue.name=${BRAKENOW_BRAKE_QUEUE_NAME:brakenow.brake.local}
mp.messaging.incoming.brake-in.routing-key=${BRAKENOW_BRAKE_ROUTING_KEY:brake.#}

# Outgoing — brake signal to simulator (separate exchange)
mp.messaging.outgoing.simulator-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.simulator-out.exchange.name=d2s.simulator.brake
mp.messaging.outgoing.simulator-out.routing-key=brake

# Incoming — scenario commands
mp.messaging.incoming.scenario-command.connector=smallrye-rabbitmq
mp.messaging.incoming.scenario-command.exchange.name=d2s.simulator.scenario
mp.messaging.incoming.scenario-command.exchange.type=fanout
mp.messaging.incoming.scenario-command.queue.name=${BRAKENOW_SCENARIO_QUEUE_NAME:brakenow.scenario.local}
mp.messaging.incoming.scenario-command.routing-key=scenario
```

### ORCHESTRATOR

```properties
# Incoming — distance readings from all vehicles
mp.messaging.incoming.vehicle-distance.connector=smallrye-rabbitmq
mp.messaging.incoming.vehicle-distance.exchange.name=d2s.vehicle.distance
mp.messaging.incoming.vehicle-distance.exchange.type=topic
mp.messaging.incoming.vehicle-distance.queue.name=orchestrator.distance
mp.messaging.incoming.vehicle-distance.routing-key=distance.#

# Outgoing — brake events triggered by condition 4
mp.messaging.outgoing.brake-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.brake-out.exchange.name=d2s.vehicle.brake
mp.messaging.outgoing.brake-out.exchange.type=topic
mp.messaging.outgoing.brake-out.routing-key=brake

# Incoming — brake events for persistence and event log
mp.messaging.incoming.brake-in.connector=smallrye-rabbitmq
mp.messaging.incoming.brake-in.exchange.name=d2s.vehicle.brake
mp.messaging.incoming.brake-in.exchange.type=topic
mp.messaging.incoming.brake-in.queue.name=orchestrator.brake
mp.messaging.incoming.brake-in.routing-key=brake.#

# Incoming — scenario commands
mp.messaging.incoming.scenario-command.connector=smallrye-rabbitmq
mp.messaging.incoming.scenario-command.exchange.name=d2s.simulator.scenario
mp.messaging.incoming.scenario-command.exchange.type=fanout
mp.messaging.incoming.scenario-command.queue.name=${ORCHESTRATOR_SCENARIO_QUEUE_NAME:orchestrator.scenario}
mp.messaging.incoming.scenario-command.routing-key=scenario
```

### SIMULATOR

```properties
# Outgoing — publish own vehicle state for peer distance calculation
mp.messaging.outgoing.simulator-state-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.simulator-state-out.exchange.name=d2s.simulator.vehicle-state
mp.messaging.outgoing.simulator-state-out.routing-key=state

# Incoming — peer vehicle state
mp.messaging.incoming.simulator-state-in.connector=smallrye-rabbitmq
mp.messaging.incoming.simulator-state-in.exchange.name=d2s.simulator.vehicle-state
mp.messaging.incoming.simulator-state-in.queue.name=${SIMULATOR_STATE_QUEUE_NAME:simulator.state.local}
mp.messaging.incoming.simulator-state-in.routing-key=state

# Incoming — scenario commands
mp.messaging.incoming.scenario-command.connector=smallrye-rabbitmq
mp.messaging.incoming.scenario-command.exchange.name=d2s.simulator.scenario
mp.messaging.incoming.scenario-command.exchange.type=fanout
mp.messaging.incoming.scenario-command.queue.name=${SIMULATOR_SCENARIO_QUEUE_NAME:simulator.scenario.local}
mp.messaging.incoming.scenario-command.routing-key=${SIMULATOR_SCENARIO_ROUTING_KEY:scenario}

# Incoming — brake commands from BRAKENOW to update simulated speed
mp.messaging.incoming.brakenow-brake-in.connector=smallrye-rabbitmq
mp.messaging.incoming.brakenow-brake-in.exchange.name=d2s.simulator.brake
mp.messaging.incoming.brakenow-brake-in.queue.name=${SIMULATOR_BRAKENOW_BRAKE_QUEUE_NAME:simulator.brakenow.brake.local}
mp.messaging.incoming.brakenow-brake-in.routing-key=${SIMULATOR_BRAKENOW_BRAKE_ROUTING_KEY:brake.D2S-DEMO-VIN-001}
```

---

## Payload Schemas

### GPS reading (`d2s.vehicle.gps`)

Published by WHEREAMI. Consumed by UTRACKED.

```json
{
  "vin": "D2S-DEMO-VIN-001",
  "latitude": 48.2093723,
  "longitude": 16.3562983,
  "timestamp": "2026-06-03T10:15:30Z"
}
```

---

### Distance reading (`d2s.vehicle.distance`)

Published by SONAR (vehicle instances). Consumed by BRAKENOW, ORCHESTRATOR, SONAR (backend).

```json
{
  "vin": "D2S-DEMO-VIN-001",
  "distanceMeters": 38.5,
  "changeRateMps": 2.3,
  "direction": "FRONT",
  "timestamp": "2026-06-03T10:15:30Z"
}
```

`changeRateMps` is positive when vehicles are approaching (distance decreasing).

Brake condition evaluation:
- Condition 1: `distanceMeters < 30 AND changeRateMps > 4`
- Condition 2: `distanceMeters < 15 AND changeRateMps > 2`
- Condition 3: `distanceMeters < 5  AND changeRateMps > 0`
- Pre-emergency: `distanceMeters < 45`

---

### Brake event (`d2s.vehicle.brake` and `d2s.simulator.brake`)

Published by BRAKENOW and ORCHESTRATOR. Consumed by ORCHESTRATOR, BRAKENOW, and SIMULATOR.

```json
{
  "vin": "D2S-DEMO-VIN-001",
  "active": true,
  "eventType": "EMERGENCY_BRAKE",
  "conditionTriggered": 2,
  "distanceAtTrigger": 12.3,
  "changeRateAtTrigger": 3.1,
  "source": "BRAKENOW",
  "timestamp": "2026-06-03T10:15:30Z"
}
```

| Field | Values | Description |
|---|---|---|
| `active` | `true` / `false` | `true` = brake triggered, `false` = brake cleared |
| `eventType` | `EMERGENCY_BRAKE`, `PRE_EMERGENCY_ENTER`, `PRE_EMERGENCY_EXIT` | Type of state change |
| `conditionTriggered` | `1`, `2`, `3`, `4`, `null` | Which condition triggered. `null` for pre-emergency events |
| `source` | `BRAKENOW`, `ORCHESTRATOR` | Who triggered the brake |

---

### Vehicle state (`d2s.simulator.vehicle-state`)

Published by SIMULATOR instances. Consumed by the peer SIMULATOR instance.

```json
{
  "vin": "D2S-DEMO-VIN-001",
  "position": 1,
  "speedMps": 8.3,
  "timestamp": "2026-06-03T10:15:30Z"
}
```

Used internally by simulators to calculate the relative distance between vehicles.

---

## Interaction Between ORCHESTRATOR and BRAKENOW

```
SONAR (vehicle) ──d2s.vehicle.distance──► BRAKENOW  ──d2s.vehicle.brake──► ORCHESTRATOR (persists)
                                              │
                                              └──d2s.simulator.brake──► SIMULATOR (stop/resume)

SONAR (vehicle) ──d2s.vehicle.distance──► ORCHESTRATOR ──d2s.vehicle.brake──► BRAKENOW (condition 4)
                                              │
                                              └── calls UTRACKED REST for GPS plausibility check
```

- Conditions 1–3: evaluated in parallel by BRAKENOW (vehicle) and ORCHESTRATOR (backend)
- Condition 4: evaluated only by ORCHESTRATOR (requires UTRACKED GPS data via REST)
- BRAKENOW signals SIMULATOR via `d2s.simulator.brake` (separate from the vehicle brake exchange)