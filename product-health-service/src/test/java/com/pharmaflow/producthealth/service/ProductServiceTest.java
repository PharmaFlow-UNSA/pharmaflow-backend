package com.pharmaflow.producthealth.service;

import com.pharmaflow.producthealth.dto.*;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import com.pharmaflow.producthealth.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubstanceRepository substanceRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private ProductService productService;

    private Product testProduct;
    private Category testCategory;
    private ProductCreateDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Analgetici");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Brufen 400mg");
        testProduct.setPrice(new BigDecimal("4.50"));
        testProduct.setManufacturer("Abbott");
        testProduct.setRequiresPrescription(false);
        testProduct.setProductType(Product.ProductType.MEDICATION);
        testProduct.setIsActive(true);
        testProduct.setCategory(testCategory);
        testProduct.setSubstances(List.of());

        validCreateDTO = new ProductCreateDTO();
        validCreateDTO.setName("Brufen 400mg");
        validCreateDTO.setPrice(new BigDecimal("4.50"));
        validCreateDTO.setManufacturer("Abbott");
        validCreateDTO.setRequiresPrescription(false);
        validCreateDTO.setProductType("MEDICATION");
        validCreateDTO.setCategoryId(1L);
    }

    // ── Osnovni CRUD testovi ────────────────────────────────────────────

    @Test
    void getAllActiveProducts_shouldReturnList() {
        when(productRepository.findAllActiveWithDetails()).thenReturn(List.of(testProduct));
        List<ProductDTO> result = productService.getAllActiveProducts();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productRepository).findAllActiveWithDetails();
    }

    @Test
    void getProductById_whenExists_shouldReturnDTO() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        ProductDTO result = productService.getProductById(1L);
        assertNotNull(result);
        assertEquals("Brufen 400mg", result.getName());
    }

    @Test
    void getProductById_whenNotExists_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    void createProduct_withValidData_shouldReturnDTO() {
        when(productRepository.existsByBarcode(any())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        ProductDTO result = productService.createProduct(validCreateDTO);
        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_withDuplicateBarcode_shouldThrowException() {
        validCreateDTO.setBarcode("12345678");
        when(productRepository.existsByBarcode("12345678")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(validCreateDTO));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_withNonexistentCategory_shouldThrowException() {
        when(productRepository.existsByBarcode(any())).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        validCreateDTO.setCategoryId(99L);
        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(validCreateDTO));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deactivateProduct_whenExists_shouldSetInactive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any())).thenReturn(testProduct);
        productService.deactivateProduct(1L);
        assertFalse(testProduct.getIsActive());
        verify(productRepository).save(testProduct);
    }

    @Test
    void deleteProduct_whenNotExists_shouldThrowException() {
        when(productRepository.existsById(999L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(999L));
        verify(productRepository, never()).deleteById(any());
    }

    // ── PATCH testovi ───────────────────────────────────────────────────

    @Test
    void patchProduct_shouldUpdateOnlyNonNullFields() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any())).thenReturn(testProduct);

        ProductPatchDTO patch = new ProductPatchDTO();
        patch.setPrice(new BigDecimal("9.99"));
        patch.setPackageSize("40 tableta");

        productService.patchProduct(1L, patch);

        assertEquals(new BigDecimal("9.99"), testProduct.getPrice());
        assertEquals("40 tableta", testProduct.getPackageSize());
        assertEquals("Abbott", testProduct.getManufacturer()); // nije se promijenio
        verify(productRepository).save(testProduct);
    }

    @Test
    void patchProduct_withEmptyPatch_shouldNotChangeAnything() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any())).thenReturn(testProduct);

        String originalName = testProduct.getName();
        productService.patchProduct(1L, new ProductPatchDTO());

        assertEquals(originalName, testProduct.getName());
    }

    @Test
    void patchProduct_whenNotExists_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> productService.patchProduct(999L, new ProductPatchDTO()));
    }

    // ── Paginacija testovi ──────────────────────────────────────────────

    @Test
    void getProductsPageable_shouldReturnPageDTO() {
        Page<Product> mockPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findAllActivePageable(any(Pageable.class))).thenReturn(mockPage);

        ProductPageDTO result = productService.getProductsPageable(0, 10, "name", "asc");

        assertNotNull(result);
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getProductsByCategoryPageable_whenCategoryNotExists_shouldThrow() {
        when(categoryRepository.existsById(999L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductsByCategoryPageable(999L, 0, 10, "name", "asc"));
    }

    // ── Custom upiti testovi ────────────────────────────────────────────

    @Test
    void getProductsByPriceRange_withValidRange_shouldReturnList() {
        when(productRepository.findByPriceRange(any(), any())).thenReturn(List.of(testProduct));
        List<ProductDTO> result = productService.getProductsByPriceRange(
                new BigDecimal("2.00"), new BigDecimal("10.00"));
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getProductsByPriceRange_whenMinGreaterThanMax_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getProductsByPriceRange(
                        new BigDecimal("10.00"), new BigDecimal("2.00")));
        verify(productRepository, never()).findByPriceRange(any(), any());
    }

    @Test
    void getProductsByTypeAndPrescription_withValidType_shouldReturnList() {
        when(productRepository.findByTypeAndPrescription(any(), any())).thenReturn(List.of(testProduct));
        List<ProductDTO> result = productService.getProductsByTypeAndPrescription("MEDICATION", false);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getProductsByTypeAndPrescription_withInvalidType_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getProductsByTypeAndPrescription("INVALID_TYPE", false));
        verify(productRepository, never()).findByTypeAndPrescription(any(), any());
    }

    // ── Batch testovi ────────────────────────────────────────────────────

    @Test
    void createProductsBatch_withValidData_shouldSaveAll() {
        when(productRepository.existsByBarcode(any())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.saveAll(any())).thenReturn(List.of(testProduct));

        ProductBatchDTO batchDTO = new ProductBatchDTO(List.of(validCreateDTO));
        List<ProductDTO> result = productService.createProductsBatch(batchDTO);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productRepository).saveAll(any());
    }

    @Test
    void createProductsBatch_withDuplicateBarcode_shouldRollbackAll() {
        validCreateDTO.setBarcode("12345678");
        when(productRepository.existsByBarcode("12345678")).thenReturn(true);

        ProductBatchDTO batchDTO = new ProductBatchDTO(List.of(validCreateDTO));
        assertThrows(DuplicateResourceException.class, () -> productService.createProductsBatch(batchDTO));
        verify(productRepository, never()).saveAll(any());
    }

    // ── Transakcija reassign testovi ─────────────────────────────────────

    @Test
    void reassignProductsToCategory_withValidData_shouldReturnResult() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);
        when(productRepository.bulkUpdateCategory(anyList(), anyLong())).thenReturn(2);

        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(1L, 2L), 2L);
        Map<String, Object> result = productService.reassignProductsToCategory(dto);

        assertNotNull(result);
        assertEquals(2, result.get("updatedCount"));
        verify(productRepository).bulkUpdateCategory(anyList(), anyLong());
    }

    @Test
    void reassignProductsToCategory_whenCategoryNotExists_shouldThrow() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(1L), 999L);
        assertThrows(ResourceNotFoundException.class,
                () -> productService.reassignProductsToCategory(dto));
        verify(productRepository, never()).bulkUpdateCategory(any(), any());
    }

    @Test
    void reassignProductsToCategory_whenProductNotExists_shouldThrow() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(99L)).thenReturn(false);

        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(1L, 99L), 2L);
        assertThrows(ResourceNotFoundException.class,
                () -> productService.reassignProductsToCategory(dto));
        verify(productRepository, never()).bulkUpdateCategory(any(), any());
    }
}
