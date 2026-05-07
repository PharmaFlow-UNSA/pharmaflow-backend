# Start Pharmacy & Inventory Service - Instance 2 (Port 8085)
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Pharmacy & Inventory Service Instance 2" -ForegroundColor Cyan
Write-Host "Port: 8085" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Starting service on port 8085..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Gray
Write-Host ""

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8085 --eureka.instance.instance-id=pharmacy-inventory-service:8085"
