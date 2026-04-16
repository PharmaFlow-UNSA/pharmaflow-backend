package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoRefillSubscriptionRepository extends JpaRepository<AutoRefillSubscription, Long> {

    List<AutoRefillSubscription> findByUserId(Long userId);

    List<AutoRefillSubscription> findByStatus(String status);

    List<AutoRefillSubscription> findByProductId(Long productId);
}
