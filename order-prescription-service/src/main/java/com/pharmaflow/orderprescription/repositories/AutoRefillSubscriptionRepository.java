package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.AutoRefillSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoRefillSubscriptionRepository extends JpaRepository<AutoRefillSubscription, Long> {
}
