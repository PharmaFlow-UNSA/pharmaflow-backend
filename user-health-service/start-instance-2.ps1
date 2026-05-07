# Script for starting a second instance of User Health Service on port 8082
# This demonstrates load balancing with Eureka

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting User Health Service Instance 2" -ForegroundColor Cyan
Write-Host "Port: 8082" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Starting service on port 8082..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the service" -ForegroundColor Gray
Write-Host ""

# Start Maven with proper Spring Boot arguments
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082 --eureka.instance.instance-id=user-health-service:8082"


