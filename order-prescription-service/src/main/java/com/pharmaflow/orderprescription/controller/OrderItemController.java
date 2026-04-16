package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
@Tag(name = "Order Item Management", description = "APIs for managing order items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping
    @Operation(summary = "Get all order items", description = "Retrieves a list of all order items")
    public ResponseEntity<List<OrderItemDTO>> getAllOrderItems() {
        return ResponseEntity.ok(orderItemService.getAllOrderItems());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order item by ID", description = "Retrieves a specific order item by its ID")
    public ResponseEntity<OrderItemDTO> getOrderItemById(@PathVariable Long id) {
        return ResponseEntity.ok(orderItemService.getOrderItemById(id));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get order items by order ID", description = "Retrieves all items for a specific order")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderItemService.getOrderItemsByOrderId(orderId));
    }

    @PostMapping
    @Operation(summary = "Create a new order item", description = "Creates a new order item")
    public ResponseEntity<OrderItemDTO> createOrderItem(@Valid @RequestBody OrderItemDTO orderItemDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemService.createOrderItem(orderItemDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order item", description = "Updates an existing order item")
    public ResponseEntity<OrderItemDTO> updateOrderItem(@PathVariable Long id, @Valid @RequestBody OrderItemDTO orderItemDTO) {
        return ResponseEntity.ok(orderItemService.updateOrderItem(id, orderItemDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order item", description = "Deletes an order item by ID")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Long id) {
        orderItemService.deleteOrderItem(id);
        return ResponseEntity.noContent().build();
    }
}
