package com.pharmaflow.pharmacyinventory.messaging.saga;

import com.pharmaflow.pharmacyinventory.config.RabbitMqSagaConfig.SagaRabbitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventorySagaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SagaRabbitProperties properties;
    private InventorySagaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new SagaRabbitProperties();
        publisher = new InventorySagaEventPublisher(rabbitTemplate, properties);
    }

    @Test
    void publishOrderStockReservedShouldUseConfiguredRoutingKey() {
        OrderStockReservedEvent event = new OrderStockReservedEvent("corr-1", 42L, LocalDateTime.now());
        publisher.publishOrderStockReserved(event);
        verify(rabbitTemplate).convertAndSend(
                eq(properties.getExchange()),
                eq(properties.getOrderStockReservedRoutingKey()),
                eq((Object) event));
    }

    @Test
    void publishOrderStockRejectedShouldUseConfiguredRoutingKey() {
        OrderStockRejectedEvent event = new OrderStockRejectedEvent(
                "corr-1", 42L, "no stock", LocalDateTime.now());
        publisher.publishOrderStockRejected(event);
        verify(rabbitTemplate).convertAndSend(
                eq(properties.getExchange()),
                eq(properties.getOrderStockRejectedRoutingKey()),
                eq((Object) event));
    }

    @Test
    void publishOrderStockRestockedShouldUseConfiguredRoutingKey() {
        OrderStockRestockedEvent event = new OrderStockRestockedEvent("corr-1", 42L, LocalDateTime.now());
        publisher.publishOrderStockRestocked(event);
        verify(rabbitTemplate).convertAndSend(
                eq(properties.getExchange()),
                eq(properties.getOrderStockRestockedRoutingKey()),
                eq((Object) event));
    }

    @Test
    void publishShouldNoOpWhenDisabled() {
        properties.setEnabled(false);
        publisher.publishOrderStockReserved(new OrderStockReservedEvent("corr-1", 42L, LocalDateTime.now()));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
