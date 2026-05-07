# Start Instance 1 on port 8081
Write-Host "Starting User Health Service - Instance 1 (Port 8081)" -ForegroundColor Green
$env:PORT = "8081"
$env:EUREKA_INSTANCE_ID = "user-health-service-1"
mvn spring-boot:run

