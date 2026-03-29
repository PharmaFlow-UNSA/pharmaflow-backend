package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
