package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.messaging.saga.OrderCancelledEvent;
import com.pharmaflow.pharmacyinventory.messaging.saga.OrderPlacedEvent;
import com.pharmaflow.pharmacyinventory.messaging.saga.OrderPlacedItem;
import com.pharmaflow.pharmacyinventory.models.Inventory;
import com.pharmaflow.pharmacyinventory.models.OrderStockReservation;
import com.pharmaflow.pharmacyinventory.repositories.InventoryRepository;
import com.pharmaflow.pharmacyinventory.repositories.OrderStockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inventory-side handler for the Order Fulfillment saga (Zadatak 8.3).
 *
 * <p>The two operations are:
 * <ul>
 *   <li>{@link #reserveStockForOrder(OrderPlacedEvent)} — deduct stock for every
 *       order item inside a single local transaction. If any line cannot be
 *       satisfied the whole transaction rolls back (atomicity) and the caller
 *       publishes {@code order.stock.rejected}.</li>
 *   <li>{@link #restockForCancellation(OrderCancelledEvent)} — inverse action;
 *       reads the saved {@link OrderStockReservation} and adds the previously
 *       deducted quantities back.</li>
 * </ul>
 */
@Service
public class OrderStockSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderStockSagaService.class);

    private final InventoryRepository inventoryRepository;
    private final OrderStockReservationRepository reservationRepository;

    public OrderStockSagaService(InventoryRepository inventoryRepository,
                                 OrderStockReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Deducts stock for the order. If any item can't be satisfied this throws,
     * rolling back the JPA transaction so no partial deduction is persisted.
     *
     * @return the persisted reservation record (idempotent — returns the
     *         existing row on redelivery).
     */
    @Transactional
    public OrderStockReservation reserveStockForOrder(OrderPlacedEvent event) {
        Optional<OrderStockReservation> existing =
                reservationRepository.findByCorrelationId(event.correlationId());
        if (existing.isPresent()) {
            log.info("Idempotent skip: reservation already exists for correlationId={}",
                    event.correlationId());
            return existing.get();
        }

        OrderStockReservation reservation = new OrderStockReservation();
        reservation.setCorrelationId(event.correlationId());
        reservation.setOrderId(event.orderId());
        reservation.setPharmacyId(event.pharmacyId());
        reservation.setStatus(OrderStockReservation.Status.RESERVED);

        List<OrderStockReservation.Item> items = new ArrayList<>();
        for (OrderPlacedItem line : event.items()) {
            Inventory inventory = inventoryRepository
                    .findByPharmacyIdAndProductId(event.pharmacyId(), line.productId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No inventory row for pharmacyId=" + event.pharmacyId()
                                    + " productId=" + line.productId()));

            if (inventory.getQuantity() == null || inventory.getQuantity() < line.quantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for productId=" + line.productId()
                                + " requested=" + line.quantity()
                                + " available=" + inventory.getQuantity());
            }

            inventory.setQuantity(inventory.getQuantity() - line.quantity());
            inventoryRepository.save(inventory);

            OrderStockReservation.Item item = new OrderStockReservation.Item();
            item.setProductId(line.productId());
            item.setQuantity(line.quantity());
            item.setInventoryId(inventory.getId());
            item.setReservation(reservation);
            items.add(item);
        }
        reservation.setItems(items);

        OrderStockReservation saved = reservationRepository.save(reservation);
        log.info("Stock reserved for correlationId={} orderId={} items={}",
                event.correlationId(), event.orderId(), items.size());
        return saved;
    }

    /**
     * Inverse action: restore stock for a previously reserved correlationId.
     * No-op if no reservation exists (rejection path — nothing was deducted).
     */
    @Transactional
    public OrderStockReservation restockForCancellation(OrderCancelledEvent event) {
        OrderStockReservation reservation = reservationRepository
                .findByCorrelationId(event.correlationId())
                .orElseThrow(() -> new IllegalStateException(
                        "No reservation to restock for correlationId=" + event.correlationId()));

        if (reservation.getStatus() == OrderStockReservation.Status.RESTOCKED) {
            log.info("Idempotent skip: reservation already RESTOCKED for correlationId={}",
                    event.correlationId());
            return reservation;
        }

        for (OrderStockReservation.Item item : reservation.getItems()) {
            Inventory inventory = inventoryRepository.findById(item.getInventoryId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Inventory missing for id=" + item.getInventoryId()));
            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);
        }
        reservation.setStatus(OrderStockReservation.Status.RESTOCKED);
        reservation.setUpdatedAt(LocalDateTime.now());

        OrderStockReservation saved = reservationRepository.save(reservation);
        log.info("Restocked for correlationId={} orderId={} items={} reason={}",
                event.correlationId(), event.orderId(), reservation.getItems().size(), event.reason());
        return saved;
    }
}
