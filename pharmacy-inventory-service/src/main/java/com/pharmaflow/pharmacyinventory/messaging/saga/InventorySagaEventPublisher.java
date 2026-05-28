package com.pharmaflow.pharmacyinventory.messaging.saga;

import com.pharmaflow.pharmacyinventory.config.RabbitMqSagaConfig.SagaRabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventorySagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SagaRabbitProperties properties;

    public InventorySagaEventPublisher(RabbitTemplate rabbitTemplate, SagaRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publishReservationCreated(ReservationCreatedEvent event) {
        publish(properties.getReservationCreatedRoutingKey(), event);
    }

    public void publishReservationRejected(ReservationRejectedEvent event) {
        publish(properties.getReservationRejectedRoutingKey(), event);
    }

    public void publishReservationCompensated(ReservationCompensatedEvent event) {
        publish(properties.getReservationCompensatedRoutingKey(), event);
    }

    // --- Order Fulfillment saga (Zadatak 8.3) ---

    public void publishOrderStockReserved(OrderStockReservedEvent event) {
        publish(properties.getOrderStockReservedRoutingKey(), event);
    }

    public void publishOrderStockRejected(OrderStockRejectedEvent event) {
        publish(properties.getOrderStockRejectedRoutingKey(), event);
    }

    public void publishOrderStockRestocked(OrderStockRestockedEvent event) {
        publish(properties.getOrderStockRestockedRoutingKey(), event);
    }

    private void publish(String routingKey, Object event) {
        if (!properties.isEnabled()) {
            return;
        }
        rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event);
    }
}
