package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(
            summary = "Get all orders",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by userId and status."
    )
    public ResponseEntity<Page<OrderDTO>> getOrders(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.findAll(userId, status, pageable));
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

    @PostMapping("/batch")
    @Operation(summary = "Batch create orders", description = "Creates multiple orders in a single transaction")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<List<OrderDTO>> createOrdersBatch(
            @RequestBody @Valid List<@Valid OrderCreateDTO> dtos) {
        return new ResponseEntity<>(orderService.createOrdersBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Updates an existing order")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderDTO orderDTO) {
        return ResponseEntity.ok(orderService.updateOrder(id, orderDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update order", description = "Applies JSON Patch operations (RFC 6902) to an order")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    public ResponseEntity<OrderDTO> patchOrder(@PathVariable Long id,
                                               @RequestBody String patchDocument) {
        return ResponseEntity.ok(orderService.patchOrder(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Deletes an order by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
