package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.OrderItem;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long>,
                                             JpaSpecificationExecutor<OrderItem> {

    @NotNull
    @EntityGraph(attributePaths = {"order"})
    Optional<OrderItem> findById(@NotNull Long id);

    List<OrderItem> findByOrderId(Long orderId);
}
