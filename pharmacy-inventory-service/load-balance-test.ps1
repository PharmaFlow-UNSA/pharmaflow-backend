# Load Balance Test Script - Pharmacy & Inventory Service
# Tests load balancing by sending 100 requests through Eureka and tracking instance distribution
# Uses /api/load-balancer-demo which calls /api/load-balance-test through @LoadBalanced RestTemplate

Write-Host "===================================================" -ForegroundColor Green
Write-Host "  Load Balancing Test - Pharmacy & Inventory Service" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""

$numberOfRequests = 100
$baseUrl = "http://localhost:8084/api/load-balancer-demo"
$results = @{}

Write-Host "Sending $numberOfRequests requests to load balancer..." -ForegroundColor Cyan
Write-Host ""

$startTime = Get-Date

for ($i = 1; $i -le $numberOfRequests; $i++) {
    try {
        $response = Invoke-RestMethod -Uri $baseUrl -Method GET -ErrorAction Stop
        $instanceId = $response.instanceId

        if ($results.ContainsKey($instanceId)) {
            $results[$instanceId]++
        } else {
            $results[$instanceId] = 1
        }

        Write-Progress -Activity "Load Balance Test" -Status "Request $i/$numberOfRequests" -PercentComplete (($i / $numberOfRequests) * 100)
    }
    catch {
        Write-Host "Error on request $i : $_" -ForegroundColor Red
    }
}

$endTime = Get-Date
$totalTime = ($endTime - $startTime).TotalSeconds

Write-Host ""
Write-Host "===================================================" -ForegroundColor Green
Write-Host "  Test Results" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Total Requests: $numberOfRequests"
Write-Host "Total Time: $($totalTime) seconds"
Write-Host "Average Time per Request: $([math]::Round($totalTime / $numberOfRequests, 3)) seconds"
Write-Host ""
Write-Host "Distribution across instances:" -ForegroundColor Yellow
Write-Host ""

$sortedResults = $results.GetEnumerator() | Sort-Object Name

foreach ($entry in $sortedResults) {
    $percentage = [math]::Round(($entry.Value / $numberOfRequests) * 100, 2)
    $bar = "#" * [math]::Floor($percentage / 2)
    Write-Host "$($entry.Key): " -NoNewline -ForegroundColor Cyan
    Write-Host "$bar" -NoNewline -ForegroundColor Green
    Write-Host " $($entry.Value) requests ($percentage%)" -ForegroundColor White
}

Write-Host ""
Write-Host "===================================================" -ForegroundColor Green
Write-Host "Test completed successfully!" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
