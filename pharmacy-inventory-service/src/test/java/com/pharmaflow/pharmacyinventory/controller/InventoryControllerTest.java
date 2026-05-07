package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.PatchOperationException;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private InventoryService inventoryService;

    private InventoryDTO testInventoryDTO;

    @BeforeEach
    void setUp() {
        testInventoryDTO = new InventoryDTO();
        testInventoryDTO.setId(1L);
        testInventoryDTO.setProductId(100L);
        testInventoryDTO.setQuantity(50);
        testInventoryDTO.setReorderLevel(10);
        testInventoryDTO.setLastRestocked(null);
        testInventoryDTO.setPharmacyId(1L);
    }

    @Test
    void getInventory_ShouldReturn200AndPagedResult() throws Exception {
        Page<InventoryDTO> page = new PageImpl<>(List.of(testInventoryDTO));
        when(inventoryService.findAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productId").value(100));

        verify(inventoryService, times(1)).findAll(any(), any(), any(), any());
    }

    @Test
    void getInventoryById_WhenExists_ShouldReturn200() throws Exception {
        when(inventoryService.getInventoryById(1L)).thenReturn(testInventoryDTO);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getInventoryById_WhenNotExists_ShouldReturn404() throws Exception {
        when(inventoryService.getInventoryById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory not found with id: 999"));

        mockMvc.perform(get("/api/inventory/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInventoryByPharmacyId_ShouldReturn200AndList() throws Exception {
        when(inventoryService.getInventoryByPharmacyId(1L)).thenReturn(List.of(testInventoryDTO));

        mockMvc.perform(get("/api/inventory/pharmacy/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getInventoryByProductId_ShouldReturn200AndList() throws Exception {
        when(inventoryService.getInventoryByProductId(100L)).thenReturn(List.of(testInventoryDTO));

        mockMvc.perform(get("/api/inventory/product/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createInventory_WithValidData_ShouldReturn201() throws Exception {
        when(inventoryService.createInventory(any(InventoryDTO.class))).thenReturn(testInventoryDTO);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testInventoryDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createInventory_WithInvalidData_ShouldReturn400() throws Exception {
        InventoryDTO invalidDTO = new InventoryDTO();
        invalidDTO.setProductId(null);
        invalidDTO.setQuantity(50);
        invalidDTO.setPharmacyId(1L);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void createInventoriesBatch_ShouldReturn201() throws Exception {
        when(inventoryService.createInventoriesBatch(anyList())).thenReturn(List.of(testInventoryDTO));

        mockMvc.perform(post("/api/inventory/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testInventoryDTO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateInventory_WithValidData_ShouldReturn200() throws Exception {
        when(inventoryService.updateInventory(eq(1L), any(InventoryDTO.class))).thenReturn(testInventoryDTO);

        mockMvc.perform(put("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testInventoryDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchInventory_WithValidPatch_ShouldReturn200() throws Exception {
        when(inventoryService.patchInventory(eq(1L), anyString())).thenReturn(testInventoryDTO);

        mockMvc.perform(patch("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/quantity\",\"value\":100}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchInventory_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(inventoryService.patchInventory(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteInventory_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(inventoryService).deleteInventory(1L);

        mockMvc.perform(delete("/api/inventory/1"))
                .andExpect(status().isNoContent());
    }
}
