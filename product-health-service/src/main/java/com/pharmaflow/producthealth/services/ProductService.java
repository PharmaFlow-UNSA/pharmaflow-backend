package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;
    private final ModelMapper modelMapper;

    // ── Osnovne CRUD metode ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllActiveProducts() {
        return productRepository.findAllActiveWithDetails().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronadjen."));
        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameWithDetails(keyword).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId))
            throw new ResourceNotFoundException("Kategorija sa ID " + categoryId + " nije pronadjena.");
        return productRepository.findByCategoryIdWithDetails(categoryId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsBySubstance(Long substanceId) {
        if (!substanceRepository.existsById(substanceId))
            throw new ResourceNotFoundException("Supstanca sa ID " + substanceId + " nije pronadjena.");
        return productRepository.findBySubstanceIdWithDetails(substanceId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public ProductDTO createProduct(ProductCreateDTO dto) {
        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Proizvod sa barkodom '" + dto.getBarcode() + "' vec postoji.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kategorija sa ID " + dto.getCategoryId() + " nije pronadjena."));

        Product product = new Product();
        mapDtoToProduct(dto, product, category);
        product.setIsActive(true);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        return toDTO(productRepository.save(product));
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductCreateDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronadjen."));

        if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                && !dto.getBarcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(dto.getBarcode()))
            throw new DuplicateResourceException("Proizvod sa barkodom '" + dto.getBarcode() + "' vec postoji.");

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kategorija sa ID " + dto.getCategoryId() + " nije pronadjena."));

        mapDtoToProduct(dto, product, category);
        product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
        return toDTO(productRepository.save(product));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronadjen."));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id))
            throw new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronadjen.");
        productRepository.deleteById(id);
    }

    // ── PATCH: parcijalno azuriranje ───────────────────────────────────────

    @Transactional
    public ProductDTO patchProduct(Long id, ProductPatchDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proizvod sa ID " + id + " nije pronadjen."));

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getBrandName() != null) product.setBrandName(dto.getBrandName());
        if (dto.getPackageSize() != null) product.setPackageSize(dto.getPackageSize());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());
        if (dto.getRequiresPrescription() != null) product.setRequiresPrescription(dto.getRequiresPrescription());
        if (dto.getProductType() != null) product.setProductType(Product.ProductType.valueOf(dto.getProductType()));

        return toDTO(productRepository.save(product));
    }

    // ── Paginacija i sortiranje ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductPageDTO getProductsPageable(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAllActivePageable(pageable);
        return new ProductPageDTO(
                productPage.getContent().stream().map(this::toDTO).toList(),
                productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages(), productPage.isLast());
    }

    @Transactional(readOnly = true)
    public ProductPageDTO getProductsByCategoryPageable(Long categoryId, int page, int size,
                                                         String sortBy, String direction) {
        if (!categoryRepository.existsById(categoryId))
            throw new ResourceNotFoundException("Kategorija sa ID " + categoryId + " nije pronadjena.");
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findByCategoryIdPageable(categoryId, pageable);
        return new ProductPageDTO(
                productPage.getContent().stream().map(this::toDTO).toList(),
                productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages(), productPage.isLast());
    }

    // ── Custom upiti ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice.compareTo(maxPrice) > 0)
            throw new IllegalArgumentException("Minimalna cijena ne smije biti veca od maksimalne.");
        return productRepository.findByPriceRange(minPrice, maxPrice).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByTypeAndPrescription(String productType, Boolean requiresPrescription) {
        Product.ProductType type;
        try {
            type = Product.ProductType.valueOf(productType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Nepoznat tip proizvoda: " + productType +
                    ". Dozvoljeni: MEDICATION, SUPPLEMENT, COSMETIC, MEDICAL_DEVICE, OTHER");
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
        List<Product> toSave = new ArrayList<>();
        for (ProductCreateDTO dto : batchDTO.getProducts()) {
            if (dto.getBarcode() != null && !dto.getBarcode().isBlank()
                    && productRepository.existsByBarcode(dto.getBarcode()))
                throw new DuplicateResourceException(
                        "Batch otkazan: proizvod sa barkodom '" + dto.getBarcode() + "' vec postoji.");

            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Batch otkazan: kategorija sa ID " + dto.getCategoryId() + " nije pronadjena."));

            Product product = new Product();
            mapDtoToProduct(dto, product, category);
            product.setIsActive(true);
            product.setSubstances(resolveSubstances(dto.getSubstanceIds()));
            toSave.add(product);
        }
        return productRepository.saveAll(toSave).stream().map(this::toDTO).toList();
    }

    // ── Transakcija sa vise repository poziva ─────────────────────────────

    @Transactional
    public Map<String, Object> reassignProductsToCategory(CategoryReassignDTO dto) {
        // Korak 1: provjeri kategoriju
        Category targetCategory = categoryRepository.findById(dto.getTargetCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ciljna kategorija sa ID " + dto.getTargetCategoryId() + " nije pronadjena."));

        // Korak 2: provjeri sve proizvode
        List<Long> productIds = dto.getProductIds();
        for (Long productId : productIds) {
            if (!productRepository.existsById(productId))
                throw new ResourceNotFoundException(
                        "Transakcija otkazana: proizvod sa ID " + productId + " nije pronadjen.");
        }

        // Korak 3: bulk update
        int updatedCount = productRepository.bulkUpdateCategory(productIds, dto.getTargetCategoryId());

        // Korak 4: provjeri rezultat
        if (updatedCount != productIds.size())
            throw new IllegalStateException(
                    "Bulk update nije uspio: ocekivano " + productIds.size() +
                    " azuriranja, izvrseno " + updatedCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Uspjesno premjesteno " + updatedCount + " proizvoda.");
        result.put("targetCategory", targetCategory.getName());
        result.put("updatedProductIds", productIds);
        result.put("updatedCount", updatedCount);
        return result;
    }

    // ── Mapping helperi ────────────────────────────────────────────────────

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
                                "Supstanca sa ID " + sid + " nije pronadjena.")))
                .toList();
    }
}
