# User & Health Service (Microservice 1)

Ovaj servis upravlja korisničkim profilima, Family Hub-om i digitalnim zdravstvenim kartonima.

## Tehnologije
- Java 17 / Spring Boot 3
- PostgreSQL
- Spring Data JPA
- BCrypt Password Encoding

## Funkcionalnosti
- [x] Registracija i upravljanje korisnicima
- [x] Family Hub (upravljanje članovima porodice)
- [x] Digitalni karton (Alergije i Terapije)
- [x] Sigurno čuvanje lozinki (BCrypt)

## Kako pokrenuti
1. Podesite Environment Variable `DB_URL`,`DB_USERNAME`,`DB_PASSWORD`.
2. Pokrenite aplikaciju putem IntelliJ-a ili komandom `./mvnw spring-boot:run`.
3. Baza će se automatski popuniti sa 10 testnih korisnika (ID 1-10).