# Java Ecommerce — WebFlux / R2DBC Performance Study

Modular monolith e-commerce API built with **Java 17**, **Spring Boot 3.2**, **WebFlux** and **R2DBC**, designed to measure request latency and **thread efficiency** after moving from blocking Spring MVC + JPA to a fully reactive stack (`Mono` / `Flux`).

Repository: [noellepaes/webflux-ecommerce-java](https://github.com/noellepaes/webflux-ecommerce-java)

Related studies:

| Repo | Stack | Goal |
|------|--------|------|
| [java-ecommerce-completable-future](https://github.com/noellepaes/java-ecommerce-completable-future) | MVC + JPA + `CompletableFuture` | Parallelize independent I/O on Tomcat |
| **this repo** | **WebFlux + R2DBC + Reactive Redis** | Non-blocking I/O end-to-end, fewer threads under load |

---

## What this project does

Modular monolith organized by DDD Bounded Contexts:

| Module | Responsibility |
|--------|----------------|
| product | Catalog, stock, product CRUD |
| customer | Customers |
| order | Orders, items, pay/cancel flow |
| payment | Payment processing |
| auth | Login and user listing |
| recommendation | Collaborative recommendations (Redis) |
| shared | Cross-cutting types only |

Each module: `domain` → `application` (use cases) → `infrastructure` → `presentation` (REST).

### Architecture highlights

- Single executable Spring Boot app on **Netty** (no Tomcat)
- PostgreSQL with separate schemas per context + Flyway (JDBC for migrations only)
- **R2DBC** for reactive persistence; **Reactive Redis** for co-view graphs
- Modules reference each other by UUID (microservice-ready)
- Prometheus + Grafana + k6 for load testing

### Goal of this repo

Compare **latency** and especially **JVM thread usage** against:

1. Synchronous MVC + JPA (baseline from the CF study)
2. MVC + JPA + `CompletableFuture` (same suite)

WebFlux wins concurrency by using a small event-loop instead of one servlet thread per request — not by making bcrypt or a single SQL query magically faster.

---

## Tech stack

- Java 17 · Spring Boot 3.2 · **Spring WebFlux** · **Spring Data R2DBC** · Spring Data Redis Reactive
- PostgreSQL 15 · Flyway (JDBC) · Redis 7
- Drivers: `org.postgresql:r2dbc-postgresql` + `r2dbc-pool` (reactive) · `org.postgresql:postgresql` (Flyway)
- Micrometer · Prometheus · Grafana · Docker Compose · k6 · SpringDoc OpenAPI (WebFlux UI)

---

## Project structure

```
src/main/java/com/ecommerce/
 ├── auth/
 ├── product/
 ├── customer/
 ├── order/
 ├── payment/
 ├── recommendation/
 ├── config/
 └── shared/

load-tests/
 ├── k6/scenarios/     # one script per endpoint (+ checkout + stress)
 ├── run-suite.ps1      # sequential suite → compact summary in results/
 ├── run-stress.ps1     # dense Redis graph + high VU (products / recommendations)
 └── results/          # compact suite-*.txt / stress-*.txt only (no raw .log dumps)

monitoring/
docs/
```

---

## Quick start

```bash
docker compose up -d --build
```

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (`admin` / `admin`) |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

Seed DEV users (password `123456`): `lebron.seed@dev.local`, `noelle.seed@dev.local`, `daniel.seed@dev.local`

---

## Load tests (k6)

```powershell
cd load-tests
.\run-suite.ps1 -Vus 50 -Duration "30s"

# Optional: high concurrency + dense Redis graph (recommendations + products)
.\run-stress.ps1 -Vus 200 -Duration "45s" -GraphPeers 100
```

Grafana load-test dashboard: http://localhost:3000/d/ecommerce-load-test

Results kept as **compact summaries** under `load-tests/results/` (full k6 dumps are not committed).

---

## Thread usage (main WebFlux win)

Measured via `/actuator/metrics/jvm.threads.live` and `jvm.threads.peak` on the running container.

### Suite (50 VUs, mixed endpoints)

| Moment | Live threads | Peak threads |
|--------|--------------|--------------|
| Idle (before suite) | **43** | 46 |
| Mid-suite (under load) | **88** | **89** |
| After suite | **39** | **89** |

### Stress (I/O endpoints, high concurrency) — 2026-07-21

| Scenario | VUs | p95 | RPS | Threads live / peak |
|----------|-----|-----|-----|---------------------|
| GET recommendations (DevSeed only) | 50 | 659 ms | 109 | ~40 / 41 |
| GET recommendations (**dense Redis graph**, 100 peers) | 200 | 39.3 s | 12.5 | ~39 / 41 |
| GET /api/products (suite baseline) | 50 | 181 ms | 278 | ~39 / 41 |
| GET /api/products stress | 200 | 697 ms | 470 | ~40 / 41 |
| GET /api/products stress | **500** | 1.47 s | 474 | **~39 / 41** |

Report: `load-tests/results/stress-20260721-142925.txt`  
Runner: `.\load-tests\run-stress.ps1 -Vus 200 -Duration "45s" -GraphPeers 100`

Dense-graph recommendations get slower (expected fan-out). **Thread count does not grow with VUs** — that is the WebFlux signal.

### Why fewer threads matters (is it cheaper?)

Yes — usually in **infra cost and headroom**, not because each HTTP call becomes cheaper:

| Effect | Why it saves money / risk |
|--------|---------------------------|
| **More connections per GB of RAM** | Each platform thread costs ~1 MB stack (+ objects). Hundreds of Tomcat threads → larger heap/RSS → bigger instance or earlier OOM. |
| **Fewer / smaller JVM replicas** | Same concurrent clients on a smaller VM (or more tenants per node) → lower cloud bill when I/O-bound. |
| **Less context-switch tax** | OS schedules fewer threads → more predictable under traffic spikes. |
| **Not a magic CPU discount** | bcrypt, big SQL, Redis fan-out still cost the same CPU/network. Login p95 can even look worse if the elastic pool is small. |

**Rule of thumb:** WebFlux pays off when you are paying for **idle blocked threads waiting on I/O** at high concurrency. It does **not** make a 3 ms JPA `SELECT` faster.

### Why this matters vs MVC

| Model | Typical behaviour under concurrent clients |
|-------|-----------------------------------------------|
| **Spring MVC (Tomcat)** | ~1 request thread blocked per in-flight call → pool grows toward `server.tomcat.threads.max` (often **200**) |
| **MVC + CompletableFuture** | Request thread may return earlier, but Tomcat + `ioTaskExecutor` still hold **many** platform threads |
| **WebFlux (Netty)** | Small event-loop; stress above stayed **~40 threads at 500 VUs** on `GET /api/products` |

**Takeaway:** under the same client pressure, WebFlux keeps a flat thread budget instead of scaling a large servlet pool. That is the primary scalability / cost-density win of this migration.

---

## Load test results — WebFlux (this repo)

**Conditions:** 50 VUs · 30s per scenario · Docker Compose · k6  
**Report:** `load-tests/results/suite-20260720-095710.txt`  
**Notes:** R2DBC pool `max-size=50`; orders/payments truncated before the suite; `orders-add-item` creates **one order per iteration** (avoids `@Version` conflicts).

p95 = 95th percentile of `http_req_duration`.

| Module | Endpoint | Reqs | RPS | p95 | Failures | Checks |
|--------|----------|------|-----|-----|----------|--------|
| Auth | POST /api/auth/login | 220 | 6.86 | **10.84 s** | 0.00% | 100.00% |
| Auth | GET /api/auth/users | 2,559 | 83.86 | 1.02 s | 0.00% | 100.00% |
| Product | GET /api/products | 8,006 | 265.97 | 253.93 ms | 0.00% | 100.00% |
| Product | GET /api/products/{id} | 10,996 | 361.71 | 114.28 ms | 0.00% | 100.00% |
| Customer | GET /api/customers | 12,914 | 429.07 | 56.09 ms | 0.00% | 100.00% |
| Customer | GET /api/customers/{id} | 12,524 | 412.47 | 68.37 ms | 0.00% | 100.00% |
| Order | GET /api/orders/customer/{id} | 10,808 | 356.16 | 130.63 ms | 0.00% | 100.00% |
| Order | POST /api/orders | 8,394 | 273.93 | 195.91 ms | 0.00% | 100.00% |
| Order | POST /api/orders/{id}/items | 8,980 | 296.04 | 276.74 ms | 0.00% | 100.00% |
| Order | POST /api/orders/{id}/pay | 10,229 | 334.55 | 241.67 ms | 0.00% | 100.00% |
| Payment | POST /api/payments | 11,684 | 383.84 | 211.55 ms | 0.00% | 100.00% |
| Redis | GET /api/recommendations/customers/{id} | 6,447 | 211.99 | 359.33 ms | 0.00% | 100.00% |
| Redis | POST /api/recommendations/.../views | 11,342 | 374.44 | 107.17 ms | 0.00% | 100.00% |
| Checkout | order + item + payment | 10,043 | 325.90 | 254.99 ms | 0.00% | 100.00% |

Login p95 is dominated by **bcrypt** on `boundedElastic` (same cost as MVC; fewer concurrent hashes → lower RPS under WebFlux’s smaller thread budget).

---

## Comparison: Sync → CompletableFuture → WebFlux

Baselines for **Sync** and **CF** come from [java-ecommerce-completable-future](https://github.com/noellepaes/java-ecommerce-completable-future) (same k6 shape: 50 VUs · 30s).  
**WebFlux** column = this run (`suite-20260720-095710`).

| Endpoint | Sync p95 | CF p95 | WebFlux p95 | Notes |
|----------|----------|--------|-------------|--------|
| POST /api/recommendations/.../views | 4.85 ms | **3.56 ms (−27%)** | 107.17 ms | CF overlaps Redis writes; WebFlux still non-blocking but R2DBC/Redis + Docker Desktop add overhead vs that baseline host |
| GET /api/recommendations/customers/{id} | 4.52 ms | 8.58 ms (+90%) | 359.33 ms | Fan-out / hydration cost; CF already slower than sync on small seed |
| GET /api/auth/users | 5.01 ms | 5.87 ms | 1.02 s* | *Antes do fix: N+1 (`findAll` users + `findByEmail` por user). Agora: 2 queries (users + `IN` emails). Login **não** é N+1. |
| GET /api/products | 3.41 ms | — | 253.93 ms | Simple read: MVC/JPA was already very fast |
| GET /api/customers/{id} | 3.23 ms | — | 68.37 ms | Best WebFlux CRUD p95 in this suite |
| POST /api/auth/login | 470.31 ms | — | 10.84 s | bcrypt-bound; WebFlux limits parallel CPU-bound hashes |
| Threads under ~50 VU load | Tomcat pool (often ≫ 100) | Tomcat + executor | **peak ≈ 89** | Clearest WebFlux advantage |
| Threads @ 500 VU (`GET /products`) | (Tomcat would grow with load) | — | **~40 live / 41 peak** | Stress run 2026-07-21; density win |

### How to read the comparison

1. **Threads ↓** — WebFlux is the winner: ~**89 peak** on the mixed suite, and **~40 threads at 500 VUs** on `GET /api/products` stress.
2. **Latency** — On this machine (Docker Desktop / Windows), absolute WebFlux p95 for simple CRUD did **not** beat the published Sync/CF numbers from the sibling study. Reactive stacks shine when you need **many concurrent connections with little memory/threads**, not when each request is a single tiny SQL round-trip already < 5 ms on JPA.
3. **CF vs WebFlux** — CF improves *selected* parallel I/O paths inside MVC. WebFlux changes the **server concurrency model** for every endpoint.
4. **Cost** — Fewer threads → less RAM per concurrent client → often **cheaper instances / higher density**, not cheaper CPU per request.

---

## What changed vs MVC/JPA

| Layer | Before (MVC study) | This repo |
|-------|--------------------|-----------|
| HTTP | Spring MVC / Tomcat | **WebFlux / Netty** |
| Persistence | Spring Data JPA | **Spring Data R2DBC** + adapters |
| Redis | `StringRedisTemplate` | **`ReactiveStringRedisTemplate`** |
| API returns | `T` / `List` / `ResponseEntity` | **`Mono` / `Flux`** |
| Flyway | JDBC | JDBC (unchanged; required) |

Order aggregate: `OrderRepositoryAdapter` loads/saves `order_items` reactively (no JPA cascade).

---

## Extra docs

- `docs/ARQUITETURA.md`
- `docs/DECISOES_ARQUITETURAIS.md`
- `docs/GUIA_RAPIDO.md`
