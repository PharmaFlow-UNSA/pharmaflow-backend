package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.PatientProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    
    @EntityGraph(attributePaths = {"allergies", "therapies"})
    Optional<PatientProfile> findById(Long id);

    @Query("SELECT p FROM PatientProfile p WHERE p.bloodType = :bloodType")
    List<PatientProfile> findByBloodType(@Param("bloodType") String bloodType);

    @Query("SELECT p FROM PatientProfile p WHERE (p.weight / (p.height * p.height)) BETWEEN :minBMI AND :maxBMI")
    List<PatientProfile> findByBMIRange(@Param("minBMI") Double minBMI, @Param("maxBMI") Double maxBMI);
}

