# User & Health Service (Microservice 1)

This service manages user profiles, Family Hub, and digital health records.

## Technologies
- Java 17 / Spring Boot 3.1.5
- PostgreSQL
- Spring Data JPA
- BCrypt Password Encoding
- SpringDoc OpenAPI (Swagger)

## Features
- [x] User registration and management
- [x] Family Hub (family member management)
- [x] Digital health records (Allergies and Therapies)
- [x] Secure password storage (BCrypt)
- [x] Professional validation (OWASP standard)
- [x] N+1 query prevention (@EntityGraph)
- [x] Comprehensive exception handling

## Documentation
- **⚡ Quick Start:** `QUICK_START.md` - Fast deployment guide
- **📖 Swagger UI (Interactive):** http://localhost:8080/swagger
- **📋 OpenAPI Spec (JSON):** http://localhost:8080/api-docs

## Quick Start

1. Set environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
2. Run: `./mvnw spring-boot:run`
3. Open Swagger UI: http://localhost:8080/swagger
4. Database will auto-populate with 10 test users (ID 1-10)

## Testing
```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=UserServiceTest
```

## Deployment

See `QUICK_START.md` for complete deployment instructions including:
- PostgreSQL setup (Docker or local)
- Environment configuration
- Building and running
- Swagger UI usage
- Postman testing
- N+1 query verification

