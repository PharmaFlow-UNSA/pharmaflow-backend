package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.OrderStockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderStockReservationRepository extends JpaRepository<OrderStockReservation, Long> {

    Optional<OrderStockReservation> findByCorrelationId(String correlationId);
}
