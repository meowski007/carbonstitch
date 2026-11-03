import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 10 },   // Ramp-up to 10 VUs
    { duration: '15s', target: 20 },  // Sustained load with 20 VUs
    { duration: '5s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should complete within 500ms
    http_req_failed: ['rate<0.01'],   // Error rate below 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Setup phase: seed sample garment for load testing
export function setup() {
  const payload = JSON.stringify({
    sku: `PERF-TEE-${Date.now()}`,
    name: 'Performance Benchmark Tee',
    category: 'T_SHIRT',
    weightKg: 0.250,
    materials: [
      { material: 'COTTON', percentage: 70.0 },
      { material: 'POLYESTER', percentage: 30.0 },
    ],
    productionInfo: {
      country: 'CN',
      transportMode: 'SEA',
      distanceKm: 12000.0,
    },
  });

  const headers = { 'Content-Type': 'application/json' };
  const res = http.post(`${BASE_URL}/api/v1/garments`, payload, { headers });
  
  if (res.status === 201) {
    const data = JSON.parse(res.body);
    return { garmentId: data.id };
  }

  // Fallback to ID 1 if already seeded
  return { garmentId: 1 };
}

export default function (data) {
  const headers = { 'Content-Type': 'application/json' };

  // 1. Calculate / recalculate footprint (evaluates emission factor lookups)
  const calcRes = http.post(`${BASE_URL}/api/v1/garments/${data.garmentId}/footprint`, null, {
    headers,
    tags: { name: 'CalculateFootprint' },
  });

  check(calcRes, {
    'calc status is 200': (r) => r.status === 200,
    'calc has totalKgCO2e': (r) => JSON.parse(r.body).totalKgCO2e !== undefined,
  });

  // 2. Query hotspots report
  const reportRes = http.get(`${BASE_URL}/api/v1/reports/hotspots?limit=10`, {
    tags: { name: 'GetHotspots' },
  });

  check(reportRes, {
    'hotspots status is 200': (r) => r.status === 200,
    'hotspots has items': (r) => JSON.parse(r.body).hotspots !== undefined,
  });

  sleep(0.05); // 50ms pause between VU iterations
}

export function teardown(data) {
  // Optional cleanup
}
