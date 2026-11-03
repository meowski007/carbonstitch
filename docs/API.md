# Carbonstitch API Specification

Base URLs:
- **Footprint API**: `http://localhost:8080` (Spring Boot 3 REST API)
- **ML Inference API**: `http://localhost:8000` (FastAPI Microservice)
- **Swagger / OpenAPI Documentation**: `http://localhost:8080/swagger-ui.html`

---

## 1. Footprint Service Endpoints (`footprint-service`)

### 1.1 Garment Catalog Management

#### `POST /api/v1/garments`
Register a new garment specification in the catalog.

**Request Body:**
```json
{
  "sku": "CS-TEE-001",
  "name": "Organic Crewneck T-Shirt",
  "category": "T_SHIRT",
  "weightKg": 0.220,
  "materials": [
    {
      "material": "COTTON",
      "percentage": 100.00
    }
  ],
  "productionInfo": {
    "country": "BD",
    "transportMode": "SEA",
    "distanceKm": 8200.00
  }
}
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "sku": "CS-TEE-001",
  "name": "Organic Crewneck T-Shirt",
  "category": "T_SHIRT",
  "weightKg": 0.220,
  "productionInfo": {
    "country": "BD",
    "transportMode": "SEA",
    "distanceKm": 8200.00
  },
  "materials": [
    {
      "material": "COTTON",
      "percentage": 100.00
    }
  ],
  "createdAt": "2026-11-03T05:30:00Z",
  "updatedAt": "2026-11-03T05:30:00Z"
}
```

#### `GET /api/v1/garments`
Retrieve all registered garments in the catalog.

#### `GET /api/v1/garments/{id}`
Retrieve details of a specific garment by ID.

#### `PUT /api/v1/garments/{id}`
Update name, weight, materials, or logistics for an existing garment.

#### `DELETE /api/v1/garments/{id}`
Remove a garment and cascade-delete its calculated footprint results (`204 No Content`).

---

### 1.2 Footprint Calculation & Reporting

#### `POST /api/v1/garments/{id}/footprint`
Calculate or recalculate the cradle-to-gate LCA carbon footprint of a garment.

**Response (`200 OK`):**
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

#### `GET /api/v1/garments/{id}/footprint`
Retrieve existing footprint results for a garment without recalculating.

#### `POST /api/v1/footprints/batch`
Asynchronously enqueue bulk calculation for multiple garments off the main request thread.

**Request Body:**
```json
{
  "garmentIds": [1, 2, 3, 4, 5]
}
```

**Response (`202 Accepted`):**
```json
{
  "jobId": "f9b23c21-124b-4892-a169-d3e527ba7119",
  "status": "PENDING",
  "totalGarments": 5,
  "statusUrl": "/api/v1/jobs/f9b23c21-124b-4892-a169-d3e527ba7119",
  "message": "Batch calculation job accepted and queued for background execution"
}
```

#### `GET /api/v1/jobs/{jobId}`
Poll the execution status of an asynchronous bulk calculation job.

**Response (`200 OK`):**
```json
{
  "jobId": "f9b23c21-124b-4892-a169-d3e527ba7119",
  "status": "COMPLETED",
  "totalItems": 5,
  "processedItems": 5,
  "progressPercentage": 100.0,
  "errorMessage": null,
  "createdAt": "2026-11-03T05:32:00Z",
  "completedAt": "2026-11-03T05:32:02Z"
}
```

#### `GET /api/v1/reports/hotspots?limit=10`
Query the highest-emitting garments across the entire catalog and identify the driving material for each.

**Response (`200 OK`):**
```json
{
  "requestedLimit": 10,
  "totalHotspots": 3,
  "generatedAt": "2026-11-03T05:33:00Z",
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

## 2. ML Service Endpoints (`ml-service`)

#### `POST /predict`
Infer garment carbon footprint using the trained `RandomForestRegressor`.

**Request Body:**
```json
{
  "category": "JEANS",
  "weight_kg": 0.850,
  "country": "TR",
  "transport_mode": "ROAD",
  "distance_km": 2800.0
}
```

**Response (`200 OK`):**
```json
{
  "estimated_kg_co2e": 8.7421,
  "model_version": "1.0.0",
  "model_name": "RandomForestRegressor"
}
```

#### `GET /model-info`
Retrieve evaluation metrics on held-out test split.

**Response (`200 OK`):**
```json
{
  "model_name": "RandomForestRegressor",
  "version": "1.0.0",
  "train_samples": 4000,
  "test_samples": 1000,
  "metrics": {
    "mae": 0.9799,
    "rmse": 1.4450,
    "r2": 0.9634
  }
}
```

#### `GET /health`
Liveness and readiness probe for container health checks.
```json
{
  "status": "UP",
  "service": "carbonstitch-ml-service"
}
```
