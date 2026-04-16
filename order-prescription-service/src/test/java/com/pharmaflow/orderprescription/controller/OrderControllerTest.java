package com.pharmaflow.orderprescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private OrderService orderService;

    private OrderDTO orderDTO;
    private OrderCreateDTO orderCreateDTO;

    @BeforeEach
    void setUp() {
        orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setUserId(1L);
        orderDTO.setStatus("PENDING");
        orderDTO.setTotalAmount(BigDecimal.valueOf(25.00));
        orderDTO.setShippingAddress("Ulica 1, Sarajevo");
        orderDTO.setCreatedAt(LocalDateTime.of(2026, 4, 16, 10, 0));
        orderDTO.setOrderItems(new ArrayList<>());
        orderDTO.setPayment(null);
        orderDTO.setPrescriptionId(null);

        orderCreateDTO = new OrderCreateDTO();
        orderCreateDTO.setUserId(1L);
        orderCreateDTO.setShippingAddress("Ulica 1, Sarajevo");
        orderCreateDTO.setOrderItems(new ArrayList<>());
    }

    @Test
    void getAllOrders_ShouldReturn200AndList() throws Exception {
        List<OrderDTO> orders = Arrays.asList(orderDTO);
        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].shippingAddress").value("Ulica 1, Sarajevo"));

        verify(orderService, times(1)).getAllOrders();
    }

    @Test
    void getOrderById_WhenExists_ShouldReturn200() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.shippingAddress").value("Ulica 1, Sarajevo"));

        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturn404() throws Exception {
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrderById(999L);
    }

    @Test
    void getOrdersByUserId_ShouldReturn200AndList() throws Exception {
        List<OrderDTO> orders = Arrays.asList(orderDTO);
        when(orderService.getOrdersByUserId(1L)).thenReturn(orders);

        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(orderService, times(1)).getOrdersByUserId(1L);
    }

    @Test
    void createOrder_WithValidData_ShouldReturn201() throws Exception {
        when(orderService.createOrder(any(OrderCreateDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.shippingAddress").value("Ulica 1, Sarajevo"));

        verify(orderService, times(1)).createOrder(any(OrderCreateDTO.class));
    }

    @Test
    void createOrder_WithInvalidData_ShouldReturn400() throws Exception {
        OrderCreateDTO invalidDTO = new OrderCreateDTO();
        invalidDTO.setUserId(1L);
        invalidDTO.setShippingAddress("");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(OrderCreateDTO.class));
    }

    @Test
    void updateOrder_WithValidData_ShouldReturn200() throws Exception {
        when(orderService.updateOrder(eq(1L), any(OrderDTO.class))).thenReturn(orderDTO);

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).updateOrder(eq(1L), any(OrderDTO.class));
    }

    @Test
    void deleteOrder_WhenExists_ShouldReturn204() throws Exception {
        doNothing().when(orderService).deleteOrder(1L);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteOrder(1L);
    }
}
