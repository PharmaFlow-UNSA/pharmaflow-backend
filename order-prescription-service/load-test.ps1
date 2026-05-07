# Load Testing Script for Order & Prescription Service
# Sends 100 requests to /api/orders and measures throughput

param(
    [string]$ServiceUrl = "http://localhost:8087",
    [int]$RequestCount = 100,
    [int]$ConcurrentRequests = 10
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Order & Prescription Load Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target Service: $ServiceUrl" -ForegroundColor Yellow
Write-Host "Total Requests: $RequestCount" -ForegroundColor Yellow
Write-Host "Concurrent Requests: $ConcurrentRequests" -ForegroundColor Yellow
Write-Host ""

$global:successCount = 0
$global:failureCount = 0
$global:totalResponseTime = 0
$global:responses = @()
$global:instanceCounts = @{}

Write-Host "Starting load test..." -ForegroundColor Cyan
Write-Host ""

$overallStartTime = Get-Date

for ($i = 1; $i -le $RequestCount; $i += $ConcurrentRequests) {
    $jobs = @()
    $batchEnd = [Math]::Min($i + $ConcurrentRequests - 1, $RequestCount)

    for ($j = $i; $j -le $batchEnd; $j++) {
        $jobs += Start-Job -ScriptBlock {
            param($url, $reqNum)

            try {
                $startTime = Get-Date
                $response = Invoke-WebRequest -Uri "$url/api/orders" -Method GET -UseBasicParsing -TimeoutSec 30
                $endTime = Get-Date
                $responseTime = ($endTime - $startTime).TotalMilliseconds

                $instanceId = $response.Headers["X-Instance-Id"]
                if (-not $instanceId) { $instanceId = "instance-1" }

                return @{
                    Success = $true
                    RequestNumber = $reqNum
                    StatusCode = $response.StatusCode
                    ResponseTime = $responseTime
                    InstanceId = $instanceId
                }
            } catch {
                return @{
                    Success = $false
                    RequestNumber = $reqNum
                    Error = $_.Exception.Message
                }
            }
        } -ArgumentList $ServiceUrl, $j
    }

    $results = $jobs | Wait-Job | Receive-Job
    $jobs | Remove-Job

    foreach ($result in $results) {
        if ($result.Success) {
            $global:successCount++
            $global:totalResponseTime += $result.ResponseTime

            $instanceId = $result.InstanceId
            if ($global:instanceCounts.ContainsKey($instanceId)) {
                $global:instanceCounts[$instanceId]++
            } else {
                $global:instanceCounts[$instanceId] = 1
            }

            $global:responses += $result
            Write-Host "Request #$($result.RequestNumber) : SUCCESS ($([Math]::Round($result.ResponseTime, 2))ms) - Instance: $instanceId" -ForegroundColor Green
        } else {
            $global:failureCount++
            Write-Host "Request #$($result.RequestNumber) : FAILED - $($result.Error)" -ForegroundColor Red
        }
    }
}

$overallEndTime = Get-Date
$totalDuration = ($overallEndTime - $overallStartTime).TotalSeconds

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Load Test Results" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total Requests: $RequestCount" -ForegroundColor White
Write-Host "Successful: $global:successCount" -ForegroundColor Green
Write-Host "Failed: $global:failureCount" -ForegroundColor Red
Write-Host "Success Rate: $([Math]::Round(($global:successCount / $RequestCount) * 100, 2))%" -ForegroundColor Yellow
Write-Host ""

if ($global:successCount -gt 0) {
    $avgResponseTime = $global:totalResponseTime / $global:successCount
    Write-Host "Average Response Time: $([Math]::Round($avgResponseTime, 2))ms" -ForegroundColor Yellow

    $sortedTimes = $global:responses | Where-Object { $_.ResponseTime } | Sort-Object ResponseTime
    $minTime = $sortedTimes[0].ResponseTime
    $maxTime = $sortedTimes[-1].ResponseTime

    Write-Host "Min Response Time: $([Math]::Round($minTime, 2))ms" -ForegroundColor Yellow
    Write-Host "Max Response Time: $([Math]::Round($maxTime, 2))ms" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Total Duration: $([Math]::Round($totalDuration, 2))s" -ForegroundColor Yellow
Write-Host "Requests per Second: $([Math]::Round($RequestCount / $totalDuration, 2))" -ForegroundColor Yellow
Write-Host ""

if ($global:instanceCounts.Count -gt 0) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Load Balancing Distribution" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""

    foreach ($instance in $global:instanceCounts.Keys | Sort-Object) {
        $count = $global:instanceCounts[$instance]
        $percentage = ($count / $global:successCount) * 100
        Write-Host "Instance $instance : $count requests ($([Math]::Round($percentage, 2))%)" -ForegroundColor Cyan
    }
    Write-Host ""
}

Write-Host "Load test completed!" -ForegroundColor Green
