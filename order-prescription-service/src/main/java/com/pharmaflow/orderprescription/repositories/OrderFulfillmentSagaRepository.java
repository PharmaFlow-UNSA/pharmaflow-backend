package com.pharmaflow.orderprescription.repositories;

import com.pharmaflow.orderprescription.models.OrderFulfillmentSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderFulfillmentSagaRepository extends JpaRepository<OrderFulfillmentSaga, Long> {

    Optional<OrderFulfillmentSaga> findByCorrelationId(String correlationId);

    Optional<OrderFulfillmentSaga> findByOrderId(Long orderId);
}
