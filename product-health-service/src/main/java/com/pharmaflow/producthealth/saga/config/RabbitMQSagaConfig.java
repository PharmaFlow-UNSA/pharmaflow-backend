package com.pharmaflow.producthealth.saga.config;

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
 * - Exchange: pharmaflow.saga.exchange (Topic Exchange) - shared with user-health-service
 * - Queues:
 *   1. product-health.saga.queue - Listens for inventory responses
 *   2. pharmacy-inventory.product.saga.queue - Receives product creation events
 *
 * Routing Keys:
 * - product.created       -> Routes to pharmacy-inventory (create stock entry)
 * - product.stock.created -> Routes to product-health (success confirmation)
 * - product.stock.failed  -> Routes to product-health (failure, trigger compensation)
 * - product.deleted       -> Routes to pharmacy-inventory (cleanup stock entry)
 */
@Configuration
public class RabbitMQSagaConfig {

    // Exchange name - shared across all microservices
    public static final String SAGA_EXCHANGE = "pharmaflow.saga.exchange";

    // Queue names
    public static final String PRODUCT_HEALTH_SAGA_QUEUE = "product-health.saga.queue";
    public static final String PHARMACY_INVENTORY_PRODUCT_SAGA_QUEUE = "pharmacy-inventory.product.saga.queue";

    // Routing keys
    public static final String PRODUCT_CREATED_ROUTING_KEY = "product.created";
    public static final String PRODUCT_STOCK_CREATED_ROUTING_KEY = "product.stock.created";
    public static final String PRODUCT_STOCK_FAILED_ROUTING_KEY = "product.stock.failed";
    public static final String PRODUCT_DELETED_ROUTING_KEY = "product.deleted";

    /**
     * Topic Exchange for saga events.
     * Shared with user-health-service — Spring will reuse existing exchange.
     */
    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    /**
     * Queue for Product Health Service to receive responses from Pharmacy Service.
     */
    @Bean
    public Queue productHealthSagaQueue() {
        return QueueBuilder.durable(PRODUCT_HEALTH_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Queue for Pharmacy Inventory Service to receive product creation events.
     */
    @Bean
    public Queue pharmacyInventoryProductSagaQueue() {
        return QueueBuilder.durable(PHARMACY_INVENTORY_PRODUCT_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_EXCHANGE + ".dlx")
                .build();
    }

    /**
     * Bind product-health queue to receive stock success events.
     */
    @Bean
    public Binding productStockCreatedBinding(Queue productHealthSagaQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(productHealthSagaQueue)
                .to(sagaExchange)
                .with(PRODUCT_STOCK_CREATED_ROUTING_KEY);
    }

    /**
     * Bind product-health queue to receive stock failure events.
     */
    @Bean
    public Binding productStockFailedBinding(Queue productHealthSagaQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(productHealthSagaQueue)
                .to(sagaExchange)
                .with(PRODUCT_STOCK_FAILED_ROUTING_KEY);
    }

    /**
     * Bind pharmacy-inventory queue to receive product creation events.
     */
    @Bean
    public Binding productCreatedBinding(Queue pharmacyInventoryProductSagaQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(pharmacyInventoryProductSagaQueue)
                .to(sagaExchange)
                .with(PRODUCT_CREATED_ROUTING_KEY);
    }

    /**
     * Bind pharmacy-inventory queue to receive product deletion events (for cleanup).
     */
    @Bean
    public Binding productDeletedBinding(Queue pharmacyInventoryProductSagaQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(pharmacyInventoryProductSagaQueue)
                .to(sagaExchange)
                .with(PRODUCT_DELETED_ROUTING_KEY);
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
