package com.pharmaflow.producthealth.saga.listener;

import com.pharmaflow.producthealth.saga.config.RabbitMQSagaConfig;
import com.pharmaflow.producthealth.saga.events.ProductStockCreatedEvent;
import com.pharmaflow.producthealth.saga.events.ProductStockCreationFailedEvent;
import com.pharmaflow.producthealth.saga.service.ProductCreationSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for saga events from other microservices.
 * Receives responses from Pharmacy Inventory Service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventListener {

    private final ProductCreationSagaService sagaService;

    /**
     * Handle successful stock entry creation.
     * Marks the product creation saga as COMPLETED.
     */
    @RabbitListener(queues = RabbitMQSagaConfig.PRODUCT_HEALTH_SAGA_QUEUE)
    public void handleProductStockCreatedEvent(ProductStockCreatedEvent event) {
        log.info("📥 Received ProductStockCreatedEvent: sagaId={}, productId={}, stockId={}",
                event.getSagaId(), event.getProductId(), event.getStockEntryId());

        try {
            sagaService.handleProductStockCreated(event);
            log.info("✅ Saga completed successfully: sagaId={}", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ Error handling ProductStockCreatedEvent: {}", e.getMessage(), e);
            // Do not rethrow — prevents infinite requeue loop
        }
    }

    /**
     * Handle stock entry creation failure.
     * Triggers compensating transaction: deletes the product.
     */
    @RabbitListener(queues = RabbitMQSagaConfig.PRODUCT_HEALTH_SAGA_QUEUE)
    public void handleProductStockCreationFailedEvent(ProductStockCreationFailedEvent event) {
        log.warn("⚠️  Received ProductStockCreationFailedEvent: sagaId={}, productId={}, reason={}",
                event.getSagaId(), event.getProductId(), event.getReason());

        try {
            sagaService.handleProductStockCreationFailed(event);
            log.info("🔄 Compensation triggered for sagaId={}", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ Error handling ProductStockCreationFailedEvent: {}", e.getMessage(), e);
            // Do not rethrow — prevents infinite requeue loop
        }
    }
}
