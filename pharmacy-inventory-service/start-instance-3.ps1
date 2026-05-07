# Start Pharmacy & Inventory Service - Instance 3 (Port 8086)
Write-Host "Starting Pharmacy & Inventory Service - Instance 3 (Port 8086)" -ForegroundColor Yellow
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8086 --eureka.instance.instance-id=pharmacy-inventory-service:8086"
