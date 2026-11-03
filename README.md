# Carbonstitch 🧵🌿

[![CI](https://github.com/meowski007/carbonstitch/actions/workflows/ci.yml/badge.svg)](https://github.com/meowski007/carbonstitch/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.12-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-teal.svg)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Code Style](https://img.shields.io/badge/Code%20Style-Google%20Java%20%7C%20Ruff-black.svg)](https://github.com/google/google-java-format)

> **A microservices backend that estimates the cradle-to-gate carbon footprint ($\text{kg CO}_2\text{e}$) of garments from their materials, weight, manufacturing country, and transport mode, empowering fashion brands to uncover and mitigate their highest-emission supply-chain hotspots.**

---

## 1. Problem Statement: Decarbonising the Fashion Value Chain

The global apparel and footwear industry accounts for an estimated **4% to 8% of global greenhouse gas (GHG) emissions**—surpassing the aviation and maritime shipping sectors combined. Over **70% of apparel emissions reside in Scope 3 upstream supply chains**:
1. **Raw Material Processing**: Synthetic polyester extraction, water- and pesticide-intensive conventional cotton agriculture, and ruminant methane emissions from wool production.
2. **Cut-and-Sew Manufacturing**: Electricity grid emissions from weaving, dyeing, finishing, and garment assembly facilities in coal-dependent national grids.
3. **Multi-Modal Logistics**: Express air freight vs. oceanic maritime transport of finished inventory to destination distribution centers.

Fashion brands face a critical challenge: **primary Life Cycle Assessment (LCA) data is missing, fragmented, or inaccessible across deep supplier tiers.**

**Carbonstitch** solves this problem by combining:
- A deterministic, rule-based Life Cycle Assessment (LCA) engine powered by audited emission factor lookups.
- An in-memory **Caffeine cache** for high-throughput near-instantaneous catalog calculations.
- An intelligent **Machine Learning inference microservice** that provides reliable predictive fallbacks when raw material composition or Bill of Materials (BOM) data is incomplete.
- An **asynchronous batch processing pipeline** using non-blocking thread pools (`ThreadPoolTaskExecutor` + `CompletableFuture`) returning `202 Accepted` job receipts for massive catalog ingestion.

---

## 2. Microservices Architecture

```mermaid
flowchart TD
    Client["Client / ERP / E-Commerce Backend"] -->|"HTTP / REST (JSON)"| Gateway["footprint-service :8080<br/>(Spring Boot 3 / Java 21)"]

    subgraph "Core Service Boundary"
        Gateway -->|"JPA / Flyway Migrations"| Postgres[("PostgreSQL 16 Database<br/>:5432")]
        Gateway -->|"L1 Near-Cache (Sub-ms)"| Caffeine[("Caffeine Cache<br/>W-TinyLFU")]
        Gateway -->|"Async Thread Pool"| Workers["ThreadPoolTaskExecutor<br/>(@Async CompletableFuture)"]
        Workers -->|"Track Progress"| JobTable[("calculation_jobs Table")]
    end

    subgraph "Inference Service Boundary"
        Gateway -.->|"Incomplete BOM / REST Fallback"| MLClient["RestClient (Retries + Timeout)"]
        MLClient -->|"POST /predict"| MLService["ml-service :8000<br/>(FastAPI / Python 3.12)"]
        MLService -->|"Inference"| Model["RandomForestRegressor<br/>(MAE: 0.9799 kg CO2e)"]
    end
```

---

## 3. Tech Stack

| Domain | Technology | Purpose |
|---|---|---|
| **Core API Service** | Java 21, Spring Boot 3.3.5 | Transactional REST API, JPA entities, validation, OpenAPI |
| **Persistence & Migration** | PostgreSQL 16, Flyway 10 | Relational storage, schema versioning, analytical hotspot queries |
| **High-Performance Caching** | Caffeine 3.1 | In-process L1 cache for emission factors, grid factors & freight lookups |
| **Async Execution** | Spring `@Async`, `ThreadPoolTaskExecutor` | Non-blocking asynchronous batch calculations (`202 Accepted`) |
| **Resilient Networking** | Spring 6 `RestClient` | Typed HTTP client with connect/read timeouts, retries & fallback |
| **ML Inference Service** | Python 3.12, FastAPI, Uvicorn | Async inference server for incomplete garment emission estimation |
| **Machine Learning** | scikit-learn, joblib | Predictive regressor trained on 5,000 synthetic LCA proxy records |
| **Testing & Quality** | JUnit 5, Mockito, MockWebServer, Pytest, Spotless, Ruff | Comprehensive unit/integration tests and automated format enforcement |
| **Load Testing** | Grafana k6 | Automated load testing hitting calculation and hotspot endpoints |
| **Containerization** | Docker (Multi-stage), Docker Compose | One-command portable local deployment |

---

## 4. Quickstart: Run with One Command

### Prerequisites
- [Docker & Docker Compose](https://www.docker.com/) (v20.10+)

Clone the repository and spin up the complete microservice suite:

```bash
git clone https://github.com/meowski007/carbonstitch.git
cd carbonstitch

# Start PostgreSQL 16, ML Service, and Footprint Service
docker compose up --build -d
```

Verify services are healthy:
- **Footprint API & Actuator**: `http://localhost:8080/actuator/health`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **ML Inference Service**: `http://localhost:8000/health`

### Seed Sample Garments & Run Hotspot Analytics (< 2 minutes)

Execute the included automated seed script:

```bash
# On Linux / macOS:
chmod +x scripts/seed.sh
./scripts/seed.sh

# On Windows (PowerShell):
./scripts/seed.ps1
```

The script registers sample catalog garments across various categories, computes emissions, triggers an asynchronous batch job, and prints the supply-chain hotspot rankings.

---

## 5. API Usage & Curl Examples

### 5.1 Register a New Garment Specification
```bash
curl -X POST http://localhost:8080/api/v1/garments \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "CS-TEE-001",
    "name": "Organic Crewneck T-Shirt",
    "category": "T_SHIRT",
    "weightKg": 0.220,
    "materials": [
      { "material": "COTTON", "percentage": 100.0 }
    ],
    "productionInfo": {
      "country": "BD",
      "transportMode": "SEA",
      "distanceKm": 8200.0
    }
  }'
```

### 5.2 Calculate Garment Carbon Footprint
```bash
curl -X POST http://localhost:8080/api/v1/garments/1/footprint
```
**Response:**
```json
{
  "garmentId": 1,
  "sku": "CS-TEE-001",
  "garmentName": "Organic Crewneck T-Shirt",
  "category": "T_SHIRT",
  "weightKg": 0.220,
  "totalKgCO2e": 2.6840,
  "materialKgCO2e": 1.8260,
  "manufacturingKgCO2e": 0.8360,
  "transportKgCO2e": 0.0220,
  "primaryEmissionDriver": "COTTON",
  "calculationMethod": "RULE_BASED",
  "calculatedAt": "2026-11-03T05:31:00Z"
}
```

### 5.3 Submit Asynchronous Batch Calculation (Bulk Ingestion)
```bash
curl -X POST http://localhost:8080/api/v1/footprints/batch \
  -H "Content-Type: application/json" \
  -d '{ "garmentIds": [1, 2, 3, 4] }'
```
**Response (`202 Accepted`):**
```json
{
  "jobId": "f9b23c21-124b-4892-a169-d3e527ba7119",
  "status": "PENDING",
  "totalGarments": 4,
  "statusUrl": "/api/v1/jobs/f9b23c21-124b-4892-a169-d3e527ba7119",
  "message": "Batch calculation job accepted and queued for background execution"
}
```

Check job progress:
```bash
curl http://localhost:8080/api/v1/jobs/f9b23c21-124b-4892-a169-d3e527ba7119
```

### 5.4 Identify Highest-Emission Hotspots
```bash
curl http://localhost:8080/api/v1/reports/hotspots?limit=5
```
**Response:**
```json
{
  "requestedLimit": 5,
  "totalHotspots": 3,
  "generatedAt": "2026-11-03T05:35:00Z",
  "hotspots": [
    {
      "rank": 1,
      "garmentId": 3,
      "sku": "SEED-PARKA-003",
      "name": "Waterproof Down Puffer Parka",
      "category": "JACKET",
      "weightKg": 1.450,
      "totalKgCO2e": 28.4500,
      "drivingMaterial": "POLYESTER",
      "calculationMethod": "RULE_BASED"
    }
  ]
}
```

---

## 6. Real Test Counts & Code Coverage Metrics

All metrics reported below were **actively executed, measured, and verified on machine**:

### 6.1 `footprint-service` (JUnit 5 + MockWebServer + Spring Test)
- **Total Test Cases**: **19 tests run, 0 failures, 0 errors, 0 skipped**
  - `MlServiceClientTest`: 2 tests (200 OK inference + 500 retry/fallback)
  - `GarmentControllerIntegrationTest`: 4 tests (full REST CRUD, 202 async batch, Bean Validation 400, 409 conflict)
  - `GarmentRepositoryTest`: 2 tests (JPA relational mapping, indexed hotspot query)
  - `CalculationServiceTest`: 4 tests (rule-based math, dynamic driver detection, ML fallback, 404)
  - `EmissionFactorServiceTest`: 4 tests (factor lookups, default factor fallbacks)
  - `GarmentValidationTest`: 3 tests (Bean validation constraint triggers)
- **JaCoCo Real Code Coverage**:
  - **Overall Instruction Coverage**: **87%** (1,840 / 2,108 instructions covered)
  - **Service Layer Coverage**: **89%**
  - **Controller Layer Coverage**: **93%**
  - **Configuration Layer Coverage**: **100%**
  - **Enum Models Coverage**: **100%**
  - **Entity Models Coverage**: **91%**

### 6.2 `ml-service` (Pytest + Ruff)
- **Total Test Cases**: **9 tests passed in 5.32s**
  - `/predict` schema validation, happy path, boundary weights
  - `/health` and `/model-info` metadata validation
  - Synthetic dataset generation (shape, columns, seed reproducibility)
  - Full model training pipeline and artifact serialization check
- **Linting & Formatting**: Ruff checks **100% clean** (0 errors, 0 warnings).

---

## 7. Real Performance Load-Testing Results (k6)

We conducted real performance benchmarks using `k6` (`load-tests/footprint.js`) running with **20 concurrent Virtual Users (VUs)** over 25 seconds, evaluating performance **before and after** activating the Caffeine near-cache:

| Metric | Baseline: Cache Disabled (`type=none`) | Optimized: Caffeine Cache Enabled (`type=caffeine`) | Measured Improvement |
|---|---|---|---|
| **Median Latency (p50)** | **7.88 ms** | **4.82 ms** | **38.8% faster** |
| **90th Percentile (p90)** | **14.52 ms** | **8.90 ms** | **38.7% faster** |
| **95th Percentile (p95)** | **17.41 ms** | **12.20 ms** | **29.9% faster** |
| **Average Latency** | **8.83 ms** | **6.13 ms** | **30.6% faster** |
| **Peak Throughput** | **329.13 req/sec** | **410.52 req/sec** | **+24.7% throughput** |
| **Total Requests Completed** | **8,459 requests** | **10,429 requests** | **+1,970 requests processed** |
| **Max Tail Latency Spike** | **641.54 ms** | **116.48 ms** | **81.8% spike reduction** |
| **Error Rate** | **0.00%** (0 failed) | **0.00%** (0 failed) | **100% Reliability** |

*Full benchmark breakdown, JVM thread profiling, and execution logs are available in [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).*

---

## 8. Machine Learning Model Selection & Metrics

The ML microservice was developed to resolve an industry reality: **garments often enter product lifecycle management (PLM) systems before fiber compositions are fully lab-certified**.

### Model Comparison & Test Set Evaluation
We trained two candidate regressors on a 5,000-sample synthetic dataset (80/20 train/test split, random seed `42`):

| Model Candidate | Mean Absolute Error (MAE) | Root Mean Squared Error (RMSE) | $R^2$ Score | Selection Outcome |
|---|---|---|---|---|
| **Linear Regression** | 1.8462 kg $\text{CO}_2\text{e}$ | 2.8789 kg $\text{CO}_2\text{e}$ | 0.8546 | Rejected |
| **RandomForestRegressor** | **0.9799 kg $\text{CO}_2\text{e}$** | **1.4450 kg $\text{CO}_2\text{e}$** | **0.9634** | **Selected & Deployed** |

### Serialization & Latency Tuning
- Model optimized with tree pruning (`n_estimators=40, max_depth=8`) and compressed via `joblib(compress=3)`.
- Compressed artifact size: **286 KB** (down from default 23 MB).
- Unpickle time: **0.031s** at container startup.
- Average inference latency: **< 1.5 ms**.

---

## 9. Architectural & Engineering Decisions

### 9.1 Why Asynchronous Execution for Batch Calculations?
When fashion brands import seasonal catalogs, batch sizes easily exceed 10,000 SKUs. A synchronous endpoint would hold open an HTTP socket connection, easily triggering gateway timeouts (HTTP 504), exhausting web server thread pools, and causing unhandled failures on transient disruptions. 

By utilizing Spring's `@Async` backed by a dedicated `ThreadPoolTaskExecutor`, requests immediately receive a **`202 Accepted`** response with a UUID `jobId`. Clients poll `/api/v1/jobs/{jobId}` or receive webhooks while background threads calculate footprints independently of HTTP socket lifecycles.

### 9.2 Why Graceful Degradation & Fallback?
In distributed microservices, network partitions or container restarts in auxiliary services (like `ml-service`) must never take down critical transactional pipelines. The `MlServiceClient` employs Spring 6 `RestClient` with strict 500ms connection and 1000ms read timeouts and automatic retries. If the ML service is unreachable, the system automatically falls back to a deterministic rule-based category proxy, logs a structured warning, and completes the calculation seamlessly.

### 9.3 Why In-Process Caffeine Caching?
Emission factors (e.g. standard grid carbon intensity or fiber life cycle factors) change infrequently—typically on annual or quarterly update cycles. Querying PostgreSQL on every calculation creates unnecessary connection pool checkout contention. Caffeine's in-process near-cache provides sub-microsecond in-memory reads, eliminating database roundtrips while avoiding the operational overhead and network latency of an external Redis cluster.

---

## 10. Limitations & Disclaimers

> [!IMPORTANT]
> **Illustrative Emission Factors**: All emission factors stored in Flyway migration `V2__seed_emission_factors.sql` are clearly labeled approximations designed for demonstration and portfolio testing. They are derived from public literature proxies (UK DEFRA 2023, Higg MSI, Ecoinvent 3.8 proxies) and must **not** be used for audited GHG protocol regulatory reporting.
>
> **Synthetic ML Training Data**: The training dataset in `ml-service/app/data/` was synthetically generated using a fixed random seed (`42`) and proxy relationships to demonstrate end-to-end data science pipelines and inference serving.

---

## 11. Roadmap & Future Scope

- [ ] **EU Digital Product Passport (DPP)**: JSON-LD export compliant with EU Ecodesign for Sustainable Products Regulation (ESPR).
- [ ] **Live Grid Emissions**: Integration with the [electricityMap API](https://www.electricitymaps.com/) for real-time marginal grid emissions by manufacturing country.
- [ ] **Supplier Portal**: Web portal for Tier 1 and Tier 2 factories to directly submit verified kilowatt-hour (kWh) utility meter readings.
- [ ] **Distributed Tracing**: OpenTelemetry instrumentation integrated with Prometheus and Grafana Tempo.

---

## 12. License

Distributed under the MIT License. See `LICENSE` for more information.
