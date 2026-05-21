package com.pharmaflow.smartfeatures.messaging.saga;

import com.pharmaflow.smartfeatures.service.recommendation.RecommendationReservationSagaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pharmaflow.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmartReservationSagaListener {

  private final RecommendationReservationSagaService sagaService;

  public SmartReservationSagaListener(RecommendationReservationSagaService sagaService) {
    this.sagaService = sagaService;
  }

  @RabbitListener(queues = "${pharmaflow.rabbitmq.smart-reservation-created-queue}")
  public void onReservationCreated(ReservationCreatedEvent event) {
    sagaService.handleReservationCreated(event);
  }

  @RabbitListener(queues = "${pharmaflow.rabbitmq.smart-reservation-rejected-queue}")
  public void onReservationRejected(ReservationRejectedEvent event) {
    sagaService.handleReservationRejected(event);
  }

  @RabbitListener(queues = "${pharmaflow.rabbitmq.smart-reservation-compensated-queue}")
  public void onReservationCompensated(ReservationCompensatedEvent event) {
    sagaService.handleReservationCompensated(event);
  }
}
