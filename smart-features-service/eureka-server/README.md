# Eureka Server

Local Eureka registry for running and testing `smart-features-service`.

## Run

From this directory:

```bash
../mvnw spring-boot:run
```

Or from `smart-features-service`:

```bash
./mvnw -f eureka-server/pom.xml spring-boot:run
```

The server listens on:

```text
http://localhost:8761
```

## Verify

```bash
curl http://localhost:8761/actuator/health
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
```

## Run smart-features-service against it

```bash
EUREKA_SERVER_URL=http://localhost:8761/eureka/ ./mvnw spring-boot:run
```
