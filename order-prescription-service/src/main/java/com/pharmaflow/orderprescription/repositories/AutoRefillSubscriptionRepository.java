package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoRefillSubscriptionRepository extends JpaRepository<AutoRefillSubscription, Long>,
                                                          JpaSpecificationExecutor<AutoRefillSubscription> {

    @NotNull
    @EntityGraph(attributePaths = {"prescription"})
    Optional<AutoRefillSubscription> findById(@NotNull Long id);

    List<AutoRefillSubscription> findByUserId(Long userId);

    List<AutoRefillSubscription> findByStatus(String status);

    List<AutoRefillSubscription> findByProductId(Long productId);
}
