# Start Order & Prescription Service - Instance 3 (Port 8089)
Write-Host "Starting Order & Prescription Service - Instance 3 (Port 8089)" -ForegroundColor Yellow
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8089 --eureka.instance.instance-id=order-prescription-service:8089"
