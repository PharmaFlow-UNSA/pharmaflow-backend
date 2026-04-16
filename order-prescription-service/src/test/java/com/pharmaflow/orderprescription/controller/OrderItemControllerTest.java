package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private OrderItemService orderItemService;

    private OrderItemDTO orderItemDTO;

    @BeforeEach
    void setUp() {
        orderItemDTO = new OrderItemDTO();
        orderItemDTO.setId(1L);
        orderItemDTO.setProductId(100L);
        orderItemDTO.setProductName("Brufen 400mg");
        orderItemDTO.setQuantity(2);
        orderItemDTO.setUnitPrice(BigDecimal.valueOf(8.50));
        orderItemDTO.setOrderId(1L);
    }

    @Test
    void getAllOrderItems_ShouldReturn200AndList() throws Exception {
        List<OrderItemDTO> orderItems = Arrays.asList(orderItemDTO);
        when(orderItemService.getAllOrderItems()).thenReturn(orderItems);

        mockMvc.perform(get("/api/order-items"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].productId").value(100))
                .andExpect(jsonPath("$[0].productName").value("Brufen 400mg"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].unitPrice").value(8.50));

        verify(orderItemService, times(1)).getAllOrderItems();
    }

    @Test
    void getOrderItemById_WhenExists_ShouldReturn200() throws Exception {
        when(orderItemService.getOrderItemById(1L)).thenReturn(orderItemDTO);

        mockMvc.perform(get("/api/order-items/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.productName").value("Brufen 400mg"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(8.50))
                .andExpect(jsonPath("$.orderId").value(1));

        verify(orderItemService, times(1)).getOrderItemById(1L);
    }

    @Test
    void getOrderItemById_WhenNotExists_ShouldReturn404() throws Exception {
        when(orderItemService.getOrderItemById(999L))
                .thenThrow(new ResourceNotFoundException("Order item not found with id: 999"));

        mockMvc.perform(get("/api/order-items/999"))
                .andExpect(status().isNotFound());

        verify(orderItemService, times(1)).getOrderItemById(999L);
    }

    @Test
    void getOrderItemsByOrderId_ShouldReturn200AndList() throws Exception {
        List<OrderItemDTO> orderItems = Arrays.asList(orderItemDTO);
        when(orderItemService.getOrderItemsByOrderId(1L)).thenReturn(orderItems);

        mockMvc.perform(get("/api/order-items/order/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId").value(1));

        verify(orderItemService, times(1)).getOrderItemsByOrderId(1L);
    }

    @Test
    void createOrderItem_WithValidData_ShouldReturn201() throws Exception {
        when(orderItemService.createOrderItem(any(OrderItemDTO.class))).thenReturn(orderItemDTO);

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderItemDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.productName").value("Brufen 400mg"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(8.50));

        verify(orderItemService, times(1)).createOrderItem(any(OrderItemDTO.class));
    }

    @Test
    void createOrderItem_WithInvalidData_ShouldReturn400() throws Exception {
        OrderItemDTO invalidDTO = new OrderItemDTO();
        invalidDTO.setProductId(100L);
        invalidDTO.setProductName("");
        invalidDTO.setQuantity(2);
        invalidDTO.setUnitPrice(BigDecimal.valueOf(8.50));
        invalidDTO.setOrderId(1L);

        mockMvc.perform(post("/api/order-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(orderItemService, never()).createOrderItem(any(OrderItemDTO.class));
    }

    @Test
    void deleteOrderItem_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(orderItemService).deleteOrderItem(1L);

        mockMvc.perform(delete("/api/order-items/1"))
                .andExpect(status().isNoContent());

        verify(orderItemService, times(1)).deleteOrderItem(1L);
    }
}
