package com.pharmaflow.pharmacyinventory.messaging.saga;

import com.pharmaflow.pharmacyinventory.models.OrderStockReservation;
import com.pharmaflow.pharmacyinventory.service.OrderStockSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryOrderFulfillmentListenerTest {

    @Mock private OrderStockSagaService sagaService;
    @Mock private InventorySagaEventPublisher eventPublisher;

    private InventoryOrderFulfillmentListener listener;

    @BeforeEach
    void setUp() {
        listener = new InventoryOrderFulfillmentListener(sagaService, eventPublisher);
    }

    @Test
    void onOrderPlacedShouldPublishReservedEventOnSuccess() {
        OrderStockReservation reservation = new OrderStockReservation();
        reservation.setCorrelationId("corr-1");
        reservation.setOrderId(42L);
        reservation.setStatus(OrderStockReservation.Status.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        when(sagaService.reserveStockForOrder(any(OrderPlacedEvent.class))).thenReturn(reservation);

        listener.onOrderPlaced(placedEvent());

        ArgumentCaptor<OrderStockReservedEvent> captor = ArgumentCaptor.forClass(OrderStockReservedEvent.class);
        verify(eventPublisher).publishOrderStockReserved(captor.capture());
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
        assertThat(captor.getValue().orderId()).isEqualTo(42L);
        verify(eventPublisher, never()).publishOrderStockRejected(any());
    }

    @Test
    void onOrderPlacedShouldPublishRejectedEventWhenServiceThrows() {
        when(sagaService.reserveStockForOrder(any()))
                .thenThrow(new IllegalStateException("Insufficient stock"));

        listener.onOrderPlaced(placedEvent());

        ArgumentCaptor<OrderStockRejectedEvent> captor = ArgumentCaptor.forClass(OrderStockRejectedEvent.class);
        verify(eventPublisher).publishOrderStockRejected(captor.capture());
        assertThat(captor.getValue().reason()).contains("Insufficient stock");
        verify(eventPublisher, never()).publishOrderStockReserved(any());
    }

    @Test
    void onOrderCancelledShouldPublishRestockedEvent() {
        OrderStockReservation reservation = new OrderStockReservation();
        reservation.setCorrelationId("corr-1");
        when(sagaService.restockForCancellation(any())).thenReturn(reservation);

        listener.onOrderCancelled(new OrderCancelledEvent(
                "corr-1", 42L, "USER_CANCELLED", LocalDateTime.now()));

        ArgumentCaptor<OrderStockRestockedEvent> captor = ArgumentCaptor.forClass(OrderStockRestockedEvent.class);
        verify(eventPublisher).publishOrderStockRestocked(captor.capture());
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
        assertThat(captor.getValue().orderId()).isEqualTo(42L);
    }

    private OrderPlacedEvent placedEvent() {
        return new OrderPlacedEvent(
                "corr-1", 42L, 1L, 7L,
                List.of(new OrderPlacedItem(100L, "Aspirin", 1, null)),
                LocalDateTime.now());
    }
}
