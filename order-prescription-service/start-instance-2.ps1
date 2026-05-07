# Start Order & Prescription Service - Instance 2 (Port 8088)
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Order & Prescription Service Instance 2" -ForegroundColor Cyan
Write-Host "Port: 8088" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Starting service on port 8088..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Gray
Write-Host ""

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8088 --eureka.instance.instance-id=order-prescription-service:8088"
