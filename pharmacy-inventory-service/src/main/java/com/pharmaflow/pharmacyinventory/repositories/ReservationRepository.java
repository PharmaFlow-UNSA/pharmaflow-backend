package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
