package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Inventory;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>,
                                             JpaSpecificationExecutor<Inventory> {

    @NotNull
    @EntityGraph(attributePaths = {"pharmacy"})
    Optional<Inventory> findById(@NotNull Long id);

    List<Inventory> findByPharmacyId(Long pharmacyId);

    List<Inventory> findByProductId(Long productId);
}
