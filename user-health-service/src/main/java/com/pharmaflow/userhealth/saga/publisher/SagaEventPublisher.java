package com.pharmaflow.userhealth.saga.publisher;

import com.pharmaflow.userhealth.saga.config.RabbitMQSagaConfig;
import com.pharmaflow.userhealth.saga.events.SagaEvent;
import com.pharmaflow.userhealth.saga.events.UserCreatedEvent;
import com.pharmaflow.userhealth.saga.events.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for saga events from User Health Service.
 * Publishes events to RabbitMQ exchange.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish UserCreatedEvent to trigger inventory account creation.
     */
    public void publishUserCreatedEvent(UserCreatedEvent event) {
        log.info("📤 Publishing UserCreatedEvent: sagaId={}, userId={}", event.getSagaId(), event.getUserId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQSagaConfig.SAGA_EXCHANGE,
                    RabbitMQSagaConfig.USER_CREATED_ROUTING_KEY,
                    event
            );
            log.info("✅ UserCreatedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish UserCreatedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish saga event", e);
        }
    }

    /**
     * Publish UserDeletedEvent as compensating action.
     */
    public void publishUserDeletedEvent(UserDeletedEvent event) {
        log.info("📤 Publishing UserDeletedEvent (compensating): sagaId={}, userId={}",
                event.getSagaId(), event.getUserId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQSagaConfig.SAGA_EXCHANGE,
                    RabbitMQSagaConfig.USER_DELETED_ROUTING_KEY,
                    event
            );
            log.info("✅ UserDeletedEvent published successfully");
        } catch (Exception e) {
            log.error("❌ Failed to publish UserDeletedEvent: {}", e.getMessage(), e);
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

