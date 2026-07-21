param(
    [int]$Vus = 200,
    [string]$Duration = "45s",
    [int]$GraphPeers = 100,
    [int]$GraphProducts = 5,
    [double]$Pause = 0.05,
    [string]$ComposeFile = (Join-Path $PSScriptRoot "..\docker-compose.yml")
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path $ComposeFile -Parent
$ResultsDir = Join-Path $ProjectRoot "load-tests\results"
New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportFile = Join-Path $ResultsDir "stress-$timestamp.txt"

function Get-ThreadMetric([string]$Name) {
    try {
        $json = curl.exe -s "http://localhost:8080/actuator/metrics/$Name"
        $obj = $json | ConvertFrom-Json
        return [math]::Round([double]$obj.measurements[0].value, 0)
    } catch {
        return -1
    }
}

function Write-Report([string]$Text) {
    Write-Host $Text
    Add-Content -Path $reportFile -Value $Text
}

$tests = @(
    @{
        Name     = "recommendations-seed-baseline"
        File     = "scenarios/recommendations-read.js"
        Vus      = 50
        Duration = "30s"
        Pause    = 0.1
        Note     = "Grafo DevSeed apenas (3 clientes) - baseline da suite"
        Graph    = $false
    },
    @{
        Name     = "recommendations-dense-graph"
        File     = "scenarios/recommendations-read-stress.js"
        Vus      = $Vus
        Duration = $Duration
        Pause    = $Pause
        Note     = "Grafo denso (peers=$GraphPeers) + alta concorrencia VU"
        Graph    = $true
    },
    @{
        Name     = "products-list-suite-baseline"
        File     = "scenarios/products-list.js"
        Vus      = 50
        Duration = "30s"
        Pause    = 0.1
        Note     = "Ja existia na suite - GET /api/products @ 50 VU"
        Graph    = $false
    },
    @{
        Name     = "products-list-high-concurrency"
        File     = "scenarios/products-list-stress.js"
        Vus      = $Vus
        Duration = $Duration
        Pause    = $Pause
        Note     = "Stress de conexoes - foco em threads, nao em bater JPA p95"
        Graph    = $false
    }
)

Write-Report ""
Write-Report "=== Stress WebFlux - $timestamp ==="
Write-Report "Default stress: $Vus VUs / $Duration / pause=$Pause"
Write-Report "Graph densify: peers=$GraphPeers overlapProducts=$GraphProducts"
Write-Report "Idle threads live=$(Get-ThreadMetric 'jvm.threads.live') peak=$(Get-ThreadMetric 'jvm.threads.peak')"
Write-Report ""

$summary = @()

foreach ($test in $tests) {
    Write-Report ">> $($test.Name) - $($test.Note)"
    $liveBefore = Get-ThreadMetric "jvm.threads.live"
    $peakBefore = Get-ThreadMetric "jvm.threads.peak"

    $envArgs = @(
        "-e", "VUS=$($test.Vus)",
        "-e", "DURATION=$($test.Duration)",
        "-e", "PAUSE=$($test.Pause)"
    )
    if ($test.Graph) {
        $envArgs += @("-e", "GRAPH_PEERS=$GraphPeers", "-e", "GRAPH_PRODUCTS=$GraphProducts", "-e", "GRAPH_EXTRA=3")
    }

    $sampler = Start-Job -ScriptBlock {
        $maxLive = 0
        $maxPeak = 0
        for ($i = 0; $i -lt 90; $i++) {
            try {
                $liveJson = curl.exe -s "http://localhost:8080/actuator/metrics/jvm.threads.live"
                $peakJson = curl.exe -s "http://localhost:8080/actuator/metrics/jvm.threads.peak"
                $live = [double](($liveJson | ConvertFrom-Json).measurements[0].value)
                $peak = [double](($peakJson | ConvertFrom-Json).measurements[0].value)
                if ($live -gt $maxLive) { $maxLive = $live }
                if ($peak -gt $maxPeak) { $maxPeak = $peak }
            } catch { }
            Start-Sleep -Seconds 2
        }
        return @{ MaxLive = $maxLive; MaxPeak = $maxPeak }
    }

    $output = docker compose -f $ComposeFile --profile load-test run --rm `
        @envArgs `
        k6 run "/scripts/$($test.File)" 2>&1 | Out-String

    Stop-Job $sampler -ErrorAction SilentlyContinue
    $sample = Receive-Job $sampler -ErrorAction SilentlyContinue
    Remove-Job $sampler -Force -ErrorAction SilentlyContinue

    $reqs = if ($output -match 'http_reqs\.+?:\s+(\d+)') { $matches[1] } else { "?" }
    $rps = if ($output -match 'http_reqs\.+?:\s+\d+\s+([\d.]+)/s') { $matches[1] } else { "?" }
    $p95 = if ($output -match 'http_req_duration[^\r\n]*p\(95\)=([\d.]+(?:ms|s|µs|us|ns)?)') { $matches[1] } else { "?" }
    $failed = if ($output -match 'http_req_failed\.+?:\s+([\d.]+%)') { $matches[1] } else { "?" }
    $checks = if ($output -match 'checks\.+?:\s+([\d.]+%)') { $matches[1] } else { "?" }

    $liveAfter = Get-ThreadMetric "jvm.threads.live"
    $peakAfter = Get-ThreadMetric "jvm.threads.peak"
    $sampledLive = if ($sample -and $sample.MaxLive) { [math]::Round($sample.MaxLive, 0) } else { $liveAfter }
    $sampledPeak = if ($sample -and $sample.MaxPeak) { [math]::Round($sample.MaxPeak, 0) } else { $peakAfter }

    $row = [PSCustomObject]@{
        Test        = $test.Name
        VUs         = $test.Vus
        Reqs        = $reqs
        RPS         = $rps
        P95         = $p95
        Falhas      = $failed
        Checks      = $checks
        ThreadsLive = $sampledLive
        ThreadsPeak = $sampledPeak
        LiveBefore  = $liveBefore
        PeakBefore  = $peakBefore
    }
    $summary += $row

    Write-Report ("   p95={0} rps={1} fail={2} threads live~{3} peak~{4}" -f $p95, $rps, $failed, $sampledLive, $sampledPeak)
    Start-Sleep -Seconds 5
}

Write-Report ""
Write-Report "=== RESUMO ==="
$table = $summary | Format-Table -AutoSize | Out-String
Write-Report $table
Write-Report "Relatorio: $reportFile"
Write-Report "Leitura: p95 alto no grafo denso e esperado (mais hops Redis/R2DBC)."
Write-Report "O sinal WebFlux e ThreadsLive/Peak estaveis com VUs altas - sem pool Tomcat crescendo com a concorrencia."
