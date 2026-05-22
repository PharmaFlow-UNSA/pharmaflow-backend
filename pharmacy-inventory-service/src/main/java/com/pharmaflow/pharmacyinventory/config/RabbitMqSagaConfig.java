package com.pharmaflow.pharmacyinventory.config;

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

@EnableRabbit
@Configuration
public class RabbitMqSagaConfig {

    @Bean
    @ConfigurationProperties(prefix = "pharmaflow.rabbitmq")
    public SagaRabbitProperties sagaRabbitProperties() {
        return new SagaRabbitProperties();
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
    public DirectExchange sagaExchange(SagaRabbitProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue recommendationReservationRequestedQueue(SagaRabbitProperties properties) {
        return new Queue(properties.getInventoryReservationRequestedQueue(), true);
    }

    @Bean
    public Queue reservationCompensationRequestedQueue(SagaRabbitProperties properties) {
        return new Queue(properties.getInventoryCompensationRequestedQueue(), true);
    }

    @Bean
    public Binding recommendationReservationRequestedBinding(
            SagaRabbitProperties properties,
            DirectExchange sagaExchange,
            Queue recommendationReservationRequestedQueue) {
        return BindingBuilder.bind(recommendationReservationRequestedQueue)
                .to(sagaExchange)
                .with(properties.getRecommendationReservationRequestedRoutingKey());
    }

    @Bean
    public Binding reservationCompensationRequestedBinding(
            SagaRabbitProperties properties,
            DirectExchange sagaExchange,
            Queue reservationCompensationRequestedQueue) {
        return BindingBuilder.bind(reservationCompensationRequestedQueue)
                .to(sagaExchange)
                .with(properties.getReservationCompensationRequestedRoutingKey());
    }

    public static class SagaRabbitProperties {

        private boolean enabled = true;
        private String exchange = "pharmaflow.saga.exchange";
        private String inventoryReservationRequestedQueue = "inventory.reservation.requested.queue";
        private String inventoryCompensationRequestedQueue = "inventory.compensation.requested.queue";
        private String recommendationReservationRequestedRoutingKey =
                "recommendation.reservation.requested";
        private String reservationCreatedRoutingKey = "reservation.created";
        private String reservationRejectedRoutingKey = "reservation.rejected";
        private String reservationCompensationRequestedRoutingKey =
                "reservation.compensation.requested";
        private String reservationCompensatedRoutingKey = "reservation.compensated";

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

        public String getInventoryReservationRequestedQueue() {
            return inventoryReservationRequestedQueue;
        }

        public void setInventoryReservationRequestedQueue(String inventoryReservationRequestedQueue) {
            this.inventoryReservationRequestedQueue = inventoryReservationRequestedQueue;
        }

        public String getInventoryCompensationRequestedQueue() {
            return inventoryCompensationRequestedQueue;
        }

        public void setInventoryCompensationRequestedQueue(String inventoryCompensationRequestedQueue) {
            this.inventoryCompensationRequestedQueue = inventoryCompensationRequestedQueue;
        }

        public String getRecommendationReservationRequestedRoutingKey() {
            return recommendationReservationRequestedRoutingKey;
        }

        public void setRecommendationReservationRequestedRoutingKey(
                String recommendationReservationRequestedRoutingKey) {
            this.recommendationReservationRequestedRoutingKey =
                    recommendationReservationRequestedRoutingKey;
        }

        public String getReservationCreatedRoutingKey() {
            return reservationCreatedRoutingKey;
        }

        public void setReservationCreatedRoutingKey(String reservationCreatedRoutingKey) {
            this.reservationCreatedRoutingKey = reservationCreatedRoutingKey;
        }

        public String getReservationRejectedRoutingKey() {
            return reservationRejectedRoutingKey;
        }

        public void setReservationRejectedRoutingKey(String reservationRejectedRoutingKey) {
            this.reservationRejectedRoutingKey = reservationRejectedRoutingKey;
        }

        public String getReservationCompensationRequestedRoutingKey() {
            return reservationCompensationRequestedRoutingKey;
        }

        public void setReservationCompensationRequestedRoutingKey(
                String reservationCompensationRequestedRoutingKey) {
            this.reservationCompensationRequestedRoutingKey = reservationCompensationRequestedRoutingKey;
        }

        public String getReservationCompensatedRoutingKey() {
            return reservationCompensatedRoutingKey;
        }

        public void setReservationCompensatedRoutingKey(String reservationCompensatedRoutingKey) {
            this.reservationCompensatedRoutingKey = reservationCompensatedRoutingKey;
        }
    }
}
