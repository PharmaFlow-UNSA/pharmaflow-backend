package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
}
