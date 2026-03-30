# Smart Features Service

Ovaj servis je zaseban Spring Boot mikroservis za feature flagove i pametne funkcionalnosti.

## Ukljuceno

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Lombok

## Kako pokrenuti

1. Provjerite da PostgreSQL radi na portu `4444`.
2. Po potrebi podesite `DB_URL`, `DB_USERNAME` i `DB_PASSWORD`.
3. Pokrenite servis iz IntelliJ-a ili komandom `./mvnw spring-boot:run`.

Default lokalna konekcija:

```text
DB_URL=jdbc:postgresql://localhost:4444/pharmaflow
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Servis se podize na portu `8082`.
