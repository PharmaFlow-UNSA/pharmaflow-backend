# Quick Start Guide

## 1. PostgreSQL Setup

### Option A: Local PostgreSQL
```sql
CREATE DATABASE pharmaflow_user_db;
```

### Option B: Docker
```powershell
docker run -d --name pharmaflow-postgres `
  -e POSTGRES_DB=pharmaflow_user_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 postgres:15
```

## 2. Environment Configuration

Edit `src/main/resources/application-local.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pharmaflow_user_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

## 3. Build and Run

```powershell
./mvnw spring-boot:run
```

Or click green arrow in IntelliJ on `UserHealthServiceApplication`

## 4. Swagger UI

Open: http://localhost:8080/swagger

Test endpoints:
- `GET /api/users` - View 10 seeded users
- `POST /api/users` - Create new user

## 5. Postman Testing

Import OpenAPI spec:
1. Open Postman → Import
2. URL: `http://localhost:8080/api-docs`
3. All endpoints auto-generated!

Example request:
```json
POST http://localhost:8080/api/users
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@pharmaflow.ba",
  "password": "TestPass123!",
  "patientProfile": {
    "weight": 70.0,
    "height": 175.0,
    "bloodType": "O+"
  }
}
```

## 6. N+1 Query Verification

Make request: `GET /api/users/1`

Check console logs for:
```
Session Metrics {
    statements prepared: 1  ✅ Single query (N+1 prevented)
}
```

## 7. Run Tests

```powershell
./mvnw test
```

## Troubleshooting

**Can't connect to database?**
- Verify PostgreSQL is running
- Check password in `application-local.properties`

**Port 8080 in use?**
```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess
Stop-Process -Id <PID> -Force
```

---

✅ **Ready!** Access http://localhost:8080/swagger


