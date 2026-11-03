# Performance Benchmark & Caching Analysis

This report documents real load-testing measurements executed on the `carbonstitch` microservice using [k6](https://k6.io/). We compare the system performance **BEFORE** and **AFTER** enabling an in-process [Caffeine](https://github.com/ben-manes/caffeine) near-cache for emission factor lookups.

---

## 1. Benchmark Setup & Configuration

- **Workload**: `load-tests/footprint.js`
  - Endpoints targeted per iteration:
    1. `POST /api/v1/garments/{id}/footprint` (triggers fiber emission lookups, grid factors, and logistics calculations)
    2. `GET /api/v1/reports/hotspots?limit=10` (executes analytical ranking query across garment portfolio)
- **Concurrency & Load Profile**:
  - Stage 1: 5s ramp-up to 10 Virtual Users (VUs)
  - Stage 2: 15s sustained peak load with 20 VUs
  - Stage 3: 5s ramp-down to 0 VUs
  - Total test duration: 25.4 seconds
- **Hardware & Host Environment**:
  - OS: Windows x86_64
  - JVM: OpenJDK 21.0.6 (Temurin)
  - Database: PostgreSQL 16.6 on local NVMe storage

---

## 2. Real Measured Benchmark Results

All figures below reflect exact metrics measured directly by the `k6` load generator.

| Metric | Baseline: Cache Disabled (`type=none`) | Optimized: Caffeine Cache Enabled (`type=caffeine`) | Improvement |
|---|---|---|---|
| **Median Latency (p50)** | **7.88 ms** | **4.82 ms** | **38.8% faster** |
| **90th Percentile (p90)** | **14.52 ms** | **8.90 ms** | **38.7% faster** |
| **95th Percentile (p95)** | **17.41 ms** | **12.20 ms** | **29.9% faster** |
| **Average Latency** | **8.83 ms** | **6.13 ms** | **30.6% faster** |
| **Minimum Latency** | **1.62 ms** | **1.49 ms** | **8.0% faster** |
| **Peak Throughput** | **329.13 req/sec** | **410.52 req/sec** | **+24.7% throughput** |
| **Total Requests Completed** | **8,459 requests** | **10,429 requests** | **+1,970 requests processed** |
| **Request Error Rate** | **0.00%** (0 / 8,459 failed) | **0.00%** (0 / 10,429 failed) | 100% Reliability |

---

## 3. Detailed k6 Execution Summaries

### 3.1 Baseline Run: Without Cache (`spring.cache.type=none`)
```text
     execution: local
        script: load-tests/footprint.js
     scenarios: 1 scenario, 20 max VUs, 25s duration

     ✓ calc status is 200
     ✓ calc has totalKgCO2e
     ✓ hotspots status is 200
     ✓ hotspots has items

     checks.........................: 100.00% 16916 out of 16916
     http_req_duration..............: avg=8.83ms min=1.62ms med=7.88ms max=641.54ms p(90)=14.52ms p(95)=17.41ms
     http_req_failed................: 0.00%   0 out of 8459
     http_reqs......................: 8459    329.13/s
     iterations.....................: 4229    164.54/s
```

### 3.2 Optimized Run: With Caffeine Cache (`spring.cache.type=caffeine`)
```text
     execution: local
        script: load-tests/footprint.js
     scenarios: 1 scenario, 20 max VUs, 25s duration

     ✓ calc status is 200
     ✓ calc has totalKgCO2e
     ✓ hotspots status is 200
     ✓ hotspots has items

     checks.........................: 100.00% 20856 out of 20856
     http_req_duration..............: avg=6.13ms min=1.49ms med=4.82ms max=116.48ms p(90)=8.90ms p(95)=12.20ms
     http_req_failed................: 0.00%   0 out of 10429
     http_reqs......................: 10429   410.52/s
     iterations.....................: 5214    205.24/s
```

---

## 4. Architectural Analysis & Takeaways

1. **Elimination of Repetitive Database Roundtrips**:
   Every garment calculation looks up emission factors for its composite materials (e.g. `COTTON`, `POLYESTER`), the manufacturing country grid (`CN`, `BD`), and the freight mode (`SEA`, `ROAD`). Without caching, each calculation requires 3 to 5 `SELECT` queries across the JDBC connection pool. With Caffeine's W-TinyLFU eviction algorithm, factors reside in L1 JVM heap memory, transforming SQL index seeks into sub-microsecond hash table lookups.

2. **Tail Latency Compression**:
   The p95 latency decreased from **17.41 ms** to **12.20 ms** (a ~30% improvement), while peak latency spikes caused by database lock contention and connection pool checkout were cut from **641.54 ms** down to **116.48 ms** (an **81.8% reduction in maximum tail latency**).

3. **Throughput Scaling**:
   Under identical 20-VU concurrency and identical 25-second durations, the cached system served **10,429 requests** compared to **8,459 requests** without cache — a net gain of **+24.7% sustained throughput**.
