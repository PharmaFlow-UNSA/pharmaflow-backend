# Start Pharmacy & Inventory Service - Instance 1 (Port 8084)
Write-Host "Starting Pharmacy & Inventory Service - Instance 1 (Port 8084)" -ForegroundColor Green
$env:PORT = "8084"
$env:EUREKA_INSTANCE_ID = "pharmacy-inventory-service-1"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8084 --eureka.instance.instance-id=pharmacy-inventory-service:8084"
