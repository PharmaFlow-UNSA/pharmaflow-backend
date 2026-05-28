package com.pharmaflow.producthealth.saga.publisher;

import com.pharmaflow.producthealth.saga.config.RabbitMQSagaConfig;
import com.pharmaflow.producthealth.saga.events.ProductCreatedEvent;
import com.pharmaflow.producthealth.saga.events.ProductDeletedEvent;
import com.pharmaflow.producthealth.saga.events.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for saga events from Product Health Service.
 * Publishes events to the shared RabbitMQ topic exchange.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish ProductCreatedEvent to trigger stock entry creation in pharmacy service.
     */
    public void publishProductCreatedEvent(ProductCreatedEvent event) {
        log.info("📤 Publishing ProductCreatedEvent: sagaId={}, productId={}",
                event.getSagaId(), event.getProductId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQSagaConfig.SAGA_EXCHANGE,
                    RabbitMQSagaConfig.PRODUCT_CREATED_ROUTING_KEY,
                    event
            );
            log.info("✅ ProductCreatedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish ProductCreatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish saga event", e);
        }
    }

    /**
     * Publish ProductDeletedEvent as compensating action.
     * Signals pharmacy-inventory to clean up partial stock entries.
     */
    public void publishProductDeletedEvent(ProductDeletedEvent event) {
        log.info("📤 Publishing ProductDeletedEvent (compensating): sagaId={}, productId={}",
                event.getSagaId(), event.getProductId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQSagaConfig.SAGA_EXCHANGE,
                    RabbitMQSagaConfig.PRODUCT_DELETED_ROUTING_KEY,
                    event
            );
            log.info("✅ ProductDeletedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish ProductDeletedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish compensating event", e);
        }
    }

    /**
     * Generic event publisher.
     */
    public void publishEvent(SagaEvent event, String routingKey) {
        log.info("📤 Publishing event: type={}, sagaId={}", event.getEventType(), event.getSagaId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQSagaConfig.SAGA_EXCHANGE,
                    routingKey,
                    event
            );
            log.info("✅ Event published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
