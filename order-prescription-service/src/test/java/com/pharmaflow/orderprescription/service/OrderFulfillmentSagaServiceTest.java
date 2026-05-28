package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.messaging.saga.OrderCancelledEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderPlacedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderSagaEventPublisher;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockRejectedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockReservedEvent;
import com.pharmaflow.orderprescription.messaging.saga.OrderStockRestockedEvent;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderFulfillmentSaga;
import com.pharmaflow.orderprescription.repositories.OrderFulfillmentSagaRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentSagaServiceTest {

    @Mock private OrderService orderService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderFulfillmentSagaRepository sagaRepository;
    @Mock private OrderSagaEventPublisher eventPublisher;

    private OrderFulfillmentSagaService sagaService;

    @BeforeEach
    void setUp() {
        sagaService = new OrderFulfillmentSagaService(
                orderService, orderRepository, sagaRepository, eventPublisher);
    }

    @Test
    void startSagaShouldCreateOrderPersistSagaAndPublishOrderPlaced() {
        OrderCreateDTO request = new OrderCreateDTO();
        request.setUserId(1L);
        request.setShippingAddress("Some Street 1");

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(100L);
        item.setProductName("Aspirin");
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(5));
        request.setOrderItems(List.of(item));

        OrderDTO saved = new OrderDTO();
        saved.setId(42L);
        saved.setUserId(1L);
        saved.setStatus("PENDING");
        saved.setOrderItems(List.of(item));

        when(orderService.createOrder(request)).thenReturn(saved);
        when(sagaRepository.save(any(OrderFulfillmentSaga.class))).thenAnswer(i -> i.getArgument(0));

        String correlationId = sagaService.startSaga(request, 7L);

        assertThat(correlationId).isNotBlank();

        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(eventPublisher).publishOrderPlaced(eventCaptor.capture());
        OrderPlacedEvent event = eventCaptor.getValue();
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.orderId()).isEqualTo(42L);
        assertThat(event.pharmacyId()).isEqualTo(7L);
        assertThat(event.items()).hasSize(1);

        // saga rows: STARTED save and STOCK_PENDING save (we count >=2)
        verify(sagaRepository, atLeast(2)).save(any(OrderFulfillmentSaga.class));
    }

    @Test
    void handleStockReservedShouldMarkOrderConfirmedAndSagaCompleted() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId("corr-1");
        saga.setOrderId(42L);
        saga.setStatus(OrderFulfillmentSaga.Status.STOCK_PENDING);

        Order order = new Order();
        order.setId(42L);
        order.setStatus("PENDING");

        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sagaService.handleStockReserved(new OrderStockReservedEvent("corr-1", 42L, LocalDateTime.now()));

        assertThat(saga.getStatus()).isEqualTo(OrderFulfillmentSaga.Status.COMPLETED);
        assertThat(saga.getCompletedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository).save(order);
    }

    @Test
    void handleStockReservedShouldBeIdempotent() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setStatus(OrderFulfillmentSaga.Status.COMPLETED);
        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));

        sagaService.handleStockReserved(new OrderStockReservedEvent("corr-1", 42L, LocalDateTime.now()));

        verify(sagaRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleStockRejectedShouldCancelOrderAndCompensateSaga() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId("corr-1");
        saga.setOrderId(42L);
        saga.setStatus(OrderFulfillmentSaga.Status.STOCK_PENDING);

        Order order = new Order();
        order.setId(42L);
        order.setStatus("PENDING");

        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sagaService.handleStockRejected(new OrderStockRejectedEvent(
                "corr-1", 42L, "insufficient stock", LocalDateTime.now()));

        assertThat(saga.getStatus()).isEqualTo(OrderFulfillmentSaga.Status.COMPENSATED);
        assertThat(saga.getErrorMessage()).contains("insufficient stock");
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(order);
    }

    @Test
    void handleStockRejectedShouldBeIdempotent() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setStatus(OrderFulfillmentSaga.Status.COMPENSATED);
        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));

        sagaService.handleStockRejected(new OrderStockRejectedEvent(
                "corr-1", 42L, "anything", LocalDateTime.now()));

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void cancelConfirmedOrderShouldOnlyAcceptCompletedSagas() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId("corr-1");
        saga.setStatus(OrderFulfillmentSaga.Status.STOCK_PENDING);
        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));

        assertThatThrownBy(() -> sagaService.cancelConfirmedOrder("corr-1", "user"))
                .isInstanceOf(IllegalStateException.class);

        verify(eventPublisher, never()).publishOrderCancelled(any());
    }

    @Test
    void cancelConfirmedOrderShouldPublishOrderCancelled() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId("corr-1");
        saga.setOrderId(42L);
        saga.setStatus(OrderFulfillmentSaga.Status.COMPLETED);
        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));
        when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sagaService.cancelConfirmedOrder("corr-1", "USER_CANCELLED");

        assertThat(saga.getStatus()).isEqualTo(OrderFulfillmentSaga.Status.CANCELLING);
        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishOrderCancelled(captor.capture());
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
        assertThat(captor.getValue().reason()).isEqualTo("USER_CANCELLED");
    }

    @Test
    void handleStockRestockedShouldMarkSagaCancelled() {
        OrderFulfillmentSaga saga = new OrderFulfillmentSaga();
        saga.setCorrelationId("corr-1");
        saga.setOrderId(42L);
        saga.setStatus(OrderFulfillmentSaga.Status.CANCELLING);

        Order order = new Order();
        order.setId(42L);
        order.setStatus("CONFIRMED");

        when(sagaRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(saga));
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(sagaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        sagaService.handleStockRestocked(new OrderStockRestockedEvent(
                "corr-1", 42L, LocalDateTime.now()));

        assertThat(saga.getStatus()).isEqualTo(OrderFulfillmentSaga.Status.CANCELLED);
        assertThat(saga.getCompletedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo("CANCELLED");
    }
}
