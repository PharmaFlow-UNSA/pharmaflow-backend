package com.pharmaflow.pharmacyinventory.controller;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @Operation(summary = "Get all reservations", description = "Retrieves a list of all reservations")
    public ResponseEntity<List<ReservationDTO>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
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

    @PutMapping("/{id}")
    @Operation(summary = "Update reservation", description = "Updates an existing reservation")
    public ResponseEntity<ReservationDTO> updateReservation(@PathVariable Long id,
                                                            @Valid @RequestBody ReservationDTO reservationDTO) {
        return ResponseEntity.ok(reservationService.updateReservation(id, reservationDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reservation", description = "Deletes a reservation by ID")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
