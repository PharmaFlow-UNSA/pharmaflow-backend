package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.messaging.saga.OrderCancelledEvent;
import com.pharmaflow.pharmacyinventory.messaging.saga.OrderPlacedEvent;
import com.pharmaflow.pharmacyinventory.messaging.saga.OrderPlacedItem;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.OrderStockReservation;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
import com.pharmaflow.pharmacyinventory.repositories.OrderStockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStockSagaServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrderStockReservationRepository reservationRepository;

    private OrderStockSagaService service;

    @BeforeEach
    void setUp() {
        service = new OrderStockSagaService(inventoryRepository, reservationRepository);
    }

    @Test
    void reserveStockForOrderShouldDeductInventoryAndSaveReservation() {
        Inventory inv = inventory(1L, 100L, 10);

        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.empty());
        when(inventoryRepository.findByPharmacyIdAndProductId(7L, 100L)).thenReturn(Optional.of(inv));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        OrderPlacedEvent event = new OrderPlacedEvent(
                "corr-1", 42L, 1L, 7L,
                List.of(new OrderPlacedItem(100L, "Aspirin", 3, BigDecimal.ONE)),
                LocalDateTime.now());

        OrderStockReservation saved = service.reserveStockForOrder(event);

        assertThat(inv.getQuantity()).isEqualTo(7);
        assertThat(saved.getStatus()).isEqualTo(OrderStockReservation.Status.RESERVED);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(saved.getItems().get(0).getInventoryId()).isEqualTo(1L);
    }

    @Test
    void reserveStockForOrderShouldThrowWhenInventoryInsufficient() {
        Inventory inv = inventory(1L, 100L, 2);
        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.empty());
        when(inventoryRepository.findByPharmacyIdAndProductId(7L, 100L)).thenReturn(Optional.of(inv));

        OrderPlacedEvent event = new OrderPlacedEvent(
                "corr-1", 42L, 1L, 7L,
                List.of(new OrderPlacedItem(100L, "Aspirin", 5, BigDecimal.ONE)),
                LocalDateTime.now());

        assertThatThrownBy(() -> service.reserveStockForOrder(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");

        // No reservation persisted on the failure path — the @Transactional caller will roll back
        // the inventory mutation too, but at the unit level we still want to verify we never
        // called save() on the reservation repo.
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveStockForOrderShouldThrowWhenInventoryRowIsMissing() {
        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.empty());
        when(inventoryRepository.findByPharmacyIdAndProductId(7L, 100L)).thenReturn(Optional.empty());

        OrderPlacedEvent event = new OrderPlacedEvent(
                "corr-1", 42L, 1L, 7L,
                List.of(new OrderPlacedItem(100L, "Aspirin", 1, BigDecimal.ONE)),
                LocalDateTime.now());

        assertThatThrownBy(() -> service.reserveStockForOrder(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No inventory row");
    }

    @Test
    void reserveStockForOrderShouldBeIdempotentOnRedelivery() {
        OrderStockReservation existing = new OrderStockReservation();
        existing.setCorrelationId("corr-1");
        existing.setStatus(OrderStockReservation.Status.RESERVED);
        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(existing));

        OrderPlacedEvent event = new OrderPlacedEvent(
                "corr-1", 42L, 1L, 7L,
                List.of(new OrderPlacedItem(100L, "Aspirin", 5, BigDecimal.ONE)),
                LocalDateTime.now());

        OrderStockReservation result = service.reserveStockForOrder(event);

        assertThat(result).isSameAs(existing);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void restockForCancellationShouldRestoreInventory() {
        Inventory inv = inventory(1L, 100L, 5);
        OrderStockReservation reservation = new OrderStockReservation();
        reservation.setCorrelationId("corr-1");
        reservation.setStatus(OrderStockReservation.Status.RESERVED);

        OrderStockReservation.Item item = new OrderStockReservation.Item();
        item.setInventoryId(1L);
        item.setProductId(100L);
        item.setQuantity(3);
        item.setReservation(reservation);
        reservation.setItems(new ArrayList<>(List.of(item)));

        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.restockForCancellation(new OrderCancelledEvent(
                "corr-1", 42L, "USER_CANCELLED", LocalDateTime.now()));

        assertThat(inv.getQuantity()).isEqualTo(8);
        assertThat(reservation.getStatus()).isEqualTo(OrderStockReservation.Status.RESTOCKED);
        ArgumentCaptor<OrderStockReservation> captor = ArgumentCaptor.forClass(OrderStockReservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStockReservation.Status.RESTOCKED);
    }

    @Test
    void restockForCancellationShouldBeIdempotent() {
        OrderStockReservation reservation = new OrderStockReservation();
        reservation.setStatus(OrderStockReservation.Status.RESTOCKED);
        when(reservationRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(reservation));

        service.restockForCancellation(new OrderCancelledEvent(
                "corr-1", 42L, "rerun", LocalDateTime.now()));

        verify(inventoryRepository, never()).findById(any());
        verify(reservationRepository, never()).save(any());
    }

    private Inventory inventory(Long id, Long productId, int qty) {
        Inventory i = new Inventory();
        i.setId(id);
        i.setProductId(productId);
        i.setQuantity(qty);
        return i;
    }
}
