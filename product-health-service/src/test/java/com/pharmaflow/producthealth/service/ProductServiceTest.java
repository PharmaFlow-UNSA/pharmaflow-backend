package com.pharmaflow.producthealth.service;

import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.dto.ProductDTO;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void getAllActiveProducts_shouldReturnList() {
        when(productRepository.findAllActiveWithDetails()).thenReturn(List.of(testProduct));

        List<ProductDTO> result = productService.getAllActiveProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productRepository, times(1)).findAllActiveWithDetails();
    }

    @Test
    void getProductById_whenExists_shouldReturnDTO() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals("Brufen 400mg", result.getName());
        assertEquals("MEDICATION", result.getProductType());
    }

    @Test
    void getProductById_whenNotExists_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void createProduct_withValidData_shouldReturnDTO() {
        when(productRepository.existsByBarcode(any())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.createProduct(validCreateDTO);

        assertNotNull(result);
        assertEquals("Brufen 400mg", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
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
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.deactivateProduct(1L);

        assertFalse(testProduct.getIsActive());
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    void deactivateProduct_whenNotExists_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.deactivateProduct(999L));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_whenExists_shouldDelete() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProduct_whenNotExists_shouldThrowException() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(999L));
        verify(productRepository, never()).deleteById(any());
    }
}
