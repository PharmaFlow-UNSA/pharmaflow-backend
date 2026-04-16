package com.pharmaflow.pharmacyinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.pharmacyinventory.dto.InventoryDTO;
import com.pharmaflow.pharmacyinventory.exception.ResourceNotFoundException;
import com.pharmaflow.pharmacyinventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
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
    void getAllInventoryItems_ShouldReturn200AndList() throws Exception {
        when(inventoryService.getAllInventoryItems()).thenReturn(List.of(testInventoryDTO));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(100));

        verify(inventoryService, times(1)).getAllInventoryItems();
    }

    @Test
    void getInventoryById_WhenExists_ShouldReturn200() throws Exception {
        when(inventoryService.getInventoryById(1L)).thenReturn(testInventoryDTO);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.quantity").value(50));

        verify(inventoryService, times(1)).getInventoryById(1L);
    }

    @Test
    void getInventoryById_WhenNotExists_ShouldReturn404() throws Exception {
        when(inventoryService.getInventoryById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory not found with id: 999"));

        mockMvc.perform(get("/api/inventory/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));

        verify(inventoryService, times(1)).getInventoryById(999L);
    }

    @Test
    void getInventoryByPharmacyId_ShouldReturn200AndList() throws Exception {
        when(inventoryService.getInventoryByPharmacyId(1L)).thenReturn(List.of(testInventoryDTO));

        mockMvc.perform(get("/api/inventory/pharmacy/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pharmacyId").value(1));

        verify(inventoryService, times(1)).getInventoryByPharmacyId(1L);
    }

    @Test
    void createInventory_WithValidData_ShouldReturn201() throws Exception {
        when(inventoryService.createInventory(any(InventoryDTO.class))).thenReturn(testInventoryDTO);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testInventoryDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(100));

        verify(inventoryService, times(1)).createInventory(any(InventoryDTO.class));
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

        verify(inventoryService, never()).createInventory(any(InventoryDTO.class));
    }

    @Test
    void updateInventory_WithValidData_ShouldReturn200() throws Exception {
        InventoryDTO updatedDTO = new InventoryDTO();
        updatedDTO.setId(1L);
        updatedDTO.setProductId(100L);
        updatedDTO.setQuantity(75);
        updatedDTO.setReorderLevel(15);
        updatedDTO.setLastRestocked(null);
        updatedDTO.setPharmacyId(1L);

        when(inventoryService.updateInventory(eq(1L), any(InventoryDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.quantity").value(75))
                .andExpect(jsonPath("$.reorderLevel").value(15));

        verify(inventoryService, times(1)).updateInventory(eq(1L), any(InventoryDTO.class));
    }

    @Test
    void deleteInventory_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(inventoryService).deleteInventory(1L);

        mockMvc.perform(delete("/api/inventory/1"))
                .andExpect(status().isNoContent());

        verify(inventoryService, times(1)).deleteInventory(1L);
    }
}
