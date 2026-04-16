package com.pharmaflow.pharmacyinventory.service;

import com.pharmaflow.pharmacyinventory.dto.DeliveryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.models.Delivery;
import com.pharmaflow.pharmacyinventory.models.Pharmacy;
import com.pharmaflow.pharmacyinventory.repositories.DeliveryRepository;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ModelMapper modelMapper;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           PharmacyRepository pharmacyRepository,
                           ModelMapper modelMapper) {
        this.deliveryRepository = deliveryRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));
        return convertToDTO(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByPharmacyId(Long pharmacyId) {
        return deliveryRepository.findByPharmacyId(pharmacyId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getDeliveriesByStatus(String status) {
        return deliveryRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public DeliveryDTO createDelivery(DeliveryDTO deliveryDTO) {
        Pharmacy pharmacy = pharmacyRepository.findById(deliveryDTO.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found with id: " + deliveryDTO.getPharmacyId()));

        Delivery delivery = new Delivery();
        delivery.setOrderId(deliveryDTO.getOrderId());
        delivery.setDeliveryAddress(deliveryDTO.getDeliveryAddress());
        delivery.setStatus(deliveryDTO.getStatus());
        delivery.setEstimatedDelivery(deliveryDTO.getEstimatedDelivery());
        delivery.setActualDelivery(deliveryDTO.getActualDelivery());
        delivery.setPharmacy(pharmacy);

        Delivery saved = deliveryRepository.save(delivery);
        return convertToDTO(saved);
    }

    @Transactional
    public DeliveryDTO updateDelivery(Long id, DeliveryDTO deliveryDTO) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        delivery.setOrderId(deliveryDTO.getOrderId());
        delivery.setDeliveryAddress(deliveryDTO.getDeliveryAddress());
        delivery.setStatus(deliveryDTO.getStatus());
        delivery.setEstimatedDelivery(deliveryDTO.getEstimatedDelivery());
        delivery.setActualDelivery(deliveryDTO.getActualDelivery());

        Delivery updated = deliveryRepository.save(delivery);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteDelivery(Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Delivery not found with id: " + id);
        }
        deliveryRepository.deleteById(id);
    }

    private DeliveryDTO convertToDTO(Delivery delivery) {
        DeliveryDTO dto = new DeliveryDTO();
        dto.setId(delivery.getId());
        dto.setOrderId(delivery.getOrderId());
        dto.setDeliveryAddress(delivery.getDeliveryAddress());
        dto.setStatus(delivery.getStatus());
        dto.setEstimatedDelivery(delivery.getEstimatedDelivery());
        dto.setActualDelivery(delivery.getActualDelivery());
        dto.setPharmacyId(delivery.getPharmacy().getId());
        return dto;
    }
}
