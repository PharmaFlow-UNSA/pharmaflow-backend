package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.messaging.saga.OrderCancelledEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderPlacedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderPlacedItem;
import com.pharmaflow.orderprescription.messaging.saga.OrderSagaEventPublisher;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockRejectedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockReservedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockRestockedEvent;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderFulfillmentSaga;
import com.pharmaflow.orderprescription.repositories.OrderFulfillmentSagaRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga orchestrator for the Order Fulfillment flow (Zadatak 8.3).
 *
 * <p>This service owns the order-side of the saga. The local transaction here
 * is the creation of an {@code Order} row, which is mirrored by an inventory
 * deduction in pharmacy-inventory-service. The orchestrator persists an
 * {@link OrderFulfillmentSaga} so progress survives restarts and replies are
 * processed idempotently.
 *
 * <p>Happy path: {@code STARTED → STOCK_PENDING → COMPLETED}. If inventory
 * rejects, the orchestrator runs the inverse action (cancel order, mark saga
 * {@code COMPENSATED}). If a confirmed order is later cancelled by the user,
 * {@code publishOrderCancelled} kicks off the restock leg and waits for
 * {@code COMPLETED → CANCELLING → CANCELLED}.
 */
@Service
public class OrderFulfillmentSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentSagaService.class);

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderFulfillmentSagaRepository sagaRepository;
    private final OrderSagaEventPublisher eventPublisher;

    public OrderFulfillmentSagaService(OrderService orderService,
                                       OrderRepository orderRepository,
                                       OrderFulfillmentSagaRepository sagaRepository,
                                       OrderSagaEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.sagaRepository = sagaRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Start the saga. Creates the order locally (T1) and publishes
     * {@code order.placed} so pharmacy-inventory can deduct stock (T2).
     *
     * @return the correlation/saga id
     */
    @Transactional
    public String startSaga(OrderCreateDTO request, Long pharmacyId) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Starting order-fulfillment saga: correlationId={}", correlationId);

        OrderDTO created = orderService.createOrder(request);

        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId(correlationId);
        saga.setOrderId(created.getId());
        saga.setPharmacyId(pharmacyId);
        saga.setStatus(OrderFulfillmentSaga.Status.STARTED);
        saga.setCurrentStep("ORDER_CREATED");
        sagaRepository.save(saga);

        OrderPlacedEvent event = new OrderPlacedEvent(
                correlationId,
                created.getId(),
                created.getUserId(),
                pharmacyId,
                toPlacedItems(created.getOrderItems()),
                LocalDateTime.now());
        eventPublisher.publishOrderPlaced(event);

        saga.setStatus(OrderFulfillmentSaga.Status.STOCK_PENDING);
        saga.setCurrentStep("STOCK_PENDING");
        sagaRepository.save(saga);

        return correlationId;
    }

    /**
     * Stock was reserved for every item. Mark the order CONFIRMED and the
     * saga as COMPLETED — this is the final, happy-path state.
     */
    @Transactional
    public void handleStockReserved(OrderStockReservedEvent event) {
        Optional<OrderFulfillmentSaga> opt =
                sagaRepository.findByCorrelationId(event.correlationId());
        if (opt.isEmpty()) {
            log.warn("Ignoring stock.reserved for unknown correlationId={}", event.correlationId());
            return;
        }

        OrderFulfillmentSaga saga = opt.get();
        if (saga.getStatus() == OrderFulfillmentSaga.Status.COMPLETED) {
            log.info("Saga already COMPLETED, idempotent skip: correlationId={}", saga.getCorrelationId());
            return;
        }

        Order order = orderRepository.findById(saga.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order missing for saga: orderId=" + saga.getOrderId()));
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        saga.setStatus(OrderFulfillmentSaga.Status.COMPLETED);
        saga.setCurrentStep("COMPLETED");
        saga.setCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        log.info("Order-fulfillment saga COMPLETED: correlationId={} orderId={}",
                saga.getCorrelationId(), saga.getOrderId());
    }

    /**
     * Inverse action — stock could not be reserved, so we cancel the locally
     * created order and mark the saga as COMPENSATED.
     */
    @Transactional
    public void handleStockRejected(OrderStockRejectedEvent event) {
        Optional<OrderFulfillmentSaga> opt =
                sagaRepository.findByCorrelationId(event.correlationId());
        if (opt.isEmpty()) {
            log.warn("Ignoring stock.rejected for unknown correlationId={}", event.correlationId());
            return;
        }

        OrderFulfillmentSaga saga = opt.get();
        if (saga.getStatus() == OrderFulfillmentSaga.Status.COMPENSATED
                || saga.getStatus() == OrderFulfillmentSaga.Status.COMPENSATING) {
            log.info("Saga already compensating/compensated, idempotent skip: correlationId={}",
                    saga.getCorrelationId());
            return;
        }

        saga.setStatus(OrderFulfillmentSaga.Status.COMPENSATING);
        saga.setCurrentStep("COMPENSATING");
        saga.setErrorMessage(event.reason());
        sagaRepository.save(saga);

        orderRepository.findById(saga.getOrderId()).ifPresent(order -> {
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });

        saga.setStatus(OrderFulfillmentSaga.Status.COMPENSATED);
        saga.setCurrentStep("COMPENSATED");
        saga.setCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        log.info("Order-fulfillment saga COMPENSATED: correlationId={} orderId={} reason={}",
                saga.getCorrelationId(), saga.getOrderId(), event.reason());
    }

    /**
     * Cancel a previously-confirmed order. The order is moved to
     * {@code CANCELLING} locally and an {@code order.cancelled} event is
     * emitted so pharmacy-inventory restocks the products.
     */
    @Transactional
    public void cancelConfirmedOrder(String correlationId, String reason) {
        OrderFulfillmentSaga saga = sagaRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Saga not found for correlationId: " + correlationId));

        if (saga.getStatus() != OrderFulfillmentSaga.Status.COMPLETED) {
            throw new IllegalStateException(
                    "Can only cancel a COMPLETED saga, current status=" + saga.getStatus());
        }

        saga.setStatus(OrderFulfillmentSaga.Status.CANCELLING);
        saga.setCurrentStep("CANCELLING");
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);

        eventPublisher.publishOrderCancelled(new OrderCancelledEvent(
                correlationId, saga.getOrderId(), reason, LocalDateTime.now()));

        log.info("Cancellation requested for saga: correlationId={} orderId={} reason={}",
                correlationId, saga.getOrderId(), reason);
    }

    /**
     * Restock confirmation from pharmacy-inventory — finalize the cancel.
     */
    @Transactional
    public void handleStockRestocked(OrderStockRestockedEvent event) {
        Optional<OrderFulfillmentSaga> opt =
                sagaRepository.findByCorrelationId(event.correlationId());
        if (opt.isEmpty()) {
            log.warn("Ignoring stock.restocked for unknown correlationId={}", event.correlationId());
            return;
        }

        OrderFulfillmentSaga saga = opt.get();
        if (saga.getStatus() == OrderFulfillmentSaga.Status.CANCELLED) {
            log.info("Saga already CANCELLED, idempotent skip: correlationId={}", saga.getCorrelationId());
            return;
        }

        orderRepository.findById(saga.getOrderId()).ifPresent(order -> {
            order.setStatus("CANCELLED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        });

        saga.setStatus(OrderFulfillmentSaga.Status.CANCELLED);
        saga.setCurrentStep("CANCELLED");
        saga.setCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

        log.info("Order-fulfillment saga CANCELLED (with restock): correlationId={} orderId={}",
                saga.getCorrelationId(), saga.getOrderId());
    }

    @Transactional(readOnly = true)
    public Optional<OrderFulfillmentSaga> findByCorrelationId(String correlationId) {
        return sagaRepository.findByCorrelationId(correlationId);
    }

    private List<OrderPlacedItem> toPlacedItems(List<OrderItemDTO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(i -> new OrderPlacedItem(
                        i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
    }
}
