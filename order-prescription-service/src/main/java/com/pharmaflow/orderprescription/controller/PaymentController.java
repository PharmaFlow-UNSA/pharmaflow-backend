package com.pharmaflow.orderprescription.controller;

import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "APIs for managing payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(
            summary = "Get all payments",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by status and method."
    )
    public ResponseEntity<Page<PaymentDTO>> getPayments(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        return ResponseEntity.ok(paymentService.findAll(status, method, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a specific payment by its ID")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new payment", description = "Creates a new payment")
    public ResponseEntity<PaymentDTO> createPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(paymentDTO));
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create payments", description = "Creates multiple payments in a single transaction")
    public ResponseEntity<List<PaymentDTO>> createPaymentsBatch(
            @RequestBody @Valid List<@Valid PaymentDTO> dtos) {
        return new ResponseEntity<>(paymentService.createPaymentsBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment", description = "Updates an existing payment")
    public ResponseEntity<PaymentDTO> updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentDTO paymentDTO) {
        return ResponseEntity.ok(paymentService.updatePayment(id, paymentDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update payment", description = "Applies JSON Patch operations (RFC 6902) to a payment")
    public ResponseEntity<PaymentDTO> patchPayment(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(paymentService.patchPayment(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment", description = "Deletes a payment by ID")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
