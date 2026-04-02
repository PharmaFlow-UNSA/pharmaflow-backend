package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}
