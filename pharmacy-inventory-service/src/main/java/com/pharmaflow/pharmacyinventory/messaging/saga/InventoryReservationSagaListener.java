package com.pharmaflow.pharmacyinventory.messaging.saga;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pharmaflow.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryReservationSagaListener {

    private final ReservationService reservationService;
    private final InventorySagaEventPublisher eventPublisher;

    public InventoryReservationSagaListener(
            ReservationService reservationService, InventorySagaEventPublisher eventPublisher) {
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.inventory-reservation-requested-queue}")
    public void onReservationRequested(ReservationRequestedEvent event) {
        try {
            ReservationDTO reservation = reservationService.createReservationForSaga(event);
            eventPublisher.publishReservationCreated(
                    new ReservationCreatedEvent(
                            event.correlationId(),
                            reservation.getId(),
                            reservation.getStatus(),
                            LocalDateTime.now()));
        } catch (RuntimeException ex) {
            eventPublisher.publishReservationRejected(
                    new ReservationRejectedEvent(
                            event.correlationId(), ex.getMessage(), LocalDateTime.now()));
        }
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.inventory-compensation-requested-queue}")
    public void onReservationCompensationRequested(ReservationCompensationRequestedEvent event) {
        ReservationDTO reservation = reservationService.compensateReservationForSaga(event.correlationId());
        eventPublisher.publishReservationCompensated(
                new ReservationCompensatedEvent(
                        event.correlationId(), reservation.getId(), LocalDateTime.now()));
    }
}
