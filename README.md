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
| auth | User listing (`GET /users`; login adiado) |
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

Seed DEV customers: `lebron.seed@dev.local`, `noelle.seed@dev.local`, `daniel.seed@dev.local`  
(`POST /api/auth/login` removido temporariamente — usuários seed ainda existem para `GET /api/auth/users`.)

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

## Product reads (WebFlux)

`GET /api/products` e `GET /api/products/{id}` ficam no `ProductController` (`@GetMapping`), com `GetProductUseCase` em R2DBC **sem** `@Transactional` no read.

Writes (`POST`/`PUT`/`decrease-stock`) também no mesmo controller.

### Benchmark products (50 VUs · 30s · pause 0.1 · Docker Compose · app quente · 2026-07-22)

Relatório: `load-tests/results/products-warm-20260722-102827.txt`

| Endpoint | Reqs | RPS | p95 | Avg | Fail |
|----------|------|-----|-----|-----|------|
| `GET /api/products` | 13 722 | **456** | **22.96 ms** | 8.19 ms | 0% |
| `GET /api/products/{id}` | 12 663 | **420** | **48.07 ms** | 16.11 ms | 0% |

(Comparação antiga RouterFunction 2026-07-21: list p95 147 ms / by-id 55 ms — este run no `@RestController` quente ficou bem melhor neste host.)

```
Cliente → Netty → ProductController → GetProductUseCase → R2DBC (Flux/Mono)
```

---

## Orders + recommendations — batch (equivalente ao EntityGraph)

R2DBC **não tem** `@EntityGraph`. O equivalente é **1 query de pais + 1 query `IN` dos filhos** (ou `findByIdIn` no catálogo).

### Comparação limpa A/B (2026-07-21)

Mesmas condições nos dois commits: truncate orders/payments, Redis flush + reseed, **30 pedidos × 2 itens** + 3 views (Noelle), k6 **50 VUs · 30s · pause 0.1**.  
Script: `load-tests/run-ab-batch.ps1` · relatório: `load-tests/results/ab-batch-20260721-165016.txt`  
BEFORE = `89f1807` (N+1 / `concatMap`) · AFTER = `856b3a7` (batch).

| Endpoint | BEFORE (N+1) | AFTER (batch) | Delta |
|----------|--------------|---------------|-------|
| `GET /api/orders/customer/{id}` | p95 **3.77 s** · RPS **27** | p95 **933 ms** · RPS **109** | **∼4× mais rápido** · **∼4× RPS** |
| `GET /api/recommendations/customers/{id}` | p95 **614 ms** · RPS **131** | p95 **523 ms** · RPS **145** | **−15% p95** · +11% RPS |

Conclusão: com **volume realista de pedidos**, o batch `IN` melhora muito. Em recomendações (grafo seed pequeno) o ganho é moderado; o ganho grande aparece no fan-out denso / hidratação SQL.

Arquivos: `GetOrderUseCase` (batch `findByOrderIdIn`), `GetPurchaseRecommendationsUseCase`, `RecommendationRouterConfig` / `RecommendationReadHandler`, `ProductViewGraphRedisStore.recordView` (`Mono.zip`).

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
| GET recommendations (DevSeed + views, pós-batch A/B) | 50 | **523 ms** | **145** | ~40 / 41 |
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
| **Not a magic CPU discount** | bcrypt / SQL pesado / fan-out Redis ainda custam CPU/rede — login foi **removido da suite** para tratar por último. |

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
**Notes:** R2DBC pool `max-size=50`. Products / orders / recommendations re-medidos após otimizações de 2026-07-21. Demais linhas = suite `20260720`.

p95 = 95th percentile of `http_req_duration`.

| Module | Endpoint | Reqs | RPS | p95 | Failures | Checks |
|--------|----------|------|-----|-----|----------|--------|
| Auth | GET /api/auth/users | 2,559 | 83.86 | 1.02 s | 0.00% | 100.00% |
| Product | GET /api/products | 13,722 | **456** | **22.96 ms** | 0.00% | 100.00% |
| Product | GET /api/products/{id} | 12,663 | **420** | **48.07 ms** | 0.00% | 100.00% |
| Customer | GET /api/customers | 12,914 | 429.07 | 56.09 ms | 0.00% | 100.00% |
| Customer | GET /api/customers/{id} | 12,524 | 412.47 | 68.37 ms | 0.00% | 100.00% |
| Order | GET /api/orders/customer/{id} | — | **109** | **933 ms** | 0.00% | 100.00% |
| Order | POST /api/orders | 8,394 | 273.93 | 195.91 ms | 0.00% | 100.00% |
| Order | POST /api/orders/{id}/items | 8,980 | 296.04 | 276.74 ms | 0.00% | 100.00% |
| Order | POST /api/orders/{id}/pay | 10,229 | 334.55 | 241.67 ms | 0.00% | 100.00% |
| Payment | POST /api/payments | 11,684 | 383.84 | 211.55 ms | 0.00% | 100.00% |
| Redis | GET /api/recommendations/customers/{id} | — | **145** | **523 ms** | 0.00% | 100.00% |
| Redis | POST /api/recommendations/.../views | 11,342 | 374.44 | 107.17 ms | 0.00% | 100.00% |
| Checkout | order + item + payment | 10,043 | 325.90 | 254.99 ms | 0.00% | 100.00% |

Orders / recommendations: A/B limpo com **30 pedidos×2 itens** — orders **3.77 s → 933 ms**; recommendations **614 → 523 ms** (`ab-batch-20260721-165016.txt`).  
`POST /api/auth/login` removido da API e da suite (bcrypt; por último).

---

## Comparison: Sync → CompletableFuture → WebFlux

Baselines for **Sync** and **CF** come from [java-ecommerce-completable-future](https://github.com/noellepaes/java-ecommerce-completable-future) (same k6 shape: 50 VUs · 30s).  
**WebFlux** column = this run (`suite-20260720-095710`).

| Endpoint | Sync p95 | CF p95 | WebFlux p95 | Notes |
|----------|----------|--------|-------------|--------|
| POST /api/recommendations/.../views | 4.85 ms | **3.56 ms (−27%)** | 107.17 ms | CF overlaps Redis writes; WebFlux: `Mono.zip` nos SADDs |
| GET /api/recommendations/customers/{id} | 4.52 ms | 8.58 ms (+90%) | **523 ms** | A/B limpo: antes N+1/concatMap **614 ms** (−15%) |
| GET /api/auth/users | 5.01 ms | 5.87 ms | 1.02 s* | *Antes: N+1. Agora: query com JOIN (login adiado). |
| GET /api/products | 3.41 ms | — | **22.96 ms** | k6 **50 VUs · 30s · pause 0.1** · 13 722 reqs · **456 RPS** · fail 0% (`products-warm-20260722-102827`) |
| GET /api/products/{id} | 3.38 ms | — | **48.07 ms** | mesma carga · 12 663 reqs · **420 RPS** · fail 0% |
| GET /api/orders/customer/{id} | — | EntityGraph (estudo JPA) | **933 ms** | A/B limpo 30 pedidos: antes N+1 **3.77 s** (∼4×) |
| GET /api/customers/{id} | 3.23 ms | — | 68.37 ms | Best WebFlux CRUD p95 in this suite |
| Threads under ~50 VU load | Tomcat pool (often ≫ 100) | Tomcat + executor | **peak ≈ 89** | Clearest WebFlux advantage |
| Threads @ 500 VU (`GET /products`) | (Tomcat would grow with load) | — | **~40 live / 41 peak** | Stress run 2026-07-21; density win |

### How to read the comparison

1. **Threads ↓** — WebFlux is the winner: ~**89 peak** on the mixed suite, and **~40 threads at 500 VUs** on `GET /api/products` stress.
2. **Latency** — On this machine (Docker Desktop / Windows), absolute WebFlux p95 for simple CRUD did **not** beat the published Sync/CF numbers from the sibling study. Reactive stacks shine when you need **many concurrent connections with little memory/threads**, not when each request is a single tiny SQL round-trip already < 5 ms on JPA.
3. **CF vs WebFlux** — CF improves *selected* parallel I/O paths inside MVC. WebFlux changes the **server concurrency model** for every endpoint.
4. **Cost** — Fewer threads → less RAM per concurrent client → often **cheaper instances / higher density**, not cheaper CPU per request.

<img width="519" height="310" alt="Image" src="https://github.com/user-attachments/assets/39ec39a9-614f-4dfe-8dc2-34107f9fe223" />
---

## What changed vs MVC/JPA

| Layer | Before (MVC study) | This repo |
|-------|--------------------|-----------|
| HTTP | Spring MVC / Tomcat | **WebFlux / Netty** |
| Persistence | Spring Data JPA | **Spring Data R2DBC** + adapters |
| Redis | `StringRedisTemplate` | **`ReactiveStringRedisTemplate`** |
| API returns | `T` / `List` / `ResponseEntity` | **`Mono` / `Flux`** |
| Flyway | JDBC | JDBC (unchanged; required) |

Order aggregate: `OrderRepository` + `OrderItemRepository` (R2DBC); montagem dos itens no UseCase (sem cascade JPA).

---

## Extra docs

- `docs/ARQUITETURA.md`
- `docs/DECISOES_ARQUITETURAIS.md`
- `docs/GUIA_RAPIDO.md`
