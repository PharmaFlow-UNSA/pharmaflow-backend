package com.pharmaflow.smartfeatures.messaging.saga;

import com.pharmaflow.smartfeatures.config.RabbitMqSagaConfig.SagaRabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SmartSagaEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final SagaRabbitProperties properties;

  public SmartSagaEventPublisher(RabbitTemplate rabbitTemplate, SagaRabbitProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publishReservationRequested(ReservationRequestedEvent event) {
    publish(properties.getRecommendationReservationRequestedRoutingKey(), event);
  }

  public void publishReservationFinalized(RecommendationReservationFinalizedEvent event) {
    publish(properties.getRecommendationReservationFinalizedRoutingKey(), event);
  }

  public void publishCompensationRequested(ReservationCompensationRequestedEvent event) {
    publish(properties.getReservationCompensationRequestedRoutingKey(), event);
  }

  private void publish(String routingKey, Object event) {
    if (!properties.isEnabled()) {
      return;
    }
    rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event);
  }
}
