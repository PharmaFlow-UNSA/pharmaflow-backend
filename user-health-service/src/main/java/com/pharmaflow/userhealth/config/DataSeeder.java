package com.pharmaflow.userhealth.config;

import com.pharmaflow.userhealth.models.*;
import com.pharmaflow.userhealth.models.enums.BloodType;
import com.pharmaflow.userhealth.models.enums.Relationship;
import com.pharmaflow.userhealth.models.enums.Severity;
import com.pharmaflow.userhealth.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        resetDatabase();
        seedData();
    }

    private void resetDatabase() {
        // Add role column if it doesn't exist
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(255) NOT NULL DEFAULT 'ROLE_USER'"
            ).executeUpdate();
            log.info(">>> Role column added or already exists");
        } catch (Exception e) {
            log.warn(">>> Could not add role column: " + e.getMessage());
        }

        // Always fully truncate all relevant tables to guarantee a clean state
        userRepository.deleteAll();
        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE family_members RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE patient_profiles RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE allergies RESTART IDENTITY CASCADE").executeUpdate();
        log.info(">>> Database fully truncated and IDs reset to 1");
    }

    private void seedData() {
        List<User> users = new ArrayList<>();

        // Test korisnici za dokumentaciju i testiranje

        // 1. Doctor test account (ROLE_DOCTOR)
        User doctor = createTestUser(
            "doctor@example.com",
            "password123",
            "Dr. Marko",
            "Marković",
            "ROLE_DOCTOR",
            BloodType.A_POSITIVE,
            175.0,
            80.0
        );
        users.add(doctor);

        // 2. Regular user test account (ROLE_USER)
        User regularUser = createTestUser(
            "user@example.com",
            "password123",
            "Petar",
            "Petrović",
            "ROLE_USER",
            BloodType.O_POSITIVE,
            170.0,
            75.0
        );
        users.add(regularUser);

        // 3. Pharmacist test account (ROLE_PHARMACIST)
        User pharmacist = createTestUser(
            "pharmacist@example.com",
            "password123",
            "Ana",
            "Anić",
            "ROLE_PHARMACIST",
            BloodType.B_POSITIVE,
            165.0,
            65.0
        );
        users.add(pharmacist);

        // 4. Admin test account (ROLE_ADMIN)
        User admin = createTestUser(
            "admin@example.com",
            "password123",
            "Admin",
            "Administrator",
            "ROLE_ADMIN",
            BloodType.AB_POSITIVE,
            180.0,
            85.0
        );
        users.add(admin);

        // Dodatni test korisnici za demonstraciju
        for (int i = 1; i <= 6; i++) {
            PatientProfile userProfile = new PatientProfile();
            userProfile.setBloodType(i % 2 == 0 ? BloodType.A_POSITIVE : BloodType.O_POSITIVE);
            userProfile.setHeight(160.0 + (i * 2));
            userProfile.setWeight(60.0 + i);

            Allergy allergy = new Allergy();
            allergy.setAllergen("Alergen_" + i);
            allergy.setSeverity(i % 3 == 0 ? Severity.HIGH : Severity.LOW);
            allergy.setActiveSubstance(i == 1 ? "Penicillin" : "Substance_" + i);
            userProfile.setAllergies(new ArrayList<>(List.of(allergy)));

            Therapy therapy = new Therapy();
            therapy.setMedicationName("Lijek_" + i);
            therapy.setDosage((i * 10) + "mg");
            therapy.setFrequency("2x dnevno");
            userProfile.setTherapies(new ArrayList<>(List.of(therapy)));

            User user = new User();
            user.setFirstName("Ime_" + i);
            user.setLastName("Prezime_" + i);
            user.setEmail("user" + i + "@pharmaflow.ba");
            user.setPassword(passwordEncoder.encode("password" + i));
            user.setRole("ROLE_USER");
            user.setPatientProfile(userProfile);

            FamilyMember member = new FamilyMember();
            member.setFirstName("Clan_" + i);
            member.setRelationship(i % 2 == 0 ? Relationship.CHILD : Relationship.SPOUSE);
            member.setUser(user);

            PatientProfile memberProfile = new PatientProfile();
            memberProfile.setBloodType(BloodType.B_NEGATIVE);
            memberProfile.setHeight(120.0 + i);
            memberProfile.setWeight(30.0 + i);

            Therapy vitamins = new Therapy();
            vitamins.setMedicationName("Vitamin C");
            vitamins.setDosage("500mg");
            vitamins.setFrequency("1x dnevno");
            memberProfile.setTherapies(new ArrayList<>(List.of(vitamins)));

            member.setPatientProfile(memberProfile);

            user.getFamilyMembers().add(member);

            users.add(user);
        }

        userRepository.saveAll(users);
        log.info(">>> Database successfully populated with {} users", users.size());
        log.info(">>> Test accounts created:");
        log.info(">>>   - doctor@example.com / password123 (ROLE_DOCTOR)");
        log.info(">>>   - user@example.com / password123 (ROLE_USER)");
        log.info(">>>   - pharmacist@example.com / password123 (ROLE_PHARMACIST)");
        log.info(">>>   - admin@example.com / password123 (ROLE_ADMIN)");
    }

    private User createTestUser(String email, String password, String firstName, String lastName,
                                String role, BloodType bloodType, Double height, Double weight) {
        PatientProfile profile = new PatientProfile();
        profile.setBloodType(bloodType);
        profile.setHeight(height);
        profile.setWeight(weight);

        Allergy allergy = new Allergy();
        allergy.setAllergen("No known allergies");
        allergy.setSeverity(Severity.LOW);
        allergy.setActiveSubstance("None");
        profile.setAllergies(new ArrayList<>(List.of(allergy)));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setPatientProfile(profile);

        return user;
    }
}