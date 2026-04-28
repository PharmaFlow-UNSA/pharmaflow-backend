package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.Therapy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TherapyRepository extends JpaRepository<Therapy, Long> {

    @Query("SELECT t FROM Therapy t WHERE LOWER(t.medicationName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Therapy> findByMedicationNameContainingIgnoreCase(@Param("name") String name);
}

