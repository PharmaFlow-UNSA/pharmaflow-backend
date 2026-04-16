package com.pharmaflow.userhealth.repositories;

import com.pharmaflow.userhealth.models.Therapy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TherapyRepository extends JpaRepository<Therapy, Long> {
}

