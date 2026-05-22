package com.pharmaflow.smartfeatures.config;

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
  public Queue reservationCreatedQueue(SagaRabbitProperties properties) {
    return new Queue(properties.getSmartReservationCreatedQueue(), true);
  }

  @Bean
  public Queue reservationRejectedQueue(SagaRabbitProperties properties) {
    return new Queue(properties.getSmartReservationRejectedQueue(), true);
  }

  @Bean
  public Queue reservationCompensatedQueue(SagaRabbitProperties properties) {
    return new Queue(properties.getSmartReservationCompensatedQueue(), true);
  }

  @Bean
  public Binding reservationCreatedBinding(
      SagaRabbitProperties properties, DirectExchange sagaExchange, Queue reservationCreatedQueue) {
    return BindingBuilder.bind(reservationCreatedQueue)
        .to(sagaExchange)
        .with(properties.getReservationCreatedRoutingKey());
  }

  @Bean
  public Binding reservationRejectedBinding(
      SagaRabbitProperties properties, DirectExchange sagaExchange, Queue reservationRejectedQueue) {
    return BindingBuilder.bind(reservationRejectedQueue)
        .to(sagaExchange)
        .with(properties.getReservationRejectedRoutingKey());
  }

  @Bean
  public Binding reservationCompensatedBinding(
      SagaRabbitProperties properties,
      DirectExchange sagaExchange,
      Queue reservationCompensatedQueue) {
    return BindingBuilder.bind(reservationCompensatedQueue)
        .to(sagaExchange)
        .with(properties.getReservationCompensatedRoutingKey());
  }

  public static class SagaRabbitProperties {

    private boolean enabled = true;
    private String exchange = "pharmaflow.saga.exchange";
    private String smartReservationCreatedQueue = "smart.reservation.created.queue";
    private String smartReservationRejectedQueue = "smart.reservation.rejected.queue";
    private String smartReservationCompensatedQueue = "smart.reservation.compensated.queue";
    private String recommendationReservationRequestedRoutingKey =
        "recommendation.reservation.requested";
    private String reservationCreatedRoutingKey = "reservation.created";
    private String reservationRejectedRoutingKey = "reservation.rejected";
    private String reservationCompensationRequestedRoutingKey =
        "reservation.compensation.requested";
    private String reservationCompensatedRoutingKey = "reservation.compensated";
    private String recommendationReservationFinalizedRoutingKey =
        "recommendation.reservation.finalized";

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

    public String getSmartReservationCreatedQueue() {
      return smartReservationCreatedQueue;
    }

    public void setSmartReservationCreatedQueue(String smartReservationCreatedQueue) {
      this.smartReservationCreatedQueue = smartReservationCreatedQueue;
    }

    public String getSmartReservationRejectedQueue() {
      return smartReservationRejectedQueue;
    }

    public void setSmartReservationRejectedQueue(String smartReservationRejectedQueue) {
      this.smartReservationRejectedQueue = smartReservationRejectedQueue;
    }

    public String getSmartReservationCompensatedQueue() {
      return smartReservationCompensatedQueue;
    }

    public void setSmartReservationCompensatedQueue(String smartReservationCompensatedQueue) {
      this.smartReservationCompensatedQueue = smartReservationCompensatedQueue;
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

    public String getRecommendationReservationFinalizedRoutingKey() {
      return recommendationReservationFinalizedRoutingKey;
    }

    public void setRecommendationReservationFinalizedRoutingKey(
        String recommendationReservationFinalizedRoutingKey) {
      this.recommendationReservationFinalizedRoutingKey =
          recommendationReservationFinalizedRoutingKey;
    }
  }
}
