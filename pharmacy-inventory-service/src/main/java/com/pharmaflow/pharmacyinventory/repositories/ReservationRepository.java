package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Reservation;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>,
                                               JpaSpecificationExecutor<Reservation> {

    @NotNull
    @EntityGraph(attributePaths = {"pharmacy"})
    Optional<Reservation> findById(@NotNull Long id);

    List<Reservation> findByPharmacyId(Long pharmacyId);

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByStatus(String status);
}
