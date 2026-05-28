package com.pharmaflow.orderprescription.messaging.saga;

import com.pharmaflow.orderprescription.config.RabbitMqSagaConfig.OrderSagaRabbitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order-saga events to the shared {@code pharmaflow.saga.exchange}.
 * Routing is configured by {@link OrderSagaRabbitProperties}.
 */
@Component
public class OrderSagaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final OrderSagaRabbitProperties properties;

    public OrderSagaEventPublisher(RabbitTemplate rabbitTemplate, OrderSagaRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        publish(properties.getOrderPlacedRoutingKey(), event);
        log.info("Published order.placed for correlationId={} orderId={}",
                event.correlationId(), event.orderId());
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publish(properties.getOrderCancelledRoutingKey(), event);
        log.info("Published order.cancelled for correlationId={} orderId={}",
                event.correlationId(), event.orderId());
    }

    private void publish(String routingKey, Object event) {
        if (!properties.isEnabled()) {
            log.debug("Saga publishing disabled, skipping event with routingKey={}", routingKey);
            return;
        }
        rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event);
    }
}
