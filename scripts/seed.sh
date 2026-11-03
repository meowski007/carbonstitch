#!/usr/bin/env bash
# ==============================================================================
# Carbonstitch Seed Script
# Seeds sample garments, triggers footprint calculation, submits batch jobs,
# and prints top emission hotspots in < 5 minutes.
# ==============================================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
echo "==> Seeding Carbonstitch at ${BASE_URL}..."

# Wait for service health
echo "--> Checking footprint-service health..."
for i in {1..30}; do
  if curl -sf "${BASE_URL}/actuator/health" > /dev/null; then
    echo "Service is healthy!"
    break
  fi
  echo "Waiting for service... ($i/30)"
  sleep 2
done

# Seed Garment 1: Organic Cotton T-Shirt
echo "--> Creating Garment 1: T-Shirt..."
G1_RES=$(curl -sf -X POST "${BASE_URL}/api/v1/garments" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SEED-TSHIRT-001",
    "name": "Classic Crewneck T-Shirt",
    "category": "T_SHIRT",
    "weightKg": 0.22,
    "materials": [
      {"material": "COTTON", "percentage": 100.0}
    ],
    "production": {
      "country": "BD",
      "transportMode": "SEA",
      "distanceKm": 8200.0
    }
  }')
G1_ID=$(echo "$G1_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "    Created Garment ID: $G1_ID"

# Seed Garment 2: Denim Jeans
echo "--> Creating Garment 2: Heavy Denim Jeans..."
G2_RES=$(curl -sf -X POST "${BASE_URL}/api/v1/garments" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SEED-JEANS-002",
    "name": "Raw Indigo Denim Jeans",
    "category": "JEANS",
    "weightKg": 0.85,
    "materials": [
      {"material": "COTTON", "percentage": 98.0},
      {"material": "ELASTANE", "percentage": 2.0}
    ],
    "production": {
      "country": "TR",
      "transportMode": "ROAD",
      "distanceKm": 2800.0
    }
  }')
G2_ID=$(echo "$G2_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "    Created Garment ID: $G2_ID"

# Seed Garment 3: Synthetic Winter Parka (High Emission)
echo "--> Creating Garment 3: Winter Puffer Jacket..."
G3_RES=$(curl -sf -X POST "${BASE_URL}/api/v1/garments" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SEED-PARKA-003",
    "name": "Waterproof Down Puffer Parka",
    "category": "JACKET",
    "weightKg": 1.45,
    "materials": [
      {"material": "POLYESTER", "percentage": 85.0},
      {"material": "NYLON", "percentage": 15.0}
    ],
    "production": {
      "country": "CN",
      "transportMode": "AIR",
      "distanceKm": 8900.0
    }
  }')
G3_ID=$(echo "$G3_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "    Created Garment ID: $G3_ID"

# Seed Garment 4: Merino Wool Knit Sweater
echo "--> Creating Garment 4: Merino Wool Sweater..."
G4_RES=$(curl -sf -X POST "${BASE_URL}/api/v1/garments" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "SEED-KNIT-004",
    "name": "Merino Wool Crew Sweater",
    "category": "SWEATER",
    "weightKg": 0.55,
    "materials": [
      {"material": "WOOL", "percentage": 100.0}
    ],
    "production": {
      "country": "IT",
      "transportMode": "ROAD",
      "distanceKm": 1200.0
    }
  }')
G4_ID=$(echo "$G4_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "    Created Garment ID: $G4_ID"

# Calculate footprints
echo "--> Calculating footprint for Garment 1..."
curl -sf -X POST "${BASE_URL}/api/v1/garments/${G1_ID}/footprint" | json_pp 2>/dev/null || true

echo "--> Submitting asynchronous batch calculation for garments [${G2_ID}, ${G3_ID}, ${G4_ID}]..."
BATCH_RES=$(curl -sf -X POST "${BASE_URL}/api/v1/footprints/batch" \
  -H "Content-Type: application/json" \
  -d "{\"garmentIds\": [${G2_ID}, ${G3_ID}, ${G4_ID}]}")
echo "$BATCH_RES"
JOB_ID=$(echo "$BATCH_RES" | grep -o '"jobId":"[^"]*' | cut -d'"' -f4)

echo "--> Polling job status for Job ID: ${JOB_ID}..."
for i in {1..10}; do
  STATUS_RES=$(curl -sf "${BASE_URL}/api/v1/jobs/${JOB_ID}")
  echo "    Status: $STATUS_RES"
  if echo "$STATUS_RES" | grep -q '"COMPLETED"'; then
    break
  fi
  sleep 1
done

# Top hotspots report
echo "--> Querying Top Emission Hotspots..."
curl -sf "${BASE_URL}/api/v1/reports/hotspots?limit=5"
echo ""
echo "==> Seeding completed successfully!"
