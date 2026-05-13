# PharmaFlow API Gateway

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Central API Gateway for PharmaFlow microservices architecture. Provides unified entry point with JWT authentication, rate limiting, circuit breaker, and comprehensive security features.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Security](#security)
- [API Documentation](#api-documentation)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)

---

## Features

### ✅ Core Functionality
- **Reactive Gateway** - Built on Spring Cloud Gateway (WebFlux) for high performance
- **Service Discovery** - Eureka integration with automatic load balancing
- **Dynamic Routing** - Route to 5 microservices with path-based predicates
- **Health Checks** - Built-in actuator endpoints for monitoring

### 🔐 Security
- **JWT Authentication** - Centralized token validation (HMAC-SHA256)
- **Token Blacklisting** - Redis-backed logout with in-memory fallback
- **Role-Based Access Control** - 4 roles with fine-grained permissions
- **Inter-Service Auth** - HMAC tokens for downstream microservices
- **Security Headers** - OWASP best practices (X-Frame-Options, CSP, HSTS, etc.)
- **CORS Protection** - Configurable origin whitelist

### 🛡️ Resilience
- **Circuit Breaker** - Resilience4j with fallback responses
- **Rate Limiting** - Per-user (100 req/min) and global (1000 req/s)
- **Retry Logic** - Automatic retry for transient failures
- **Timeouts** - Request/response timeout configuration

### 📊 Observability
- **Structured Logging** - JSON logs with correlation IDs
- **Metrics** - Actuator metrics for Prometheus
- **Tracing** - Request/response logging filter
- **Health Indicators** - Circuit breaker and service health

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      External Clients                        │
│           (Web App, Mobile App, Third-party APIs)           │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTPS (Port 8443)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Port 8080/8443)           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Security Filters:                                   │  │
│  │  1. SecurityHeadersFilter (OWASP headers)           │  │
│  │  2. LoggingFilter (request/response logging)        │  │
│  │  3. JwtAuthenticationFilter (JWT validation)        │  │
│  │  4. RateLimitFilter (abuse prevention)              │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Gateway Features:                                   │  │
│  │  • Circuit Breaker (Resilience4j)                   │  │
│  │  • Load Balancing (Eureka)                          │  │
│  │  • Token Blacklist (Redis + In-memory fallback)     │  │
│  │  • CORS Configuration                                │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────┬───────┬────────┬────────┬────────┬────────────┘
             │       │        │        │        │
             ▼       ▼        ▼        ▼        ▼
┌──────────────────────────────────────────────────────────────┐
│                      Microservices Layer                      │
├───────────┬───────────┬───────────┬───────────┬──────────────┤
│  User &   │ Product & │  Order &  │ Pharmacy &│   Smart      │
│  Health   │  Medical  │Prescription│ Inventory │  Features    │
│  Service  │  Service  │  Service  │  Service  │  Service     │
│ (8081)    │  (8082)   │  (8083)   │  (8084)   │  (8085)      │
└───────────┴───────────┴───────────┴───────────┴──────────────┘
             │           │            │
             ▼           ▼            ▼
┌──────────────────────────────────────────────────────────────┐
│                     Data Layer                                │
├────────────────────┬────────────────┬─────────────────────────┤
│   PostgreSQL       │     Redis      │    External APIs        │
│   (Persistent)     │   (Caching)    │   (Third-party)         │
└────────────────────┴────────────────┴─────────────────────────┘
```

---

## Prerequisites

### Required
- **Java** 17 or higher
- **Maven** 3.8+
- **Eureka Server** running on port 8761

### Optional (but recommended)
- **Redis** 7.0+ for token blacklisting (falls back to in-memory if not available)
- **Docker** for containerized deployment
- **SSL Certificate** for HTTPS (self-signed for dev, CA-signed for prod)

---

## Quick Start

### 1. Clone and Navigate
```bash
git clone https://github.com/pharmaflow/pharmaflow-backend.git
cd pharmaflow-backend/api-gateway
```

### 2. Configure Environment Variables
```bash
# Required
export JWT_SECRET="pharmaflow-secret-key-2024-very-long-and-secure-key-for-production"
export INTERNAL_SERVICE_SECRET="pharmaflow-internal-secret-2024-very-secure"

# Optional (Redis)
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

# Optional (Eureka)
export EUREKA_SERVER="http://localhost:8761/eureka/"
```

### 3. Start Redis (Optional)
```bash
# Using Docker
docker run -d --name pharmaflow-redis -p 6379:6379 redis:7-alpine

# Or using local installation
redis-server
```

### 4. Build and Run
```bash
# Build
mvn clean install

# Run in development mode
mvn spring-boot:run

# Or run the JAR
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### 5. Verify Installation
```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","groups":["liveness","readiness"]}
```

---

## Configuration

### Application Profiles

The gateway supports multiple profiles:

| Profile | Port | SSL | Redis | Use Case |
|---------|------|-----|-------|----------|
| `default` | 8080 | No | Optional | Development |
| `prod` | 8443 | Yes | Required | Production |

#### Switch Profiles
```bash
# Development
java -jar api-gateway.jar

# Production
java -jar api-gateway.jar --spring.profiles.active=prod
```

### Environment Variables

#### Security Configuration
```bash
# JWT Settings (NEVER commit these to version control!)
JWT_SECRET=<generate-with-openssl-rand>          # REQUIRED in production
JWT_EXPIRATION=86400000                          # 24 hours in milliseconds

# Internal Service Communication (NEVER commit these!)
INTERNAL_SERVICE_SECRET=<generate-with-openssl-rand>  # REQUIRED in production

# SSL/TLS (Production only)
SSL_KEY_STORE_PASSWORD=<your-keystore-password>
```

**⚠️ SECURITY WARNING:**
- **NEVER** commit secrets to version control
- **NEVER** use default/example values in production
- Use environment variables or secret management tools (HashiCorp Vault, AWS Secrets Manager)
- Rotate secrets regularly (every 90 days recommended)

#### Redis Configuration
```bash
REDIS_HOST=localhost                              # Default: localhost
REDIS_PORT=6379                                   # Default: 6379
REDIS_PASSWORD=                                   # Leave empty for no password
```

#### Service Discovery
```bash
EUREKA_SERVER=http://localhost:8761/eureka/      # Eureka service URL
```

#### CORS Configuration
```bash
# Comma-separated list of allowed origins
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200,https://pharmaflow.ba
```

### Generate Secure Secrets

```bash
# Generate JWT secret (256-bit)
openssl rand -base64 32

# Generate internal service secret
openssl rand -base64 32

# Example output:
# kJ8x3yP2mN5vQ9wR1tY6uI4oP0aS7dF2gH3jK8lZ9xC=
```

### SSL Certificate Generation

For **development/testing**:
```bash
# Windows
cd scripts
.\generate-ssl-cert.ps1

# Linux/Mac
cd scripts
chmod +x generate-ssl-cert.sh
./generate-ssl-cert.sh
```

For **production**, use Let's Encrypt:
```bash
sudo certbot certonly --standalone -d api.pharmaflow.ba

sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/api.pharmaflow.ba/fullchain.pem \
  -inkey /etc/letsencrypt/live/api.pharmaflow.ba/privkey.pem \
  -out src/main/resources/keystore.p12 \
  -name pharmaflow-gateway
```

---

## Security

### Authentication Flow

```
┌─────────┐                                  ┌─────────┐
│ Client  │                                  │ Gateway │
└────┬────┘                                  └────┬────┘
     │                                            │
     │  POST /api/auth/login                    │
     │  {email, password}                        │
     ├──────────────────────────────────────────▶│
     │                                            │
     │  200 OK                                    │
     │  {accessToken, refreshToken}              │
     │◀──────────────────────────────────────────┤
     │                                            │
     │  GET /api/users/me                        │
     │  Authorization: Bearer {accessToken}      │
     ├──────────────────────────────────────────▶│
     │                                            │
     │                          ┌─────────────────┤
     │                          │ 1. Validate JWT │
     │                          │ 2. Check blacklist (Redis)
     │                          │ 3. Extract roles │
     │                          │ 4. Check permissions
     │                          │ 5. Add internal token
     │                          └─────────────────┤
     │                                            │
     │                                            │ Forward with headers:
     │                                            │ X-Username, X-Roles,
     │                                            │ X-Internal-Token
     │                                            ├────────────────────▶
     │                                            │              Microservice
     │                                            │◀────────────────────┤
     │  200 OK {user data}                       │
     │◀──────────────────────────────────────────┤
     │                                            │
```

### Roles and Permissions

| Role | Access Rights |
|------|---------------|
| **ROLE_ADMIN** | Full access to all endpoints |
| **ROLE_PHARMACIST** | Inventory management, order fulfillment |
| **ROLE_DOCTOR** | Prescriptions, patient profiles |
| **ROLE_USER** | Read own data, create orders |

### Protected Endpoints

| Path | Required Role | Description |
|------|---------------|-------------|
| `/api/auth/**` | Public | Login, register, refresh |
| `/api/users/me` | Authenticated | Current user info |
| `/api/prescriptions/**` | DOCTOR, PHARMACIST | Prescription management |
| `/api/inventory/**` | PHARMACIST | Inventory operations |
| `/api/patient-profiles/**` | DOCTOR, USER | Health profiles |
| `/actuator/health` | Public | Health check |

### Token Blacklisting (Logout)

Tokens are invalidated on logout:

```bash
# Logout request
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token>"}'

# Subsequent requests with same token will fail
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <access-token>"
# Response: 401 Unauthorized - "Token has been revoked"
```

**Storage**:
- **Redis** (preferred): Distributed, automatic TTL cleanup
- **In-Memory** (fallback): Single-node, periodic cleanup
  - ⚠️ Not suitable for production with multiple gateway instances

---

## API Documentation

### Base URL
- Development: `http://localhost:8080`
- Production: `https://api.pharmaflow.ba`

### Service Routes

| Service | Base Path | Target |
|---------|-----------|--------|
| User & Health | `/api/users/**`, `/api/auth/**`, `/api/family-members/**`, `/api/allergies/**`, `/api/therapies/**`, `/api/patient-profiles/**` | `lb://USER-HEALTH-SERVICE` |
| Product & Medical | `/api/products/**`, `/api/categories/**`, `/api/substances/**`, `/api/interactions/**`, `/api/contraindications/**`, `/api/substitutes/**` | `lb://PRODUCT-HEALTH-SERVICE` |
| Order & Prescription | `/api/orders/**`, `/api/prescriptions/**`, `/api/payments/**` | `lb://ORDER-PRESCRIPTION-SERVICE` |
| Pharmacy & Inventory | `/api/pharmacies/**`, `/api/inventory/**`, `/api/reservations/**` | `lb://PHARMACY-INVENTORY-SERVICE` |
| Smart Features | `/api/symptoms/**`, `/api/recommendations/**`, `/api/notifications/**`, `/api/fraud/**` | `lb://SMART-FEATURES-SERVICE` |

### Example Requests

#### 1. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@pharmaflow.ba",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "email": "doctor@pharmaflow.ba",
  "roles": ["ROLE_DOCTOR"]
}
```

#### 2. Authenticated Request
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 3. Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

#### 4. Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token>"}'
```

---

## Monitoring

### Actuator Endpoints

| Endpoint | Description | Access |
|----------|-------------|--------|
| `/actuator/health` | Application health status | Public |
| `/actuator/info` | Application information | Public |
| `/actuator/metrics` | Prometheus metrics | Restricted |
| `/actuator/routes` | Gateway route configuration | Restricted |
| `/actuator/circuitbreakers` | Circuit breaker status | Restricted |

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

### View Active Routes
```bash
curl http://localhost:8080/actuator/routes | jq
```

### Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### Metrics (Prometheus Format)
```bash
curl http://localhost:8080/actuator/prometheus
```

---

## Troubleshooting

### Common Issues

#### 1. Redis Connection Failed
**Symptom:** Warning on startup: "Redis not configured - using in-memory blacklist"

**Solution:**
- The gateway will work with in-memory fallback (development only)
- For production, start Redis:
  ```bash
  docker run -d -p 6379:6379 redis:7-alpine
  ```
- Verify connection:
  ```bash
  redis-cli ping
  # Expected: PONG
  ```

#### 2. Eureka Registration Failed
**Symptom:** "Cannot resolve eureka server"

**Solution:**
- Start Eureka server first
- Verify Eureka is running:
  ```bash
  curl http://localhost:8761/eureka/apps
  ```
- Check `EUREKA_SERVER` environment variable

#### 3. JWT Validation Failed
**Symptom:** 401 Unauthorized - "Invalid or expired token"

**Possible causes:**
- Token expired (24h lifetime)
- Token blacklisted (after logout)
- Wrong JWT_SECRET in gateway vs user-service
- Token malformed

**Solution:**
- Get new token via login
- Verify JWT_SECRET matches across services
- Check token expiration:
  ```bash
  # Decode JWT (without verification)
  echo "eyJhbGci..." | cut -d'.' -f2 | base64 -d | jq
  ```

#### 4. 403 Forbidden - Insufficient Permissions
**Symptom:** "Insufficient permissions"

**Solution:**
- Verify user has required role
- Check role-path mapping in `JwtAuthenticationFilter.hasAccessToPath()`
- Example: `/api/prescriptions` requires `ROLE_DOCTOR` or `ROLE_PHARMACIST`

#### 5. 429 Too Many Requests
**Symptom:** "Rate limit exceeded"

**Solution:**
- Wait 1 minute (100 requests per minute per user)
- For testing, increase limit in `application.properties`:
  ```properties
  resilience4j.ratelimiter.instances.apiGateway.limit-for-period=1000
  ```

#### 6. 503 Service Unavailable - Circuit Breaker Open
**Symptom:** Fallback response returned

**Solution:**
- Check if downstream microservice is running
- Verify service registered in Eureka:
  ```bash
  curl http://localhost:8761/eureka/apps
  ```
- Circuit will auto-recover after 30 seconds (half-open state)

### Debug Logging

Enable debug logging for troubleshooting:

```properties
# application.properties
logging.level.com.pharmaflow.gateway=DEBUG
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.io.github.resilience4j=DEBUG
```

Or via command line:
```bash
java -jar api-gateway.jar --logging.level.com.pharmaflow.gateway=DEBUG
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Generate production JWT_SECRET (256-bit)
- [ ] Generate production INTERNAL_SERVICE_SECRET
- [ ] Obtain SSL certificate (Let's Encrypt or CA)
- [ ] Configure Redis cluster (high availability)
- [ ] Set up load balancer
- [ ] Configure firewall rules
- [ ] Set up monitoring (Prometheus + Grafana)
- [ ] Set up logging (ELK stack)
- [ ] Configure CORS whitelist for production domains
- [ ] Test all endpoints with production configuration
- [ ] Document incident response procedures

### Docker Deployment

```bash
# Build image
docker build -t pharmaflow/api-gateway:1.0.0 .

# Run container
docker run -d \
  --name api-gateway \
  -p 8443:8443 \
  -p 9443:9443 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=$JWT_SECRET \
  -e INTERNAL_SERVICE_SECRET=$INTERNAL_SERVICE_SECRET \
  -e REDIS_HOST=redis \
  -e REDIS_PASSWORD=$REDIS_PASSWORD \
  -e EUREKA_SERVER=https://eureka-server:8761/eureka/ \
  pharmaflow/api-gateway:1.0.0
```

### Kubernetes Deployment

See `PRODUCTION_DEPLOYMENT_GUIDE.md` for complete Kubernetes manifests.

```bash
# Create namespace
kubectl create namespace pharmaflow

# Create secrets
kubectl create secret generic pharmaflow-secrets \
  --from-literal=jwt-secret=$JWT_SECRET \
  --from-literal=internal-secret=$INTERNAL_SERVICE_SECRET \
  -n pharmaflow

# Deploy
kubectl apply -f k8s/api-gateway-deployment.yaml
```

### Performance Tuning

#### JVM Options
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xms512m \
     -Xmx1024m \
     -jar api-gateway.jar
```

#### Connection Pool Sizing
```properties
# application-prod.properties
spring.cloud.gateway.httpclient.pool.max-connections=500
spring.cloud.gateway.httpclient.pool.max-idle-time=30s
```

### Scaling Recommendations

| Load | Gateway Instances | Redis | Recommendations |
|------|-------------------|-------|-----------------|
| < 100 req/s | 1 | Single node | Development/staging |
| 100-1000 req/s | 2-3 | Sentinel (HA) | Small production |
| 1000-10000 req/s | 5-10 | Cluster | Medium production |
| > 10000 req/s | 10+ | Cluster + CDN | Large enterprise |

---

## Contributing

Please read [CONTRIBUTING.md](../CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

---

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

## Support

- **Documentation:** [Full documentation](../SECURITY_IMPLEMENTATION.md)
- **Deployment Guide:** [Production deployment](../PRODUCTION_DEPLOYMENT_GUIDE.md)
- **Issues:** [GitHub Issues](https://github.com/pharmaflow/pharmaflow-backend/issues)
- **Email:** devops@pharmaflow.ba

---

**Version:** 1.0.0  
**Last Updated:** May 8, 2026  
**Maintainer:** PharmaFlow Engineering Team

