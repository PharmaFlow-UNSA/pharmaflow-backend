package com.pharmaflow.producthealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.producthealth.controllers.ProductController;
import com.pharmaflow.producthealth.dto.CategoryDTO;
import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.dto.ProductDTO;
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

import static org.hamcrest.Matchers.hasSize;
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
        CategoryDTO categoryDTO = new CategoryDTO(1L, "Analgetici", "Lijekovi za bol", null);

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

    @Test
    void getAllProducts_shouldReturn200AndList() throws Exception {
        when(productService.getAllActiveProducts()).thenReturn(List.of(testProductDTO));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Brufen 400mg"));

        verify(productService, times(1)).getAllActiveProducts();
    }

    @Test
    void getProductById_whenExists_shouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(testProductDTO);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Brufen 400mg"))
                .andExpect(jsonPath("$.productType").value("MEDICATION"));
    }

    @Test
    void getProductById_whenNotExists_shouldReturn404() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Proizvod sa ID 999 nije pronađen."));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("Proizvod sa ID 999 nije pronađen."));
    }

    @Test
    void createProduct_withValidData_shouldReturn201() throws Exception {
        when(productService.createProduct(any(ProductCreateDTO.class))).thenReturn(testProductDTO);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Brufen 400mg"));

        verify(productService, times(1)).createProduct(any(ProductCreateDTO.class));
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
        validCreateDTO.setProductType("INVALID_TYPE");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void updateProduct_withValidData_shouldReturn200() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductCreateDTO.class))).thenReturn(testProductDTO);

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

        verify(productService, times(1)).deactivateProduct(1L);
    }

    @Test
    void deleteProduct_whenNotExists_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Proizvod sa ID 999 nije pronađen."))
                .when(productService).deleteProduct(999L);

        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
}
