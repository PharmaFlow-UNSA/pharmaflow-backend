package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.OrderItemService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private OrderItemService orderItemService;

    private OrderItemDTO testItemDTO;

    @BeforeEach
    void setUp() {
        testItemDTO = new OrderItemDTO();
        testItemDTO.setId(1L);
        testItemDTO.setProductId(100L);
        testItemDTO.setProductName("Aspirin");
        testItemDTO.setQuantity(2);
        testItemDTO.setUnitPrice(new BigDecimal("10.00"));
        testItemDTO.setOrderId(1L);
    }

    @Test
    void getOrderItems_ShouldReturn200AndPagedResult() throws Exception {
        Page<OrderItemDTO> page = new PageImpl<>(List.of(testItemDTO));
        when(orderItemService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/order-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getOrderItemById_WhenExists_ShouldReturn200() throws Exception {
        when(orderItemService.getOrderItemById(1L)).thenReturn(testItemDTO);

        mockMvc.perform(get("/api/order-items/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrderItemById_WhenNotExists_ShouldReturn404() throws Exception {
        when(orderItemService.getOrderItemById(999L))
                .thenThrow(new ResourceNotFoundException("Order item not found with id: 999"));

        mockMvc.perform(get("/api/order-items/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderItemsByOrderId_ShouldReturn200() throws Exception {
        when(orderItemService.getOrderItemsByOrderId(1L)).thenReturn(List.of(testItemDTO));

        mockMvc.perform(get("/api/order-items/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createOrderItem_WithValidData_ShouldReturn201() throws Exception {
        when(orderItemService.createOrderItem(any(OrderItemDTO.class))).thenReturn(testItemDTO);

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItemDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createOrderItem_WithInvalidData_ShouldReturn400() throws Exception {
        OrderItemDTO invalid = new OrderItemDTO();
        // missing required fields

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrderItemsBatch_ShouldReturn201() throws Exception {
        when(orderItemService.createOrderItemsBatch(anyList())).thenReturn(List.of(testItemDTO));

        mockMvc.perform(post("/api/order-items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testItemDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateOrderItem_WithValidData_ShouldReturn200() throws Exception {
        when(orderItemService.updateOrderItem(eq(1L), any(OrderItemDTO.class))).thenReturn(testItemDTO);

        mockMvc.perform(put("/api/order-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItemDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchOrderItem_WithValidPatch_ShouldReturn200() throws Exception {
        when(orderItemService.patchOrderItem(eq(1L), anyString())).thenReturn(testItemDTO);

        mockMvc.perform(patch("/api/order-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/quantity\",\"value\":5}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchOrderItem_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(orderItemService.patchOrderItem(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/order-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteOrderItem_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(orderItemService).deleteOrderItem(1L);

        mockMvc.perform(delete("/api/order-items/1"))
                .andExpect(status().isNoContent());
    }
}
