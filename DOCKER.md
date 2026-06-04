# Running PharmaFlow with Docker (Zadatak 2 & 3)

This repo ships a Dockerfile for every microservice, the databases, and the
frontend, plus a `docker-compose.yml` that boots the whole backend in the right
order with working internal communication.

## What's containerized

| Container | Image base | Host port | Notes |
|---|---|---|---|
| postgres | `pgvector/pgvector:pg15` | 5432 | single instance, 5 databases (see `docker/init-db.sql`) |
| rabbitmq | `rabbitmq:3.13-management-alpine` | 5672 / 15672 | saga choreography bus; management UI on 15672 |
| redis | `redis:7-alpine` | 6379 | token blacklist (gateway + user-health) |
| embedding-service | `python:3.11-slim` | 8000 | FastAPI + MiniLM, CPU-only torch |
| eureka-server | `eclipse-temurin:17-jre-alpine` | 8761 | service discovery |
| config-server | `eclipse-temurin:17-jre-alpine` | 8888 | Spring Cloud Config (optional) |
| user-health-service | `eclipse-temurin:17-jre-alpine` | 8081 | |
| product-health-service | `eclipse-temurin:17-jre-alpine` | 8083 | |
| pharmacy-inventory-service | `eclipse-temurin:17-jre-alpine` | 8084 | |
| order-prescription-service | `eclipse-temurin:17-jre-alpine` | 8087 | |
| smart-features-service | `eclipse-temurin:17-jre-alpine` | 8082 | Flyway runs `CREATE EXTENSION vector` |
| api-gateway | `eclipse-temurin:17-jre-alpine` | 8080 | edge / JWT / RBAC |
| frontend | `nginx:alpine` | 3000 | separate compose, see below |

## Memory optimization (Zadatak 2)

- **Multi-stage builds** — a `maven:3.9-eclipse-temurin-17` build stage produces the
  jar; the runtime is the much smaller `eclipse-temurin:17-jre-alpine`.
- **JVM container tuning** baked into each Java image:
  `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseSerialGC
  -XX:MaxMetaspaceSize=128m -Xss512k -XX:+ExitOnOutOfMemoryError`. SerialGC has the
  lowest footprint for small services, and the heap is sized as a % of the
  container limit rather than the host.
- **`mem_limit` per service** in compose, so the JVM sees a real cgroup ceiling.
- **embedding-service** installs **CPU-only torch** (`--index-url .../whl/cpu`)
  instead of the multi-GB CUDA build, with `--no-cache-dir`.
- **frontend** ships only static files on `nginx:alpine` (the Node toolchain stays
  in the discarded build stage).
- Non-root users and `.dockerignore` files keep images small and clean.

## Startup order & internal communication (Zadatak 3)

`depends_on` with `condition: service_healthy` enforces:

```
postgres, rabbitmq, redis, embedding-service   (infra, healthchecked)
        └─> eureka-server, config-server
                └─> user / product / pharmacy / order / smart-features
                        └─> api-gateway
```

Services talk to each other by **compose service name** on the `pharmaflow-net`
bridge network:

- DB:        `jdbc:postgresql://postgres:5432/<db>`
- Eureka:    `http://eureka-server:8761/eureka/`
- RabbitMQ:  `rabbitmq:5672`
- Redis:     `redis:6379`
- Embedding: `http://embedding-service:8000`

The gateway routes to the business services with `lb://SERVICE-NAME` resolved
through Eureka. Every service registers with `eureka.instance.prefer-ip-address=true`,
so the container IPs (routable on the bridge network) are used for load-balanced
calls — no extra configuration required.

## Usage

Backend:

```bash
cd pharmaflow-backend
docker compose up -d --build      # build images and start everything
docker compose ps                 # watch health come up (gateway is last)
docker compose logs -f api-gateway
docker compose down               # stop (add -v to also drop DB/RabbitMQ volumes)
```

Frontend (separate compose, sibling folder):

```bash
cd pharmaflow-frontend
docker compose up -d --build      # serves the SPA on http://localhost:3000
```

Open the app at <http://localhost:3000>. It calls the gateway at
`http://localhost:8080`; the gateway's CORS allow-list already includes
`http://localhost:3000`.

## Configuration overrides

All settings have sensible inline defaults, so no `.env` file is required. To
override (secrets, credentials, CORS, frontend gateway URL), create
`pharmaflow-backend/.env` — see `.env.docker.example`. For the frontend, set
`VITE_GATEWAY_URL` before building (it is baked into the bundle at build time).

## First-run notes

- The very first `up` builds 8 Java images (Maven downloads) and the embedding
  image (downloads torch + the MiniLM model). Expect several minutes; subsequent
  builds are cached.
- The embedding-service healthcheck has a 120s `start_period` because the model
  loads on startup.
- Postgres creates the 5 databases only on the **first** initialization of the
  `postgres-data` volume. If you change `init-db.sql`, run `docker compose down -v`
  to recreate them.
