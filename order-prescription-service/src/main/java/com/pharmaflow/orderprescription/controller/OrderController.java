package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves a list of all orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by its ID")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID", description = "Retrieves all orders for a specific user")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves all orders with a specific status")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with items and payment")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(orderCreateDTO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Updates an existing order")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Deletes an order by ID")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
