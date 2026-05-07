package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.OrderService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    private OrderService orderService;

    private OrderDTO testOrderDTO;
    private OrderCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        testOrderDTO = new OrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setUserId(10L);
        testOrderDTO.setStatus("PENDING");
        testOrderDTO.setShippingAddress("Marsala Tita 10, Sarajevo");
        testOrderDTO.setTotalAmount(new BigDecimal("100.00"));
        testOrderDTO.setCreatedAt(LocalDateTime.now());

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(100L);
        item.setProductName("Aspirin");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setOrderId(1L);

        PaymentDTO payment = new PaymentDTO();
        payment.setAmount(new BigDecimal("20.00"));
        payment.setMethod("CARD");
        payment.setStatus("PENDING");

        testCreateDTO = new OrderCreateDTO();
        testCreateDTO.setUserId(10L);
        testCreateDTO.setShippingAddress("Marsala Tita 10, Sarajevo");
        testCreateDTO.setOrderItems(List.of(item));
        testCreateDTO.setPayment(payment);
    }

    @Test
    void getOrders_ShouldReturn200AndPagedResult() throws Exception {
        Page<OrderDTO> page = new PageImpl<>(List.of(testOrderDTO));
        when(orderService.findAll(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getOrderById_WhenExists_ShouldReturn200() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testOrderDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturn404() throws Exception {
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByUserId_ShouldReturn200() throws Exception {
        when(orderService.getOrdersByUserId(10L)).thenReturn(List.of(testOrderDTO));

        mockMvc.perform(get("/api/orders/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getOrdersByStatus_ShouldReturn200() throws Exception {
        when(orderService.getOrdersByStatus("PENDING")).thenReturn(List.of(testOrderDTO));

        mockMvc.perform(get("/api/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createOrder_WithValidData_ShouldReturn201() throws Exception {
        when(orderService.createOrder(any(OrderCreateDTO.class))).thenReturn(testOrderDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void createOrder_WithInvalidData_ShouldReturn400() throws Exception {
        OrderCreateDTO invalid = new OrderCreateDTO();
        // missing userId, shippingAddress, etc.

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrdersBatch_ShouldReturn201() throws Exception {
        when(orderService.createOrdersBatch(anyList())).thenReturn(List.of(testOrderDTO));

        mockMvc.perform(post("/api/orders/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testCreateDTO))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateOrder_WithValidData_ShouldReturn200() throws Exception {
        when(orderService.updateOrder(eq(1L), any(OrderDTO.class))).thenReturn(testOrderDTO);

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void patchOrder_WithValidPatch_ShouldReturn200() throws Exception {
        when(orderService.patchOrder(eq(1L), anyString())).thenReturn(testOrderDTO);

        mockMvc.perform(patch("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"SHIPPED\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    void patchOrder_WithInvalidPatch_ShouldReturn400() throws Exception {
        when(orderService.patchOrder(eq(1L), anyString()))
                .thenThrow(new PatchOperationException("bad"));

        mockMvc.perform(patch("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteOrder_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(orderService).deleteOrder(1L);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }
}
