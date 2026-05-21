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

    private void publish(String routingKey, Object event) {
        if (!properties.isEnabled()) {
            return;
        }
        rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event);
    }
}
