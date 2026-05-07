# Start Order & Prescription Service - Instance 1 (Port 8087)
Write-Host "Starting Order & Prescription Service - Instance 1 (Port 8087)" -ForegroundColor Green
$env:PORT = "8087"
$env:EUREKA_INSTANCE_ID = "order-prescription-service-1"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8087 --eureka.instance.instance-id=order-prescription-service:8087"
