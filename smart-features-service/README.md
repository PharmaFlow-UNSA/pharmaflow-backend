# Smart Features Service

`smart-features-service` je zaseban Spring Boot mikroservis za lokalne smart funkcionalnosti bez komunikacije sa drugim mikroservisima. Strani identifikatori poput `userId`, `patientProfileId`, `productId` i `orderId` tretiraju se kao reference.

## Ukljuceno

- Symptom CRUD, search, search sessions i symptom-product matches
- Personalized recommendations i interaction audit
- Therapy reminders i notifications
- FAQ management i chatbot / tele-pharmacy chat session flow
- Fraud rules, fraud checks i audit logovi
- DTO validacija + entity guardrail validacija
- Flyway migracije za normalized i unique rollout scenarije

## Zahtjevi

- JDK `17`
- Maven wrapper (`./mvnw`)
- PostgreSQL za lokalni run

## Kako pokrenuti

1. Podesite `DB_URL`, `DB_USERNAME` i `DB_PASSWORD`.
2. Pokrenite servis komandom `./mvnw spring-boot:run`.
3. Za testove koristite `./mvnw clean test`.

Primjer lokalne konekcije:

```text
DB_URL=jdbc:postgresql://localhost:4444/pharmaflow
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

Servis se podize na portu `8082`.

## IntelliJ setup

1. Otvorite projekat preko root `pom.xml` ili direktno `smart-features-service/pom.xml`.
2. Postavite Project SDK na JDK `17`.
3. U IntelliJ-u ukljucite:
   `Build, Execution, Deployment > Build Tools > Maven > Runner > Delegate IDE build/run actions to Maven`
4. Pokrenite Maven reimport.
5. Test runner neka bude `JUnit 5`.

U repo-u su dodane i dijeljene `.run` konfiguracije za aplikaciju i `clean test`.
