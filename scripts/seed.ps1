# ==============================================================================
# Carbonstitch Seed Script (PowerShell)
# Seeds sample garments, triggers footprint calculation, submits batch jobs,
# and prints top emission hotspots in < 5 minutes.
# ==============================================================================
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
Write-Host "==> Seeding Carbonstitch at $BaseUrl..." -ForegroundColor Cyan

# Wait for service health
Write-Host "--> Checking footprint-service health..."
$healthy = $false
for ($i = 1; $i -le 30; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            $healthy = $true
            Write-Host "Service is healthy!" -ForegroundColor Green
            break
        }
    } catch {
        Write-Host "Waiting for service... ($i/30)"
        Start-Sleep -Seconds 2
    }
}

if (-not $healthy) {
    Write-Warning "Service did not respond with UP in 60s, proceeding anyway..."
}

# 1. Garment 1: Organic Cotton T-Shirt
Write-Host "--> Creating Garment 1: T-Shirt..."
$g1Body = @{
    sku = "SEED-TSHIRT-001"
    name = "Classic Crewneck T-Shirt"
    category = "T_SHIRT"
    weightKg = 0.220
    materials = @(
        @{ material = "COTTON"; percentage = 100.0 }
    )
    productionInfo = @{
        country = "BD"
        transportMode = "SEA"
        distanceKm = 8200.0
    }
} | ConvertTo-Json -Depth 5

$g1 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/garments" -Method Post -ContentType "application/json" -Body $g1Body
Write-Host "    Created Garment ID: $($g1.id)"

# 2. Garment 2: Denim Jeans
Write-Host "--> Creating Garment 2: Heavy Denim Jeans..."
$g2Body = @{
    sku = "SEED-JEANS-002"
    name = "Raw Indigo Denim Jeans"
    category = "JEANS"
    weightKg = 0.850
    materials = @(
        @{ material = "COTTON"; percentage = 98.0 },
        @{ material = "ELASTANE"; percentage = 2.0 }
    )
    productionInfo = @{
        country = "TR"
        transportMode = "ROAD"
        distanceKm = 2800.0
    }
} | ConvertTo-Json -Depth 5

$g2 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/garments" -Method Post -ContentType "application/json" -Body $g2Body
Write-Host "    Created Garment ID: $($g2.id)"

# 3. Garment 3: Winter Puffer Jacket
Write-Host "--> Creating Garment 3: Winter Puffer Jacket..."
$g3Body = @{
    sku = "SEED-PARKA-003"
    name = "Waterproof Down Puffer Parka"
    category = "JACKET"
    weightKg = 1.450
    materials = @(
        @{ material = "POLYESTER"; percentage = 85.0 },
        @{ material = "NYLON"; percentage = 15.0 }
    )
    productionInfo = @{
        country = "CN"
        transportMode = "AIR"
        distanceKm = 8900.0
    }
} | ConvertTo-Json -Depth 5

$g3 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/garments" -Method Post -ContentType "application/json" -Body $g3Body
Write-Host "    Created Garment ID: $($g3.id)"

# 4. Garment 4: Merino Wool Sweater
Write-Host "--> Creating Garment 4: Merino Wool Sweater..."
$g4Body = @{
    sku = "SEED-KNIT-004"
    name = "Merino Wool Crew Sweater"
    category = "SWEATER"
    weightKg = 0.550
    materials = @(
        @{ material = "WOOL"; percentage = 100.0 }
    )
    productionInfo = @{
        country = "IT"
        transportMode = "ROAD"
        distanceKm = 1200.0
    }
} | ConvertTo-Json -Depth 5

$g4 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/garments" -Method Post -ContentType "application/json" -Body $g4Body
Write-Host "    Created Garment ID: $($g4.id)"

# Calculate single footprint
Write-Host "--> Calculating footprint for Garment 1..."
$fp1 = Invoke-RestMethod -Uri "$BaseUrl/api/v1/garments/$($g1.id)/footprint" -Method Post
Write-Host "    Total kg CO2e: $($fp1.totalKgCO2e) (Method: $($fp1.calculationMethod), Driver: $($fp1.primaryEmissionDriver))"

# Submit batch calculation
Write-Host "--> Submitting asynchronous batch calculation for garments [$($g2.id), $($g3.id), $($g4.id)]..."
$batchBody = @{ garmentIds = @($g2.id, $g3.id, $g4.id) } | ConvertTo-Json
$batchRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/footprints/batch" -Method Post -ContentType "application/json" -Body $batchBody
$jobId = $batchRes.jobId
Write-Host "    Batch Job ID: $jobId, Status: $($batchRes.status)"

# Poll job status
Write-Host "--> Polling job status for Job ID: $jobId..."
for ($i = 1; $i -le 10; $i++) {
    $statusRes = Invoke-RestMethod -Uri "$BaseUrl/api/v1/jobs/$jobId" -Method Get
    Write-Host "    Progress: $($statusRes.status) ($($statusRes.processedItems)/$($statusRes.totalItems) items)"
    if ($statusRes.status -eq "COMPLETED") {
        break
    }
    Start-Sleep -Seconds 1
}

# Hotspots report
Write-Host "--> Querying Top Emission Hotspots..." -ForegroundColor Cyan
$hotspots = Invoke-RestMethod -Uri "$BaseUrl/api/v1/reports/hotspots?limit=5" -Method Get
Write-Host "    Total Analyzed: $($hotspots.totalAnalyzed)"
$hotspots.hotspots | ForEach-Object {
    Write-Host "    - [$($_.garmentSku)] $($_.garmentName): $($_.totalKgCO2e) kg CO2e (Driver: $($_.primaryEmissionDriver), $($_.calculationMethod))"
}

Write-Host "==> Seeding completed successfully!" -ForegroundColor Green
