package com.pharmaflow.pharmacyinventory.messaging.saga;

import com.pharmaflow.pharmacyinventory.models.OrderStockReservation;
import com.pharmaflow.pharmacyinventory.service.OrderStockSagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Inventory-side listener for the order-fulfillment saga (Zadatak 8.3).
 *
 * <p>Handles two inbound events:
 * <ul>
 *   <li>{@code order.placed} — deduct stock and reply with {@code order.stock.reserved}
 *       or {@code order.stock.rejected}.</li>
 *   <li>{@code order.cancelled} — restock and reply with {@code order.stock.restocked}.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "pharmaflow.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventoryOrderFulfillmentListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryOrderFulfillmentListener.class);

    private final OrderStockSagaService sagaService;
    private final InventorySagaEventPublisher eventPublisher;

    public InventoryOrderFulfillmentListener(OrderStockSagaService sagaService,
                                             InventorySagaEventPublisher eventPublisher) {
        this.sagaService = sagaService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.order-placed-queue}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed correlationId={} orderId={}",
                event.correlationId(), event.orderId());
        try {
            OrderStockReservation reservation = sagaService.reserveStockForOrder(event);
            eventPublisher.publishOrderStockReserved(new OrderStockReservedEvent(
                    event.correlationId(),
                    event.orderId(),
                    reservation.getCreatedAt() != null ? reservation.getCreatedAt() : LocalDateTime.now()));
        } catch (RuntimeException ex) {
            log.warn("Stock reservation rejected for correlationId={}: {}",
                    event.correlationId(), ex.getMessage());
            eventPublisher.publishOrderStockRejected(new OrderStockRejectedEvent(
                    event.correlationId(),
                    event.orderId(),
                    ex.getMessage(),
                    LocalDateTime.now()));
        }
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.order-cancelled-queue}")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled correlationId={} orderId={} reason={}",
                event.correlationId(), event.orderId(), event.reason());
        sagaService.restockForCancellation(event);
        eventPublisher.publishOrderStockRestocked(new OrderStockRestockedEvent(
                event.correlationId(), event.orderId(), LocalDateTime.now()));
    }
}
