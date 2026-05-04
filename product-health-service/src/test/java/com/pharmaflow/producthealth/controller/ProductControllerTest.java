package com.pharmaflow.producthealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.producthealth.controllers.ProductController;
import com.pharmaflow.producthealth.dto.*;
import com.pharmaflow.producthealth.exception.GlobalExceptionHandler;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ProductService productService;

    private ProductDTO testProductDTO;
    private ProductCreateDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        CategoryDTO categoryDTO = new CategoryDTO(1L, "Analgetici", "Opis", null);

        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setName("Brufen 400mg");
        testProductDTO.setPrice(new BigDecimal("4.50"));
        testProductDTO.setManufacturer("Abbott");
        testProductDTO.setRequiresPrescription(false);
        testProductDTO.setProductType("MEDICATION");
        testProductDTO.setIsActive(true);
        testProductDTO.setCategory(categoryDTO);

        validCreateDTO = new ProductCreateDTO();
        validCreateDTO.setName("Brufen 400mg");
        validCreateDTO.setPrice(new BigDecimal("4.50"));
        validCreateDTO.setManufacturer("Abbott");
        validCreateDTO.setRequiresPrescription(false);
        validCreateDTO.setProductType("MEDICATION");
        validCreateDTO.setCategoryId(1L);
    }

    // ── Osnovni CRUD testovi ───────────────────────────────────────────────

    @Test
    void getAllProducts_shouldReturn200AndList() throws Exception {
        when(productService.getAllActiveProducts()).thenReturn(List.of(testProductDTO));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Brufen 400mg"));
        verify(productService).getAllActiveProducts();
    }

    @Test
    void getProductById_whenExists_shouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testProductDTO);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productType").value("MEDICATION"));
    }

    @Test
    void getProductById_whenNotExists_shouldReturn404() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Proizvod sa ID 999 nije pronadjen."));
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createProduct_withValidData_shouldReturn201() throws Exception {
        when(productService.createProduct(any(ProductCreateDTO.class))).thenReturn(testProductDTO);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Brufen 400mg"));
        verify(productService).createProduct(any());
    }

    @Test
    void createProduct_withMissingName_shouldReturn400() throws Exception {
        ProductCreateDTO invalid = new ProductCreateDTO();
        invalid.setPrice(new BigDecimal("4.50"));
        invalid.setManufacturer("Abbott");
        invalid.setRequiresPrescription(false);
        invalid.setProductType("MEDICATION");
        invalid.setCategoryId(1L);
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.errors.name").exists());
        verify(productService, never()).createProduct(any());
    }

    @Test
    void createProduct_withInvalidProductType_shouldReturn400() throws Exception {
        validCreateDTO.setProductType("INVALID");
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.productType").exists());
    }

    @Test
    void updateProduct_withValidData_shouldReturn200() throws Exception {
        when(productService.updateProduct(eq(1L), any())).thenReturn(testProductDTO);
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deactivateProduct_shouldReturn204() throws Exception {
        doNothing().when(productService).deactivateProduct(1L);
        mockMvc.perform(patch("/api/products/1/deactivate"))
                .andExpect(status().isNoContent());
        verify(productService).deactivateProduct(1L);
    }

    @Test
    void deleteProduct_whenNotExists_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Proizvod sa ID 999 nije pronadjen."))
                .when(productService).deleteProduct(999L);
        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH testovi ──────────────────────────────────────────────────────

    @Test
    void patchProduct_withValidData_shouldReturn200() throws Exception {
        ProductPatchDTO patchDTO = new ProductPatchDTO();
        patchDTO.setPrice(new BigDecimal("5.99"));
        patchDTO.setPackageSize("40 tableta");

        when(productService.patchProduct(eq(1L), any(ProductPatchDTO.class))).thenReturn(testProductDTO);

        mockMvc.perform(patch("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(productService).patchProduct(eq(1L), any());
    }

    @Test
    void patchProduct_withInvalidPrice_shouldReturn400() throws Exception {
        ProductPatchDTO patchDTO = new ProductPatchDTO();
        patchDTO.setPrice(new BigDecimal("-1.00"));
        mockMvc.perform(patch("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void patchProduct_whenNotExists_shouldReturn404() throws Exception {
        ProductPatchDTO patchDTO = new ProductPatchDTO();
        patchDTO.setName("Novo ime");
        when(productService.patchProduct(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Proizvod sa ID 999 nije pronadjen."));
        mockMvc.perform(patch("/api/products/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDTO)))
                .andExpect(status().isNotFound());
    }

    // ── Paginacija testovi ─────────────────────────────────────────────────

    @Test
    void getProductsPageable_shouldReturn200WithPageData() throws Exception {
        ProductPageDTO pageDTO = new ProductPageDTO(List.of(testProductDTO), 0, 10, 1L, 1, true);
        when(productService.getProductsPageable(0, 10, "name", "asc")).thenReturn(pageDTO);

        mockMvc.perform(get("/api/products/page")
                        .param("page", "0").param("size", "10")
                        .param("sortBy", "name").param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getProductsByCategoryPageable_whenCategoryNotExists_shouldReturn404() throws Exception {
        when(productService.getProductsByCategoryPageable(eq(999L), anyInt(), anyInt(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Kategorija sa ID 999 nije pronadjena."));
        mockMvc.perform(get("/api/products/category/999/page"))
                .andExpect(status().isNotFound());
    }

    // ── Custom upiti testovi ───────────────────────────────────────────────

    @Test
    void getByPriceRange_withValidRange_shouldReturn200() throws Exception {
        when(productService.getProductsByPriceRange(any(), any())).thenReturn(List.of(testProductDTO));
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "2.00").param("maxPrice", "10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByPriceRange_withNegativeMin_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/products/price-range")
                        .param("minPrice", "-1.00").param("maxPrice", "10.00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByTypeAndPrescription_shouldReturn200() throws Exception {
        when(productService.getProductsByTypeAndPrescription("MEDICATION", false))
                .thenReturn(List.of(testProductDTO));
        mockMvc.perform(get("/api/products/filter")
                        .param("type", "MEDICATION").param("requiresPrescription", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getOtcProducts_shouldReturn200() throws Exception {
        when(productService.getOtcProductsSortedByPrice()).thenReturn(List.of(testProductDTO));
        mockMvc.perform(get("/api/products/otc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getCountByType_shouldReturn200WithStats() throws Exception {
        when(productService.getProductCountByType()).thenReturn(Map.of("MEDICATION", 5L, "SUPPLEMENT", 2L));
        mockMvc.perform(get("/api/products/stats/count-by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MEDICATION").value(5));
    }

    // ── Batch testovi ──────────────────────────────────────────────────────

    @Test
    void createProductsBatch_withValidData_shouldReturn201() throws Exception {
        ProductBatchDTO batchDTO = new ProductBatchDTO(List.of(validCreateDTO));
        when(productService.createProductsBatch(any())).thenReturn(List.of(testProductDTO));
        mockMvc.perform(post("/api/products/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createProductsBatch_withEmptyList_shouldReturn400() throws Exception {
        ProductBatchDTO batchDTO = new ProductBatchDTO(List.of());
        mockMvc.perform(post("/api/products/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.products").exists());
        verify(productService, never()).createProductsBatch(any());
    }

    // ── Transakcija reassign testovi ───────────────────────────────────────

    @Test
    void reassignProductsToCategory_withValidData_shouldReturn200() throws Exception {
        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(1L, 2L), 3L);
        Map<String, Object> result = Map.of("message", "Uspjesno premjesteno 2 proizvoda.", "updatedCount", 2);
        when(productService.reassignProductsToCategory(any())).thenReturn(result);

        mockMvc.perform(patch("/api/products/reassign-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2));
    }

    @Test
    void reassignProductsToCategory_withEmptyProductList_shouldReturn400() throws Exception {
        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(), 1L);
        mockMvc.perform(patch("/api/products/reassign-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.productIds").exists());
        verify(productService, never()).reassignProductsToCategory(any());
    }

    @Test
    void reassignProductsToCategory_whenCategoryNotFound_shouldReturn404() throws Exception {
        CategoryReassignDTO dto = new CategoryReassignDTO(List.of(1L), 999L);
        when(productService.reassignProductsToCategory(any()))
                .thenThrow(new ResourceNotFoundException("Ciljna kategorija sa ID 999 nije pronadjena."));
        mockMvc.perform(patch("/api/products/reassign-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
}
