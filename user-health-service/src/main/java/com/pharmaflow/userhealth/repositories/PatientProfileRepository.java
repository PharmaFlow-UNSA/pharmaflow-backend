package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.PatientProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    
    @EntityGraph(attributePaths = {"allergies", "therapies"})
    Optional<PatientProfile> findById(Long id);
}

