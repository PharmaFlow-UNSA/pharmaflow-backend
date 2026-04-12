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
                .orElseThrow(() -> new ResourceNotFoundException("Zamjena sa ID " + id + " nije pronađena.")));
    }

    @Transactional(readOnly = true)
    public List<ProductSubstituteDTO> getSubstitutesByProduct(Long productId) {
        if (!productRepository.existsById(productId))
            throw new ResourceNotFoundException("Proizvod sa ID " + productId + " nije pronađen.");
        return substituteRepository.findByOriginalProductIdWithDetails(productId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProductSubstituteDTO createSubstitute(ProductSubstituteDTO dto) {
        if (dto.getOriginalProductId().equals(dto.getSubstituteProductId()))
            throw new IllegalArgumentException("Proizvod ne može biti zamjena za samog sebe.");
        if (substituteRepository.existsByOriginalProductIdAndSubstituteProductId(
                dto.getOriginalProductId(), dto.getSubstituteProductId()))
            throw new DuplicateResourceException("Ova zamjena između proizvoda već postoji.");
        Product original = productRepository.findById(dto.getOriginalProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + dto.getOriginalProductId() + " nije pronađen."));
        Product substitute = productRepository.findById(dto.getSubstituteProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + dto.getSubstituteProductId() + " nije pronađen."));
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
                .orElseThrow(() -> new ResourceNotFoundException("Zamjena sa ID " + id + " nije pronađena."));
        entity.setSubstituteType(ProductSubstitute.SubstituteType.valueOf(dto.getSubstituteType()));
        entity.setIsTherapeuticEquivalent(dto.getIsTherapeuticEquivalent());
        entity.setNote(dto.getNote());
        return toDTO(substituteRepository.save(entity));
    }

    @Transactional
    public void deleteSubstitute(Long id) {
        if (!substituteRepository.existsById(id))
            throw new ResourceNotFoundException("Zamjena sa ID " + id + " nije pronađena.");
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
