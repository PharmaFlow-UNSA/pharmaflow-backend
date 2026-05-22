package com.pharmaflow.userhealth.saga.publisher;

import com.pharmaflow.userhealth.saga.config.RabbitMQSagaConfig;
import com.pharmaflow.userhealth.saga.events.UserCreatedEvent;
import com.pharmaflow.userhealth.saga.events.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SagaEventPublisher.
 * Tests event publishing without requiring RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaEventPublisher - Unit Tests")
class SagaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SagaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SagaEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publishUserCreatedEvent should send event to correct exchange and routing key")
    void publishUserCreatedEvent_shouldSendEventToCorrectExchangeAndRoutingKey() {
        // Given
        String sagaId = "test-saga-123";
        UserCreatedEvent event = new UserCreatedEvent(
                sagaId, 1L, "John", "Doe", "john@test.com", "ROLE_USER"
        );

        // When
        publisher.publishUserCreatedEvent(event);

        // Then
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UserCreatedEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);

        verify(rabbitTemplate, times(1)).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(RabbitMQSagaConfig.SAGA_EXCHANGE);
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMQSagaConfig.USER_CREATED_ROUTING_KEY);
        assertThat(eventCaptor.getValue().getSagaId()).isEqualTo(sagaId);
    }

    @Test
    @DisplayName("publishUserCreatedEvent should throw exception when RabbitMQ fails")
    void publishUserCreatedEvent_shouldThrowExceptionWhenRabbitMQFails() {
        // Given
        UserCreatedEvent event = new UserCreatedEvent(
                "saga-123", 1L, "John", "Doe", "john@test.com", "ROLE_USER"
        );

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When/Then
        assertThatThrownBy(() -> publisher.publishUserCreatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish saga event");
    }

    @Test
    @DisplayName("publishUserDeletedEvent should send event to correct exchange and routing key")
    void publishUserDeletedEvent_shouldSendEventToCorrectExchangeAndRoutingKey() {
        // Given
        String sagaId = "test-saga-123";
        UserDeletedEvent event = new UserDeletedEvent(sagaId, 1L, "COMPENSATION");

        // When
        publisher.publishUserDeletedEvent(event);

        // Then
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);

        verify(rabbitTemplate, times(1)).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo(RabbitMQSagaConfig.SAGA_EXCHANGE);
        assertThat(routingKeyCaptor.getValue()).isEqualTo(RabbitMQSagaConfig.USER_DELETED_ROUTING_KEY);
        assertThat(eventCaptor.getValue().getReason()).isEqualTo("COMPENSATION");
    }

    @Test
    @DisplayName("publishUserDeletedEvent should throw exception when RabbitMQ fails")
    void publishUserDeletedEvent_shouldThrowExceptionWhenRabbitMQFails() {
        // Given
        UserDeletedEvent event = new UserDeletedEvent("saga-123", 1L, "COMPENSATION");

        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When/Then
        assertThatThrownBy(() -> publisher.publishUserDeletedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish compensating event");
    }

    @Test
    @DisplayName("publishEvent should send generic event with custom routing key")
    void publishEvent_shouldSendGenericEventWithCustomRoutingKey() {
        // Given
        String customRoutingKey = "custom.routing.key";
        UserCreatedEvent event = new UserCreatedEvent(
                "saga-123", 1L, "Jane", "Smith", "jane@test.com", "ROLE_ADMIN"
        );

        // When
        publisher.publishEvent(event, customRoutingKey);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQSagaConfig.SAGA_EXCHANGE),
                eq(customRoutingKey),
                eq(event)
        );
    }

    @Test
    @DisplayName("publishEvent should throw exception when RabbitMQ fails")
    void publishEvent_shouldThrowExceptionWhenRabbitMQFails() {
        // Given
        UserCreatedEvent event = new UserCreatedEvent(
                "saga-123", 1L, "Jane", "Smith", "jane@test.com", "ROLE_ADMIN"
        );

        doThrow(new RuntimeException("Connection error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When/Then
        assertThatThrownBy(() -> publisher.publishEvent(event, "some.key"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event");
    }

    @Test
    @DisplayName("Publisher should handle multiple events in sequence")
    void publisher_shouldHandleMultipleEventsInSequence() {
        // Given
        UserCreatedEvent event1 = new UserCreatedEvent("saga-1", 1L, "A", "B", "a@test.com", "ROLE_USER");
        UserCreatedEvent event2 = new UserCreatedEvent("saga-2", 2L, "C", "D", "c@test.com", "ROLE_USER");
        UserDeletedEvent event3 = new UserDeletedEvent("saga-3", 3L, "COMPENSATION");

        // When
        publisher.publishUserCreatedEvent(event1);
        publisher.publishUserCreatedEvent(event2);
        publisher.publishUserDeletedEvent(event3);

        // Then
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}




