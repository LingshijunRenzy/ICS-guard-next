# ICS-Guard Next

ICS/OT network security platform — real-time anomaly detection and automated threat response for industrial control systems. Rewrite from scratch; legacy code in `legacy/` is reference-only.

## Architecture

**Two-team boundary**: Team A (Java Backend + Vue3 WebUI + data infrastructure) ↔ Team B (Python Ryu Controller + AI Inference Engine + Device Simulator). Teams communicate exclusively through Kafka (async data plane) and HTTP REST (sync control plane).

### 9 Docker services (8 default + 1 optional)

| Service | Team | Stack | Purpose |
|---|---|---|---|
| `redpanda` | Shared | Redpanda v24 (Kafka-compatible) | Message bus |
| `postgres` | A | PostgreSQL 16 | Persistent state (owned by Team A) |
| `redis` | A | Redis 7 | Sessions, rate limiting, temp cache |
| `kafka-init` | Shared | Redpanda CLI | Auto-creates 6 Kafka topics on startup |
| `app-backend` | A | Java 17/21 + Spring Boot 3 | REST API, RBAC, policy config, alert management |
| `web-ui` | A | Vue 3 + Vite + TypeScript | Dashboard, topology viz, rule editor |
| `sdn-controller` | B | Python 3 + Ryu + OpenFlow 1.3 | Flow table management, traffic mirroring, feature extraction |
| `inference-engine` | B | Python 3 + ONNX/LightGBM | Three-tier cascade anomaly detection (<15ms target) |
| `device-simulator` | B (optional) | Python 3 + pymodbus + scapy | ICS device emulation and traffic generation (`--profile simulation`) |

### Data plane vs Control plane

- **Kafka (async high-speed data)**: Controller → Engine (traffic features), Engine → Backend (threat alerts), Controller → Backend (topology events, traffic metrics), Backend → Engine (model events), Backend → External (audit logs)
- **HTTP REST (sync control)**: Backend → Controller (flow block/revoke, rule push), WebUI ↔ Backend (CRUD, dashboard)

### 6 Kafka topics

| Topic | Direction | Retention |
|---|---|---|
| `ics.traffic.features` | Controller → Engine | 1h |
| `ics.threat.alerts` | Engine → Backend | 7d |
| `ics.topology.events` | Controller → Backend | 7d |
| `ics.traffic.metrics` | Controller → Backend | 1h |
| `ics.model.events` | Backend → Engine | 30d |
| `ics.audit.logs` | Backend → External | 90d |

## Project structure

```
app-backend/          # Team A — Java Spring Boot
  src/main/java/
  src/main/resources/
frontend/             # Team A — Vue 3 + Vite
  src/
sdn-controller/       # Team B — Python Ryu
  ryu_apps/
  tests/
inference-engine/     # Team B — Python AI
  consumer/
  core/
  models/
simulator/            # Team B — Python device emulation
  devices/
  scenarios/
  src/
models/               # Team B — AI model files & thresholds
deploy/
  config/             # Shared — env config samples
  k8s/                # Shared — Helm charts (future)
docs/                 # Shared — contracts, architecture, conventions
legacy/               # Reference only — NOT built or imported
```

## Key conventions

- **Contract-first**: Define API/Kafka schemas in `docs/` before coding. Review by both teams.
- **DB isolation**: PostgreSQL owned by Team A. Team B Python components NEVER connect directly — use Kafka or HTTP API.
- **Model isolation**: AI models owned by Team B. Team A only consumes alert results.
- **Timestamps**: Always Unix millisecond int64 or UTC ISO8601. No local timezone strings.
- **Trace IDs**: Every traffic feature and alert must carry a globally unique `trace_id` (UUID) from the capture point.
- **Config via env vars**: No hardcoded addresses, ports, or secrets. `.env.example` defines all shared vars.
- **Response format**: All HTTP APIs use `{ code, msg, data }` envelope. `code` = HTTP status code.

## Development workflow

```bash
# Copy and customize env
cp .env.example .env

# Start core services (8 containers)
docker compose up -d

# Start with device simulator
docker compose --profile simulation up -d

# Stop all
docker compose down
```

Services bootstrap in dependency order: Redpanda → kafka-init (creates topics) → postgres/redis → app-backend → web-ui, sdn-controller, inference-engine.

## Git workflow

- `main` = production-stable, `dev` = integration
- Team A and B work on `feature/XXX` branches, merge via PR to `dev`
- No cross-team commits (A doesn't modify `sdn-controller/`, B doesn't modify `app-backend/`)

## Current phase

Phase 1 (scaffold + dev environment) — docker-compose.yml, .env.example, and directory scaffold are complete. Service Dockerfiles and source code not yet implemented.
