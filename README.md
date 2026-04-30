# ICS-Guard Next

Next-generation ICS/OT network security platform — real-time anomaly detection and automated threat response for industrial control systems.

## Architecture

Two teams share a message-driven architecture:

| Team | Scope | Stack |
|---|---|---|
| A | Backend + WebUI | Java 21 / Spring Boot 3 + Vue 3 / TypeScript |
| B | Controller + Engine | Python 3 / Ryu + ONNX / LightGBM |

Teams communicate exclusively through **Kafka** (async data plane) and **HTTP REST** (sync control plane). See [CLAUDE.md](CLAUDE.md) for the full architecture and conventions.

### Services

| Service | Purpose |
|---|---|
| `redpanda` | Kafka-compatible message bus |
| `postgres` | Persistent state (Team A) |
| `redis` | Sessions, rate limiting, temp cache |
| `kafka-init` | Auto-creates 6 Kafka topics on startup |
| `app-backend` | REST API, RBAC, policy config, alert management |
| `web-ui` | Dashboard, topology viz, rule editor |
| `sdn-controller` | Flow table management, traffic mirroring |
| `inference-engine` | Three-tier cascade anomaly detection |
| `device-simulator` | ICS device emulation (optional, `--profile simulation`) |

## Quick Start

```bash
# Clone and prepare environment
git clone <repo-url> && cd ICS-guard-next
cp .env.example .env

# Start all services
docker compose up -d

# Start with device simulator
docker compose --profile simulation up -d

# Stop
docker compose down
```

## Local Development

Develop and debug services on your host machine while infrastructure runs in Docker.

### Prerequisites

- **Java 21** — [Eclipse Temurin](https://adoptium.net/) recommended
- **Node.js 22+** — for the Vue frontend
- **Python 3.11+** — for Team B services
- **Docker** — for running infrastructure containers

### Setup

```bash
cp .env.example .env
```

### Start infrastructure

```bash
docker compose up -d redpanda postgres redis kafka-init

# Verify
docker compose ps
```

All four containers should show `healthy` or `Up`.

### Backend (Team A)

```bash
cd app-backend

# First build — download dependencies
./gradlew build -x test

# Start with local profile (host → localhost)
./gradlew bootRun --args='--spring.profiles.active=local'

# Or in your IDE: set spring.profiles.active=local
```

The `local` profile enables: `ddl-auto: update`, SQL logging, DevTools hot reload.

Verify: `curl http://localhost:8080/actuator/health`

### Frontend (Team A)

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`. API requests proxy to `localhost:8080`.

### Team B services

```bash
cd sdn-controller
pip install -r requirements.txt
python main.py
```

```bash
cd inference-engine
pip install -r requirements.txt
python main.py
```

Ports: Controller `8000`, OpenFlow `6633` / `6653`.

### Docker mode

When running a service in Docker instead of locally:

```bash
# Build and start a single service
docker compose up -d --build app-backend
```

Environment variables in Docker use service names (`redpanda`, `postgres`, `redis`). The `.env.example` file provides defaults for both modes.

## Project Structure

```
app-backend/          # Team A — Java Spring Boot
frontend/             # Team A — Vue 3 + Vite
sdn-controller/       # Team B — Python Ryu
inference-engine/     # Team B — Python AI
simulator/            # Team B — Device simulation
models/               # Team B — AI model files
deploy/config/        # Shared — env config samples
docs/                 # Shared — contracts, architecture
legacy/               # Reference only
```

## Git Workflow

- `main` = production-stable
- `dev` = integration branch
- `feature/XXX` = feature branches, merge to `dev` via PR
- No cross-team commits (A doesn't modify `sdn-controller/`, B doesn't modify `app-backend/`)

## Current Phase

Phase 1 — scaffold and development environment. Docker Compose, directory structure, and configuration are in place. Service implementations in progress.
