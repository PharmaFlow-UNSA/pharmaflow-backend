package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
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
@RequestMapping("/api/reservations")
@Tag(name = "Reservation Management", description = "APIs for managing product reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(
            summary = "Get all reservations",
            description = "Supports pagination (?page=0&size=10&sort=id,asc) and optional filtering by status, userId, pharmacyId."
    )
    public ResponseEntity<Page<ReservationDTO>> getReservations(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long pharmacyId) {
        return ResponseEntity.ok(reservationService.findAll(status, userId, pharmacyId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reservation by ID", description = "Retrieves a specific reservation by its ID")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    @Operation(summary = "Get reservations by pharmacy ID", description = "Retrieves all reservations for a specific pharmacy")
    public ResponseEntity<List<ReservationDTO>> getReservationsByPharmacyId(@PathVariable Long pharmacyId) {
        return ResponseEntity.ok(reservationService.getReservationsByPharmacyId(pharmacyId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reservations by user ID", description = "Retrieves all reservations for a specific user")
    public ResponseEntity<List<ReservationDTO>> getReservationsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUserId(userId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get reservations by status", description = "Retrieves all reservations with a specific status")
    public ResponseEntity<List<ReservationDTO>> getReservationsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(reservationService.getReservationsByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new reservation", description = "Creates a new product reservation at a pharmacy")
    public ResponseEntity<ReservationDTO> createReservation(@Valid @RequestBody ReservationDTO reservationDTO) {
        return new ResponseEntity<>(reservationService.createReservation(reservationDTO), HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch create reservations", description = "Creates multiple reservations in a single transaction")
    public ResponseEntity<List<ReservationDTO>> createReservationsBatch(
            @RequestBody @Valid List<@Valid ReservationDTO> dtos) {
        return new ResponseEntity<>(reservationService.createReservationsBatch(dtos), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update reservation", description = "Updates an existing reservation")
    public ResponseEntity<ReservationDTO> updateReservation(@PathVariable Long id,
                                                            @Valid @RequestBody ReservationDTO reservationDTO) {
        return ResponseEntity.ok(reservationService.updateReservation(id, reservationDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update reservation", description = "Applies JSON Patch operations (RFC 6902) to a reservation")
    public ResponseEntity<ReservationDTO> patchReservation(@PathVariable Long id,
                                                           @RequestBody String patchDocument) {
        return ResponseEntity.ok(reservationService.patchReservation(id, patchDocument));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reservation", description = "Deletes a reservation by ID")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
