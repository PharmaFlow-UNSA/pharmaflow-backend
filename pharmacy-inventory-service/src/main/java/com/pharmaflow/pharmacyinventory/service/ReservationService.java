package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.ReservationDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.models.Reservation;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import com.pharmaflow.pharmacyinventory.repositories.ReservationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;

    public ReservationService(ReservationRepository reservationRepository,
                              PharmacyRepository pharmacyRepository,
                              ModelMapper modelMapper) {
        this.reservationRepository = reservationRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
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
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + reservationDTO.getPharmacyId()));

        Reservation reservation = new Reservation();
        reservation.setUserId(reservationDTO.getUserId());
        reservation.setProductId(reservationDTO.getProductId());
        reservation.setQuantity(reservationDTO.getQuantity());
        reservation.setStatus(reservationDTO.getStatus());
        reservation.setReservedAt(reservationDTO.getReservedAt());
        reservation.setExpiresAt(reservationDTO.getExpiresAt());
        reservation.setPharmacy(pharmacy);

        Reservation saved = reservationRepository.save(reservation);
        return convertToDTO(saved);
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
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
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
