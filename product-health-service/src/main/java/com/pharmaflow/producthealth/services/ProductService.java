package com.pharmaflow.producthealth.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.producthealth.dto.*;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.PatchOperationException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import com.pharmaflow.producthealth.specifications.ProductSpecs;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    // ── Osnovni CRUD ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProducts() {
        long start = System.currentTimeMillis();
        List<ProductDTO> result = productRepository.findAllActiveWithDetails().stream().map(this::toDTO).toList();
        log.info("getAllActiveProducts executed in {} ms, returned {} products",
                System.currentTimeMillis() - start, result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found."));
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameWithDetails(keyword).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId))
            throw new ResourceNotFoundException("Category with ID " + categoryId + " not found.");
        return productRepository.findByCategoryIdWithDetails(categoryId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySubstance(Long substanceId) {
        if (!substanceRepository.existsById(substanceId))
            throw new ResourceNotFoundException("Substance with ID " + substanceId + " not found.");
        return productRepository.findBySubstanceIdWithDetails(substanceId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProductDTO createProduct(ProductCreateDTO dto) {
        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Product with barcode '" + dto.getBarcode() + "' already exists.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with ID " + dto.getCategoryId() + " not found."));

        Product product = new Product();
        mapDtoToProduct(dto, product, category);
        product.setIsActive(true);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        Product saved = productRepository.save(product);
        log.info("Product created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductCreateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found."));

        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && !dto.getBarcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Product with barcode '" + dto.getBarcode() + "' already exists.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category with ID " + dto.getCategoryId() + " not found."));

        mapDtoToProduct(dto, product, category);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        Product saved = productRepository.save(product);
        log.info("Product updated with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found."));
        product.setIsActive(false);
        productRepository.save(product);
        log.info("Product deactivated with id: {}", id);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new ResourceNotFoundException("Product with ID " + id + " not found.");
        productRepository.deleteById(id);
        log.info("Product deleted with id: {}", id);
    }

    // ── PATCH - JSON Patch (RFC 6902) ─────────────────────────────────────

    @Transactional
    public ProductDTO patchProduct(Long id, String patchDocument) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + id + " not found."));

            JsonNode productJson = objectMapper.valueToTree(product);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(productJson);
            Product patchedProduct = objectMapper.treeToValue(patchedJson, Product.class);

            if (patchedProduct.getBarcode() != null
                    && !patchedProduct.getBarcode().equals(product.getBarcode())
                    && productRepository.existsByBarcode(patchedProduct.getBarcode())) {
                throw new DuplicateResourceException(
                        "Product with barcode '" + patchedProduct.getBarcode() + "' already exists.");
            }

            Product saved = productRepository.save(patchedProduct);
            log.info("Product patched with id: {}", saved.getId());
            return toDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    // ── Paginacija i sortiranje sa Specification ──────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsPageable(String name, String manufacturer,
                                                 String productType, Boolean requiresPrescription,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 Pageable pageable) {
        long start = System.currentTimeMillis();

        Specification<Product> spec = Specification
                .where(ProductSpecs.isActive())
                .and(ProductSpecs.nameContains(name))
                .and(ProductSpecs.manufacturerEquals(manufacturer))
                .and(ProductSpecs.requiresPrescription(requiresPrescription))
                .and(ProductSpecs.priceBetween(minPrice, maxPrice));

        if (productType != null && !productType.isBlank()) {
            try {
                spec = spec.and(ProductSpecs.productTypeEquals(Product.ProductType.valueOf(productType.toUpperCase())));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown product type: " + productType);
            }
        }

        Page<ProductDTO> result = productRepository.findAll(spec, pageable).map(this::toDTO);
        log.info("getProductsPageable executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - start, result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    // ── Custom upiti ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice.compareTo(maxPrice) > 0)
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price.");
        return productRepository.findByPriceRange(minPrice, maxPrice).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByTypeAndPrescription(String productType, Boolean requiresPrescription) {
        Product.ProductType type;
        try {
            type = Product.ProductType.valueOf(productType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Nepoznat tip: " + productType + ". Allowed values: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER");
        }
        return productRepository.findByTypeAndPrescription(type, requiresPrescription)
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getOtcProductsSortedByPrice() {
        return productRepository.findAllOtcOrderByPriceAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getProductCountByType() {
        List<Object[]> results = productRepository.countByProductType();
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Object[] row : results)
            stats.put(((Product.ProductType) row[0]).name(), (Long) row[1]);
        return stats;
    }

    // ── Batch unos ────────────────────────────────────────────────────────

    @Transactional
    public List<ProductDTO> createProductsBatch(ProductBatchDTO batchDTO) {
        long start = System.currentTimeMillis();
        List<Product> toSave = new ArrayList<>();
        for (ProductCreateDTO dto : batchDTO.getProducts()) {
            if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                    && productRepository.existsByBarcode(dto.getBarcode()))
                throw new DuplicateResourceException(
                        "Batch failed: product with barcode '" + dto.getBarcode() + "' already exists.");
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Batch failed: category with ID " + dto.getCategoryId() + " not found."));
            Product product = new Product();
            mapDtoToProduct(dto, product, category);
            product.setIsActive(true);
            product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
            toSave.add(product);
        }
        List<ProductDTO> result = productRepository.saveAll(toSave).stream().map(this::toDTO).toList();
        log.info("Batch created {} products in {} ms", result.size(), System.currentTimeMillis() - start);
        return result;
    }

    // ── Transakcija sa vise repo poziva ───────────────────────────────────

    @Transactional
    public Map<String, Object> reassignProductsToCategory(CategoryReassignDTO dto) {
        Category targetCategory = categoryRepository.findById(dto.getTargetCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target category with ID " + dto.getTargetCategoryId() + " not found."));

        List<Long> productIds = dto.getProductIds();
        for (Long productId : productIds) {
            if (!productRepository.existsById(productId))
                throw new ResourceNotFoundException(
                        "Transaction cancelled: product with ID " + productId + " not found.");
        }

        int updatedCount = productRepository.bulkUpdateCategory(productIds, dto.getTargetCategoryId());

        if (updatedCount != productIds.size())
            throw new IllegalStateException(
                    "Bulk update failed: expected " + productIds.size() +
                    " updates, executed " + updatedCount);

        log.info("Reassigned {} products to category {}", updatedCount, targetCategory.getName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successfully moved " + updatedCount + " proizvoda.");
        result.put("targetCategory", targetCategory.getName());
        result.put("updatedProductIds", productIds);
        result.put("updatedCount", updatedCount);
        return result;
    }

    // ── Mapping helperi ───────────────────────────────────────────────────

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
            dto.setSubstances(p.getSubstances().stream()
                    .map(s -> modelMapper.map(s, SubstanceDTO.class)).toList());
        return dto;
    }

    private void mapDtoToProduct(ProductCreateDTO dto, Product product, Category category) {
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
    }

    private List<Substance> resolveSubstances(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return ids.stream()
                .map(sid -> substanceRepository.findById(sid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Substance with ID " + sid + " not found.")))
                .toList();
    }
}
