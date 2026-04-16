package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.Order;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @NotNull
    @EntityGraph(attributePaths = {"orderItems", "payment"})
    Optional<Order> findById(@NotNull Long id);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(String status);
}
