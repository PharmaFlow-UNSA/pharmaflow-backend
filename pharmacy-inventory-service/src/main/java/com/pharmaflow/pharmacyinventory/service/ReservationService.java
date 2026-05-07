package com.pharmaflow.pharmacyinventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.models.Reservation;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.repositories.ReservationRepository;
import com.pharmaflow.pharmacyinventory.specifications.ReservationSpecs;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ReservationService(ReservationRepository reservationRepository,
                              PharmacyRepository pharmacyRepository,
                              ModelMapper modelMapper,
                              ObjectMapper objectMapper,
                              Validator validator) {
        this.reservationRepository = reservationRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<ReservationDTO> findAll(String status, Long userId, Long pharmacyId, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Reservation> spec = Specification
                .where(ReservationSpecs.hasStatus(status))
                .and(ReservationSpecs.hasUserId(userId))
                .and(ReservationSpecs.hasPharmacyId(pharmacyId));

        Page<ReservationDTO> result = reservationRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Reservation findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public ReservationDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        return convertToDTO(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByPharmacyId(Long pharmacyId) {
        return reservationRepository.findByPharmacyId(pharmacyId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByStatus(String status) {
        return reservationRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public ReservationDTO createReservation(ReservationDTO reservationDTO) {
        Pharmacy pharmacy = pharmacyRepository.findById(reservationDTO.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pharmacy not found with id: " + reservationDTO.getPharmacyId()));

        Reservation reservation = new Reservation();
        reservation.setUserId(reservationDTO.getUserId());
        reservation.setProductId(reservationDTO.getProductId());
        reservation.setQuantity(reservationDTO.getQuantity());
        reservation.setStatus(reservationDTO.getStatus());
        reservation.setReservedAt(reservationDTO.getReservedAt());
        reservation.setExpiresAt(reservationDTO.getExpiresAt());
        reservation.setPharmacy(pharmacy);

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<ReservationDTO> createReservationsBatch(List<ReservationDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Reservation> reservations = new ArrayList<>();

        for (ReservationDTO dto : dtos) {
            Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pharmacy not found with id: " + dto.getPharmacyId()));

            Reservation reservation = new Reservation();
            reservation.setUserId(dto.getUserId());
            reservation.setProductId(dto.getProductId());
            reservation.setQuantity(dto.getQuantity());
            reservation.setStatus(dto.getStatus());
            reservation.setReservedAt(dto.getReservedAt());
            reservation.setExpiresAt(dto.getExpiresAt());
            reservation.setPharmacy(pharmacy);

            reservations.add(reservation);
        }

        List<Reservation> saved = reservationRepository.saveAll(reservations);
        log.info("Batch created {} reservations in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public ReservationDTO updateReservation(Long id, ReservationDTO reservationDTO) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        reservation.setUserId(reservationDTO.getUserId());
        reservation.setProductId(reservationDTO.getProductId());
        reservation.setQuantity(reservationDTO.getQuantity());
        reservation.setStatus(reservationDTO.getStatus());
        reservation.setReservedAt(reservationDTO.getReservedAt());
        reservation.setExpiresAt(reservationDTO.getExpiresAt());

        Reservation updated = reservationRepository.save(reservation);
        log.info("Reservation updated with id: {}", updated.getId());
        return convertToDTO(updated);
    }

    @Transactional
    public ReservationDTO patchReservation(Long id, String patchDocument) {
        try {
            Reservation reservation = reservationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

            ReservationDTO currentDTO = convertToDTO(reservation);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            ReservationDTO patchedDTO = objectMapper.treeToValue(patchedJson, ReservationDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<ReservationDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            reservation.setUserId(patchedDTO.getUserId());
            reservation.setProductId(patchedDTO.getProductId());
            reservation.setQuantity(patchedDTO.getQuantity());
            reservation.setStatus(patchedDTO.getStatus());
            reservation.setReservedAt(patchedDTO.getReservedAt());
            reservation.setExpiresAt(patchedDTO.getExpiresAt());

            if (patchedDTO.getPharmacyId() != null
                    && !patchedDTO.getPharmacyId().equals(reservation.getPharmacy().getId())) {
                Pharmacy pharmacy = pharmacyRepository.findById(patchedDTO.getPharmacyId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pharmacy not found with id: " + patchedDTO.getPharmacyId()));
                reservation.setPharmacy(pharmacy);
            }

            Reservation saved = reservationRepository.save(reservation);
            log.info("Reservation patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
        log.info("Reservation deleted with id: {}", id);
    }

    private ReservationDTO convertToDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUserId());
        dto.setProductId(reservation.getProductId());
        dto.setQuantity(reservation.getQuantity());
        dto.setStatus(reservation.getStatus());
        dto.setReservedAt(reservation.getReservedAt());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setPharmacyId(reservation.getPharmacy().getId());
        return dto;
    }
}
