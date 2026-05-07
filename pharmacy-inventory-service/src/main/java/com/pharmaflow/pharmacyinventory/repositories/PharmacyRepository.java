package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long>,
                                            JpaSpecificationExecutor<Pharmacy> {

    @NotNull
    @EntityGraph(attributePaths = {"inventoryItems", "reservations", "deliveries"})
    Optional<Pharmacy> findById(@NotNull Long id);

    boolean existsByName(String name);
}
