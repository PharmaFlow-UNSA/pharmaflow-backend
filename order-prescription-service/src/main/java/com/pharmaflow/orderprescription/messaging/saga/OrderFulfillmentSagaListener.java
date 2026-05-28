package com.pharmaflow.orderprescription.messaging.saga;

import com.pharmaflow.orderprescription.service.OrderFulfillmentSagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Listens for inventory replies on the order-fulfillment saga.
 *
 * <p>Disabled (along with the publisher being a no-op) when
 * {@code pharmaflow.rabbitmq.enabled=false}, which is the default for tests.
 */
@Component
@ConditionalOnProperty(prefix = "pharmaflow.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderFulfillmentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentSagaListener.class);

    private final OrderFulfillmentSagaService sagaService;

    public OrderFulfillmentSagaListener(OrderFulfillmentSagaService sagaService) {
        this.sagaService = sagaService;
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.order-stock-reserved-queue}")
    public void onStockReserved(OrderStockReservedEvent event) {
        log.info("Received order.stock.reserved for correlationId={}", event.correlationId());
        sagaService.handleStockReserved(event);
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.order-stock-rejected-queue}")
    public void onStockRejected(OrderStockRejectedEvent event) {
        log.warn("Received order.stock.rejected for correlationId={} reason={}",
                event.correlationId(), event.reason());
        sagaService.handleStockRejected(event);
    }

    @RabbitListener(queues = "${pharmaflow.rabbitmq.order-stock-restocked-queue}")
    public void onStockRestocked(OrderStockRestockedEvent event) {
        log.info("Received order.stock.restocked for correlationId={}", event.correlationId());
        sagaService.handleStockRestocked(event);
    }
}
