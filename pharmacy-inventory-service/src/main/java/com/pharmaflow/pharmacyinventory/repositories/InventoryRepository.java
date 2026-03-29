package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
