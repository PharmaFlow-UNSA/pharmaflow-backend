package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.CategoryDTO;
import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.dto.ProductDTO;
import com.pharmaflow.producthealth.dto.SubstanceDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProducts() {
        return productRepository.findAllActiveWithDetails().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronađen."));
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameWithDetails(keyword).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId))
            throw new ResourceNotFoundException("Kategorija sa ID " + categoryId + " nije pronađena.");
        return productRepository.findByCategoryIdWithDetails(categoryId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySubstance(Long substanceId) {
        if (!substanceRepository.existsById(substanceId))
            throw new ResourceNotFoundException("Supstanca sa ID " + substanceId + " nije pronađena.");
        return productRepository.findBySubstanceIdWithDetails(substanceId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProductDTO createProduct(ProductCreateDTO dto) {
        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Proizvod sa barkodom '" + dto.getBarcode() + "' već postoji.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija sa ID " + dto.getCategoryId() + " nije pronađena."));

        Product product = new Product();
        product.setName(dto.getName());
        product.setBarcode(dto.getBarcode());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setBrandName(dto.getBrandName());
        product.setManufacturer(dto.getManufacturer());
        product.setRequiresPrescription(dto.getRequiresPrescription());
        product.setProductType(Product.ProductType.valueOf(dto.getProductType()));
        product.setImageUrl(dto.getImageUrl());
        product.setPackageSize(dto.getPackageSize());
        product.setIsActive(true);
        product.setCategory(category);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        return toDTO(productRepository.save(product));
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductCreateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronađen."));

        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && !dto.getBarcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Proizvod sa barkodom '" + dto.getBarcode() + "' već postoji.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija sa ID " + dto.getCategoryId() + " nije pronađena."));

        product.setName(dto.getName());
        product.setBarcode(dto.getBarcode());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setBrandName(dto.getBrandName());
        product.setManufacturer(dto.getManufacturer());
        product.setRequiresPrescription(dto.getRequiresPrescription());
        product.setProductType(Product.ProductType.valueOf(dto.getProductType()));
        product.setImageUrl(dto.getImageUrl());
        product.setPackageSize(dto.getPackageSize());
        product.setCategory(category);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        return toDTO(productRepository.save(product));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronađen."));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronađen.");
        productRepository.deleteById(id);
    }

    ProductDTO toDTO(Product p) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setBarcode(p.getBarcode());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setBrandName(p.getBrandName());
        dto.setManufacturer(p.getManufacturer());
        dto.setRequiresPrescription(p.getRequiresPrescription());
        dto.setProductType(p.getProductType() != null ? p.getProductType().name() : null);
        dto.setImageUrl(p.getImageUrl());
        dto.setIsActive(p.getIsActive());
        dto.setPackageSize(p.getPackageSize());
        if (p.getCategory() != null)
            dto.setCategory(modelMapper.map(p.getCategory(), CategoryDTO.class));
        if (p.getSubstances() != null)
            dto.setSubstances(p.getSubstances().stream().map(s -> modelMapper.map(s, SubstanceDTO.class)).toList());
        return dto;
    }

    private List<Substance> resolveSubstances(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return ids.stream()
                .map(sid -> substanceRepository.findById(sid)
                        .orElseThrow(() -> new ResourceNotFoundException("Supstanca sa ID " + sid + " nije pronađena.")))
                .toList();
    }
}
