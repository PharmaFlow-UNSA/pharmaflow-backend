package com.pharmaflow.userhealth.saga.listener;

import com.pharmaflow.userhealth.saga.config.RabbitMQSagaConfig;
import com.pharmaflow.userhealth.saga.events.InventoryCreatedEvent;
import com.pharmaflow.userhealth.saga.events.InventoryCreationFailedEvent;
import com.pharmaflow.userhealth.saga.service.UserRegistrationSagaOrchestrator;
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

    private final UserRegistrationSagaOrchestrator sagaOrchestrator;

    /**
     * Handle successful inventory account creation.
     * Marks the saga as completed.
     */
    @RabbitListener(queues = RabbitMQSagaConfig.USER_HEALTH_SAGA_QUEUE)
    public void handleInventoryCreatedEvent(InventoryCreatedEvent event) {
        log.info("📥 Received InventoryCreatedEvent: sagaId={}, userId={}, inventoryId={}",
                event.getSagaId(), event.getUserId(), event.getInventoryAccountId());

        try {
            sagaOrchestrator.handleInventoryCreated(event);
            log.info("✅ Saga completed successfully: sagaId={}", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ Error handling InventoryCreatedEvent: {}", e.getMessage(), e);
            // Don't throw exception - event will be requeued
        }
    }

    /**
     * Handle inventory account creation failure.
     * Triggers compensating transaction (user deletion).
     */
    @RabbitListener(queues = RabbitMQSagaConfig.USER_HEALTH_SAGA_QUEUE)
    public void handleInventoryCreationFailedEvent(InventoryCreationFailedEvent event) {
        log.warn("⚠️  Received InventoryCreationFailedEvent: sagaId={}, userId={}, reason={}",
                event.getSagaId(), event.getUserId(), event.getReason());

        try {
            sagaOrchestrator.handleInventoryCreationFailed(event);
            log.info("🔄 Compensation triggered for sagaId={}", event.getSagaId());
        } catch (Exception e) {
            log.error("❌ Error handling InventoryCreationFailedEvent: {}", e.getMessage(), e);
            // Don't throw exception - event will be requeued
        }
    }
}

