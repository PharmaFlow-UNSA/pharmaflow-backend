package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.Prescription;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @NotNull
    @EntityGraph(attributePaths = {"orders", "autoRefillSubscriptions"})
    Optional<Prescription> findById(@NotNull Long id);

    List<Prescription> findByUserId(Long userId);

    List<Prescription> findByStatus(String status);
}
