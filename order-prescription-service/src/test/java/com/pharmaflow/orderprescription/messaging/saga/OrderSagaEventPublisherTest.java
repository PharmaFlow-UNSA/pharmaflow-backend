package com.pharmaflow.orderprescription.messaging.saga;

import com.pharmaflow.orderprescription.config.RabbitMqSagaConfig.OrderSagaRabbitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderSagaRabbitProperties properties;
    private OrderSagaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new OrderSagaRabbitProperties();
        publisher = new OrderSagaEventPublisher(rabbitTemplate, properties);
    }

    @Test
    void publishOrderPlacedShouldUseConfiguredExchangeAndRoutingKey() {
        OrderPlacedEvent event = new OrderPlacedEvent(
                "corr-1", 10L, 1L, 7L, List.of(), LocalDateTime.now());

        publisher.publishOrderPlaced(event);

        verify(rabbitTemplate).convertAndSend(
                eq(properties.getExchange()),
                eq(properties.getOrderPlacedRoutingKey()),
                eq((Object) event));
    }

    @Test
    void publishOrderCancelledShouldUseConfiguredExchangeAndRoutingKey() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                "corr-1", 10L, "USER_CANCELLED", LocalDateTime.now());

        publisher.publishOrderCancelled(event);

        verify(rabbitTemplate).convertAndSend(
                eq(properties.getExchange()),
                eq(properties.getOrderCancelledRoutingKey()),
                eq((Object) event));
    }

    @Test
    void publishShouldNoOpWhenDisabled() {
        properties.setEnabled(false);

        publisher.publishOrderPlaced(new OrderPlacedEvent(
                "corr-1", 10L, 1L, 7L, List.of(), LocalDateTime.now()));

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
