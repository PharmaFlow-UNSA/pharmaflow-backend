package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    @Query("SELECT a FROM Allergy a WHERE LOWER(a.severity) = LOWER(:severity)")
    List<Allergy> findBySeverity(@Param("severity") String severity);

    @Query("SELECT a FROM Allergy a WHERE LOWER(a.allergen) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Allergy> findByAllergenContainingIgnoreCase(@Param("name") String name);
}

