package com.pharmaflow.orderprescription.messaging.saga;

import com.pharmaflow.orderprescription.service.OrderFulfillmentSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentSagaListenerTest {

    @Mock
    private OrderFulfillmentSagaService sagaService;

    private OrderFulfillmentSagaListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderFulfillmentSagaListener(sagaService);
    }

    @Test
    void onStockReservedShouldDelegateToService() {
        OrderStockReservedEvent event = new OrderStockReservedEvent("corr-1", 10L, LocalDateTime.now());
        listener.onStockReserved(event);
        verify(sagaService).handleStockReserved(event);
    }

    @Test
    void onStockRejectedShouldDelegateToService() {
        OrderStockRejectedEvent event = new OrderStockRejectedEvent(
                "corr-1", 10L, "insufficient stock", LocalDateTime.now());
        listener.onStockRejected(event);
        verify(sagaService).handleStockRejected(event);
    }

    @Test
    void onStockRestockedShouldDelegateToService() {
        OrderStockRestockedEvent event = new OrderStockRestockedEvent(
                "corr-1", 10L, LocalDateTime.now());
        listener.onStockRestocked(event);
        verify(sagaService).handleStockRestocked(event);
    }
}
