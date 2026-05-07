# Start Instance 3 on port 8083
Write-Host "Starting User Health Service - Instance 3 (Port 8083)" -ForegroundColor Yellow
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8083 --eureka.instance.instance-id=user-health-service:8083"

