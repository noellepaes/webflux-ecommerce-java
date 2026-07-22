# Clean A/B: before (N+1) vs after (batch IN)
$ErrorActionPreference = "Continue"
$ComposeFile = Join-Path $PSScriptRoot "..\docker-compose.yml"
$ProjectRoot = Split-Path $ComposeFile -Parent
$ResultsDir = Join-Path $PSScriptRoot "results"
New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null
$report = Join-Path $ResultsDir "ab-batch-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$tmpDir = Join-Path $env:TEMP "webflux-ab"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function Wait-App {
    $deadline = (Get-Date).AddMinutes(4)
    do {
        $h = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/actuator/health
        if ($h -eq "200") {
            $p = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/api/products
            if ($p -eq "200") { return }
        }
        Start-Sleep 3
    } while ((Get-Date) -lt $deadline)
    throw "App/products health timeout"
}

function Reset-State {
    docker exec ecommerce-postgres psql -U ecommerce -d ecommerce -c "TRUNCATE order_schema.order_items, order_schema.orders, payment_schema.payments RESTART IDENTITY CASCADE;" | Out-Null
    docker exec ecommerce-redis redis-cli FLUSHDB | Out-Null
    docker restart ecommerce-app | Out-Null
    Wait-App
    Start-Sleep 10
}

function Post-Json([string]$Url, [string]$JsonPath) {
    return curl.exe -s -X POST $Url -H "Content-Type: application/json" --data-binary "@$JsonPath"
}

function Seed-Workload {
    $customers = curl.exe -s http://localhost:8080/api/customers | ConvertFrom-Json
    $cust = @($customers) | Where-Object { $_.email -eq "noelle.seed@dev.local" } | Select-Object -First 1
    if (-not $cust) { throw "Noelle customer missing" }
    $cid = [string]$cust.id

    $products = @((curl.exe -s http://localhost:8080/api/products | ConvertFrom-Json))
    if ($products.Count -lt 2) { throw "Need products count=$($products.Count)" }
    $p0 = $products[0]
    $p1 = $products[1]

    foreach ($p in $products | Select-Object -First 3) {
        $viewFile = Join-Path $tmpDir "view.json"
        Set-Content -Path $viewFile -Value ("{`"productId`":`"$($p.id)`"}") -NoNewline -Encoding ascii
        Post-Json "http://localhost:8080/api/recommendations/customers/$cid/views" $viewFile | Out-Null
    }

    $orderFile = Join-Path $tmpDir "order.json"
    Set-Content -Path $orderFile -Value ("{`"customerId`":`"$cid`"}") -NoNewline -Encoding ascii

    for ($i = 0; $i -lt 30; $i++) {
        $raw = Post-Json "http://localhost:8080/api/orders" $orderFile
        $o = $raw | ConvertFrom-Json
        if (-not $o.id) { throw "Create order failed: $raw" }
        foreach ($prod in @($p0, $p1)) {
            $itemFile = Join-Path $tmpDir "item.json"
            Set-Content -Path $itemFile -Value ("{`"productId`":`"$($prod.id)`",`"productName`":`"item`",`"quantity`":1,`"unitPrice`":10}") -NoNewline -Encoding ascii
            Post-Json "http://localhost:8080/api/orders/$($o.id)/items" $itemFile | Out-Null
        }
    }
    Write-Host "Seeded 30 orders x 2 items + 3 views for $($cust.email)"
}

function Run-K6([string]$Label, [string]$Script) {
    $out = docker compose -f $ComposeFile --profile load-test run --rm `
        -e VUS=50 -e DURATION=30s -e PAUSE=0.1 `
        k6 run "/scripts/scenarios/$Script" 2>&1 | Out-String
    $p95 = if ($out -match 'http_req_duration[^\r\n]*p\(95\)=([\d.]+(?:ms|s|µs)?)') { $matches[1] } else { "?" }
    $rps = if ($out -match 'http_reqs\.+?:\s+\d+\s+([\d.]+)/s') { $matches[1] } else { "?" }
    $fail = if ($out -match 'http_req_failed\.+?:\s+([\d.]+%)') { $matches[1] } else { "?" }
    $line = "$Label`t$Script`tp95=$p95`trps=$rps`tfail=$fail"
    Write-Host $line
    Add-Content -Path $report -Value $line
    return [pscustomobject]@{ Label = $Label; Script = $Script; P95 = $p95; RPS = $rps; Fail = $fail }
}

function Measure-Commit([string]$Commit, [string]$Label) {
    Write-Host ""
    Write-Host "=== $Label ($Commit) ===" -ForegroundColor Cyan
    Add-Content -Path $report -Value ""
    Add-Content -Path $report -Value "=== $Label ($Commit) ==="
    Push-Location $ProjectRoot
    git checkout $Commit --quiet
    Write-Host "Building image for $Commit ..."
    docker compose -f $ComposeFile up -d --build --force-recreate app 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "docker build failed for $Commit" }
    Pop-Location
    Wait-App
    Reset-State
    Seed-Workload
    $r1 = Run-K6 $Label "orders-list.js"
    Start-Sleep 5
    $r2 = Run-K6 $Label "recommendations-read.js"
    return @($r1, $r2)
}

("A/B batch vs N+1 - " + (Get-Date -Format o)) | Set-Content $report
try {
    $before = Measure-Commit "89f1807" "BEFORE-N+1"
    $after = Measure-Commit "856b3a7" "AFTER-BATCH"
} finally {
    Push-Location $ProjectRoot
    git checkout main --quiet
    Pop-Location
}

Write-Host ""
Write-Host "=== RESUMO ===" -ForegroundColor Green
Write-Host "Report: $report"
$before + $after | Format-Table -AutoSize
