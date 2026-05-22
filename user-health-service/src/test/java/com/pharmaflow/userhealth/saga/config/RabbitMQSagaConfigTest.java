package com.pharmaflow.userhealth.saga.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RabbitMQ Saga Configuration.
 * Validates exchange, queue, and binding definitions.
 */
@DisplayName("RabbitMQ Saga Configuration - Unit Tests")
class RabbitMQSagaConfigTest {

    private final RabbitMQSagaConfig config = new RabbitMQSagaConfig();

    @Test
    @DisplayName("Saga exchange should be topic exchange with correct name")
    void sagaExchange_shouldBeTopicExchangeWithCorrectName() {
        // When
        TopicExchange exchange = config.sagaExchange();

        // Then
        assertThat(exchange.getName()).isEqualTo("pharmaflow.saga.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("User health saga queue should be durable")
    void userHealthSagaQueue_shouldBeDurable() {
        // When
        Queue queue = config.userHealthSagaQueue();

        // Then
        assertThat(queue.getName()).isEqualTo("user-health.saga.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Pharmacy inventory saga queue should be durable")
    void pharmacyInventorySagaQueue_shouldBeDurable() {
        // When
        Queue queue = config.pharmacyInventorySagaQueue();

        // Then
        assertThat(queue.getName()).isEqualTo("pharmacy-inventory.saga.queue");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Inventory created binding should use correct routing key")
    void inventoryCreatedBinding_shouldUseCorrectRoutingKey() {
        // Given
        Queue queue = config.userHealthSagaQueue();
        TopicExchange exchange = config.sagaExchange();

        // When
        Binding binding = config.inventoryCreatedBinding(queue, exchange);

        // Then
        assertThat(binding.getDestination()).isEqualTo("user-health.saga.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("inventory.created");
    }

    @Test
    @DisplayName("Inventory failed binding should use correct routing key")
    void inventoryFailedBinding_shouldUseCorrectRoutingKey() {
        // Given
        Queue queue = config.userHealthSagaQueue();
        TopicExchange exchange = config.sagaExchange();

        // When
        Binding binding = config.inventoryFailedBinding(queue, exchange);

        // Then
        assertThat(binding.getDestination()).isEqualTo("user-health.saga.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("inventory.failed");
    }

    @Test
    @DisplayName("User created binding should use correct routing key")
    void userCreatedBinding_shouldUseCorrectRoutingKey() {
        // Given
        Queue queue = config.pharmacyInventorySagaQueue();
        TopicExchange exchange = config.sagaExchange();

        // When
        Binding binding = config.userCreatedBinding(queue, exchange);

        // Then
        assertThat(binding.getDestination()).isEqualTo("pharmacy-inventory.saga.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("user.created");
    }

    @Test
    @DisplayName("User deleted binding should use correct routing key")
    void userDeletedBinding_shouldUseCorrectRoutingKey() {
        // Given
        Queue queue = config.pharmacyInventorySagaQueue();
        TopicExchange exchange = config.sagaExchange();

        // When
        Binding binding = config.userDeletedBinding(queue, exchange);

        // Then
        assertThat(binding.getDestination()).isEqualTo("pharmacy-inventory.saga.queue");
        assertThat(binding.getRoutingKey()).isEqualTo("user.deleted");
    }

    @Test
    @DisplayName("Configuration constants should match implementation")
    void configurationConstants_shouldMatchImplementation() {
        // Then
        assertThat(RabbitMQSagaConfig.SAGA_EXCHANGE).isEqualTo("pharmaflow.saga.exchange");
        assertThat(RabbitMQSagaConfig.USER_HEALTH_SAGA_QUEUE).isEqualTo("user-health.saga.queue");
        assertThat(RabbitMQSagaConfig.PHARMACY_INVENTORY_SAGA_QUEUE).isEqualTo("pharmacy-inventory.saga.queue");
        assertThat(RabbitMQSagaConfig.USER_CREATED_ROUTING_KEY).isEqualTo("user.created");
        assertThat(RabbitMQSagaConfig.INVENTORY_CREATED_ROUTING_KEY).isEqualTo("inventory.created");
        assertThat(RabbitMQSagaConfig.INVENTORY_FAILED_ROUTING_KEY).isEqualTo("inventory.failed");
        assertThat(RabbitMQSagaConfig.USER_DELETED_ROUTING_KEY).isEqualTo("user.deleted");
    }
}

