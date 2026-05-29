package com.pharmaflow.userhealth.saga.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Saga Choreography pattern.
 *
 * Architecture:
 * - Exchange: pharmaflow.saga.exchange (Direct Exchange)
 * - Queues:
 *   1. user-health.saga.queue - Listens for inventory responses
 *   2. pharmacy-inventory.saga.queue - Receives user creation events
 *
 * Routing Keys:
 * - user.created -> Routes to pharmacy-inventory
 * - inventory.created -> Routes to user-health
 * - inventory.failed -> Routes to user-health
 * - user.deleted -> Routes to pharmacy-inventory (for cleanup)
 */
@Configuration
public class RabbitMQSagaConfig {

    // Exchange name
    public static final String SAGA_EXCHANGE = "pharmaflow.saga.exchange";

    // Queue names
    public static final String USER_HEALTH_SAGA_QUEUE = "user-health.saga.queue";
    public static final String PHARMACY_INVENTORY_SAGA_QUEUE = "pharmacy-inventory.saga.queue";

    // Routing keys
    public static final String USER_CREATED_ROUTING_KEY = "user.created";
    public static final String INVENTORY_CREATED_ROUTING_KEY = "inventory.created";
    public static final String INVENTORY_FAILED_ROUTING_KEY = "inventory.failed";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";

    /**
     * Direct Exchange for saga events.
     * The shared saga exchange is declared as direct by the other services.
     */
    @Bean
    public DirectExchange sagaExchange() {
        return new DirectExchange(SAGA_EXCHANGE, true, false);
    }

    /**
     * Queue for User Health Service to receive responses from Pharmacy Service.
     */
    @Bean
    public Queue userHealthSagaQueue() {
        return QueueBuilder.durable(USER_HEALTH_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Queue for Pharmacy Inventory Service to receive user creation events.
     * This would typically be defined in pharmacy-inventory-service, but we define it here
     * to ensure it exists when user-health-service starts.
     */
    @Bean
    public Queue pharmacyInventorySagaQueue() {
        return QueueBuilder.durable(PHARMACY_INVENTORY_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Bind user-health queue to receive inventory success/failure events.
     */
    @Bean
    public Binding inventoryCreatedBinding(Queue userHealthSagaQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(userHealthSagaQueue)
                .to(sagaExchange)
                .with(INVENTORY_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryFailedBinding(Queue userHealthSagaQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(userHealthSagaQueue)
                .to(sagaExchange)
                .with(INVENTORY_FAILED_ROUTING_KEY);
    }

    /**
     * Bind pharmacy-inventory queue to receive user creation events.
     */
    @Bean
    public Binding userCreatedBinding(Queue pharmacyInventorySagaQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(pharmacyInventorySagaQueue)
                .to(sagaExchange)
                .with(USER_CREATED_ROUTING_KEY);
    }

    /**
     * Bind pharmacy-inventory queue to receive user deletion events (for cleanup).
     */
    @Bean
    public Binding userDeletedBinding(Queue pharmacyInventorySagaQueue, DirectExchange sagaExchange) {
        return BindingBuilder.bind(pharmacyInventorySagaQueue)
                .to(sagaExchange)
                .with(USER_DELETED_ROUTING_KEY);
    }

    /**
     * Message converter for JSON serialization/deserialization.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
