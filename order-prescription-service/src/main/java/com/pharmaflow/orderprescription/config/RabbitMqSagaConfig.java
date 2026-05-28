package com.pharmaflow.orderprescription.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Order Fulfillment saga (Zadatak 8.3).
 *
 * Choreography:
 *  - order-prescription publishes {@code order.placed} when an order is created (T1)
 *  - pharmacy-inventory listens, deducts stock (T2), then publishes
 *    {@code order.stock.reserved} (success) or {@code order.stock.rejected} (failure)
 *  - order-prescription listens for both replies and either marks the order CONFIRMED
 *    (final state) or runs the inverse action (cancel order) when the reservation fails
 *  - if an already-confirmed order is later cancelled the order service publishes
 *    {@code order.cancelled}; pharmacy-inventory restocks and emits {@code order.stock.restocked}
 *
 * A single direct exchange ({@code pharmaflow.saga.exchange}) is shared with the
 * existing recommendation-reservation saga; routing keys are namespaced with
 * the {@code order.*} prefix so the two flows do not collide.
 */
@EnableRabbit
@Configuration
public class RabbitMqSagaConfig {

    @Bean
    @ConfigurationProperties(prefix = "pharmaflow.rabbitmq")
    public OrderSagaRabbitProperties orderSagaRabbitProperties() {
        return new OrderSagaRabbitProperties();
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }

    @Bean
    public DirectExchange sagaExchange(OrderSagaRabbitProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    // Reply queues consumed by order-prescription-service.

    @Bean
    public Queue orderStockReservedQueue(OrderSagaRabbitProperties properties) {
        return new Queue(properties.getOrderStockReservedQueue(), true);
    }

    @Bean
    public Queue orderStockRejectedQueue(OrderSagaRabbitProperties properties) {
        return new Queue(properties.getOrderStockRejectedQueue(), true);
    }

    @Bean
    public Queue orderStockRestockedQueue(OrderSagaRabbitProperties properties) {
        return new Queue(properties.getOrderStockRestockedQueue(), true);
    }

    @Bean
    public Binding orderStockReservedBinding(
            OrderSagaRabbitProperties properties,
            DirectExchange sagaExchange,
            Queue orderStockReservedQueue) {
        return BindingBuilder.bind(orderStockReservedQueue)
                .to(sagaExchange)
                .with(properties.getOrderStockReservedRoutingKey());
    }

    @Bean
    public Binding orderStockRejectedBinding(
            OrderSagaRabbitProperties properties,
            DirectExchange sagaExchange,
            Queue orderStockRejectedQueue) {
        return BindingBuilder.bind(orderStockRejectedQueue)
                .to(sagaExchange)
                .with(properties.getOrderStockRejectedRoutingKey());
    }

    @Bean
    public Binding orderStockRestockedBinding(
            OrderSagaRabbitProperties properties,
            DirectExchange sagaExchange,
            Queue orderStockRestockedQueue) {
        return BindingBuilder.bind(orderStockRestockedQueue)
                .to(sagaExchange)
                .with(properties.getOrderStockRestockedRoutingKey());
    }

    public static class OrderSagaRabbitProperties {

        private boolean enabled = true;
        private String exchange = "pharmaflow.saga.exchange";

        // Queues consumed by order-prescription-service
        private String orderStockReservedQueue = "order.stock.reserved.queue";
        private String orderStockRejectedQueue = "order.stock.rejected.queue";
        private String orderStockRestockedQueue = "order.stock.restocked.queue";

        // Routing keys
        private String orderPlacedRoutingKey = "order.placed";
        private String orderCancelledRoutingKey = "order.cancelled";
        private String orderStockReservedRoutingKey = "order.stock.reserved";
        private String orderStockRejectedRoutingKey = "order.stock.rejected";
        private String orderStockRestockedRoutingKey = "order.stock.restocked";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getOrderStockReservedQueue() {
            return orderStockReservedQueue;
        }

        public void setOrderStockReservedQueue(String orderStockReservedQueue) {
            this.orderStockReservedQueue = orderStockReservedQueue;
        }

        public String getOrderStockRejectedQueue() {
            return orderStockRejectedQueue;
        }

        public void setOrderStockRejectedQueue(String orderStockRejectedQueue) {
            this.orderStockRejectedQueue = orderStockRejectedQueue;
        }

        public String getOrderStockRestockedQueue() {
            return orderStockRestockedQueue;
        }

        public void setOrderStockRestockedQueue(String orderStockRestockedQueue) {
            this.orderStockRestockedQueue = orderStockRestockedQueue;
        }

        public String getOrderPlacedRoutingKey() {
            return orderPlacedRoutingKey;
        }

        public void setOrderPlacedRoutingKey(String orderPlacedRoutingKey) {
            this.orderPlacedRoutingKey = orderPlacedRoutingKey;
        }

        public String getOrderCancelledRoutingKey() {
            return orderCancelledRoutingKey;
        }

        public void setOrderCancelledRoutingKey(String orderCancelledRoutingKey) {
            this.orderCancelledRoutingKey = orderCancelledRoutingKey;
        }

        public String getOrderStockReservedRoutingKey() {
            return orderStockReservedRoutingKey;
        }

        public void setOrderStockReservedRoutingKey(String orderStockReservedRoutingKey) {
            this.orderStockReservedRoutingKey = orderStockReservedRoutingKey;
        }

        public String getOrderStockRejectedRoutingKey() {
            return orderStockRejectedRoutingKey;
        }

        public void setOrderStockRejectedRoutingKey(String orderStockRejectedRoutingKey) {
            this.orderStockRejectedRoutingKey = orderStockRejectedRoutingKey;
        }

        public String getOrderStockRestockedRoutingKey() {
            return orderStockRestockedRoutingKey;
        }

        public void setOrderStockRestockedRoutingKey(String orderStockRestockedRoutingKey) {
            this.orderStockRestockedRoutingKey = orderStockRestockedRoutingKey;
        }
    }
}
