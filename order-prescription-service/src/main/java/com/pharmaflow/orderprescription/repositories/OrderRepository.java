package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
