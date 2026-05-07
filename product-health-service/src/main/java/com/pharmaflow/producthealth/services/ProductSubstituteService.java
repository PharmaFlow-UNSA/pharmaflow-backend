package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.ProductSubstituteDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.ProductSubstitute;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.ProductSubstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSubstituteService {

    private final ProductSubstituteRepository substituteRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductSubstituteDTO> getAllSubstitutes() {
        return substituteRepository.findAllWithProducts().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductSubstituteDTO getSubstituteById(Long id) {
        return toDTO(substituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Substitute with ID " + id + " not found.")));
    }

    @Transactional(readOnly = true)
    public List<ProductSubstituteDTO> getSubstitutesByProduct(Long productId) {
        if (!productRepository.existsById(productId))
            throw new ResourceNotFoundException("Product with ID " + productId + " not found.");
        return substituteRepository.findByOriginalProductIdWithDetails(productId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProductSubstituteDTO createSubstitute(ProductSubstituteDTO dto) {
        if (dto.getOriginalProductId().equals(dto.getSubstituteProductId()))
            throw new IllegalArgumentException("A product cannot be a substitute for itself.");
        if (substituteRepository.existsByOriginalProductIdAndSubstituteProductId(
                dto.getOriginalProductId(), dto.getSubstituteProductId()))
            throw new DuplicateResourceException("This product substitute relationship already exists.");
        Product original = productRepository.findById(dto.getOriginalProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + dto.getOriginalProductId() + " not found."));
        Product substitute = productRepository.findById(dto.getSubstituteProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + dto.getSubstituteProductId() + " not found."));
        ProductSubstitute entity = new ProductSubstitute();
        entity.setOriginalProduct(original);
        entity.setSubstituteProduct(substitute);
        entity.setSubstituteType(ProductSubstitute.SubstituteType.valueOf(dto.getSubstituteType()));
        entity.setIsTherapeuticEquivalent(dto.getIsTherapeuticEquivalent());
        entity.setNote(dto.getNote());
        return toDTO(substituteRepository.save(entity));
    }

    @Transactional
    public ProductSubstituteDTO updateSubstitute(Long id, ProductSubstituteDTO dto) {
        ProductSubstitute entity = substituteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Substitute with ID " + id + " not found."));
        entity.setSubstituteType(ProductSubstitute.SubstituteType.valueOf(dto.getSubstituteType()));
        entity.setIsTherapeuticEquivalent(dto.getIsTherapeuticEquivalent());
        entity.setNote(dto.getNote());
        return toDTO(substituteRepository.save(entity));
    }

    @Transactional
    public void deleteSubstitute(Long id) {
        if (!substituteRepository.existsById(id))
            throw new ResourceNotFoundException("Substitute with ID " + id + " not found.");
        substituteRepository.deleteById(id);
    }

    private ProductSubstituteDTO toDTO(ProductSubstitute s) {
        ProductSubstituteDTO dto = new ProductSubstituteDTO();
        dto.setId(s.getId());
        dto.setOriginalProductId(s.getOriginalProduct().getId());
        dto.setSubstituteProductId(s.getSubstituteProduct().getId());
        dto.setOriginalProductName(s.getOriginalProduct().getName());
        dto.setSubstituteProductName(s.getSubstituteProduct().getName());
        dto.setSubstituteType(s.getSubstituteType().name());
        dto.setIsTherapeuticEquivalent(s.getIsTherapeuticEquivalent());
        dto.setNote(s.getNote());
        return dto;
    }
}
