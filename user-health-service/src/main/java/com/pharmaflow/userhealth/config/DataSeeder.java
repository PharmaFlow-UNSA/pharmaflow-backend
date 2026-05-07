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
        // Always fully truncate all relevant tables to guarantee a clean state
        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE family_members RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE patient_profiles RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE allergies RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE therapies RESTART IDENTITY CASCADE").executeUpdate();
        log.info(">>> Database fully truncated and IDs reset to 1");
    }

    private void seedData() {
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
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
        log.info(">>> Database successfully populated with {} users (IDs 1-10, encrypted passwords)", users.size());
    }
}