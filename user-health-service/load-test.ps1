# Load Testing Script for PharmaFlow Microservices
# This script tests load balancing by sending 100 requests to the User Health Service

param(
    [string]$ServiceUrl = "http://localhost:8081",
    [int]$RequestCount = 100,
    [int]$ConcurrentRequests = 10
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PharmaFlow Load Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target Service: $ServiceUrl" -ForegroundColor Yellow
Write-Host "Total Requests: $RequestCount" -ForegroundColor Yellow
Write-Host "Concurrent Requests: $ConcurrentRequests" -ForegroundColor Yellow
Write-Host ""

# Statistics variables
$global:successCount = 0
$global:failureCount = 0
$global:totalResponseTime = 0
$global:responses = @()
$global:instanceCounts = @{}

# Function to send a single request
function Send-TestRequest {
    param([int]$requestNumber)

    try {
        $startTime = Get-Date
        $response = Invoke-WebRequest -Uri "$ServiceUrl/api/users" -Method GET -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds

        # Extract instance info from response headers if available
        $instanceId = $response.Headers["X-Instance-Id"]
        if (-not $instanceId) {
            $instanceId = "unknown"
        }

        # Update statistics
        $global:successCount++
        $global:totalResponseTime += $responseTime

        # Count instances
        if ($global:instanceCounts.ContainsKey($instanceId)) {
            $global:instanceCounts[$instanceId]++
        } else {
            $global:instanceCounts[$instanceId] = 1
        }

        $global:responses += @{
            RequestNumber = $requestNumber
            StatusCode = $response.StatusCode
            ResponseTime = $responseTime
            InstanceId = $instanceId
        }

        Write-Host "Request #$requestNumber : SUCCESS (${responseTime}ms) - Instance: $instanceId" -ForegroundColor Green

    } catch {
        $global:failureCount++
        Write-Host "Request #$requestNumber : FAILED - $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Main execution
Write-Host "Starting load test..." -ForegroundColor Cyan
Write-Host ""

$overallStartTime = Get-Date

# Send requests in batches
for ($i = 1; $i -le $RequestCount; $i += $ConcurrentRequests) {
    $jobs = @()
    $batchEnd = [Math]::Min($i + $ConcurrentRequests - 1, $RequestCount)

    for ($j = $i; $j -le $batchEnd; $j++) {
        $jobs += Start-Job -ScriptBlock {
            param($url, $reqNum)

            try {
                $startTime = Get-Date
                $response = Invoke-WebRequest -Uri "$url/api/users" -Method GET -UseBasicParsing -TimeoutSec 30
                $endTime = Get-Date
                $responseTime = ($endTime - $startTime).TotalMilliseconds

                $instanceId = $response.Headers["X-Instance-Id"]
                if (-not $instanceId) {
                    $instanceId = "instance-1"
                }

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

    # Wait for all jobs in the batch to complete
    $results = $jobs | Wait-Job | Receive-Job
    $jobs | Remove-Job

    # Process results
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

# Display results
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

# Display instance distribution
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

