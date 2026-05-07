package com.pharmaflow.pharmacyinventory.repositories;

import com.pharmaflow.pharmacyinventory.models.Delivery;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long>,
                                            JpaSpecificationExecutor<Delivery> {

    @NotNull
    @EntityGraph(attributePaths = {"pharmacy"})
    Optional<Delivery> findById(@NotNull Long id);

    List<Delivery> findByPharmacyId(Long pharmacyId);

    List<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByStatus(String status);
}
