package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderItem;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderService orderService;

    private Order testOrder;
    private OrderCreateDTO testCreateDTO;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, prescriptionRepository, modelMapper, objectMapper, validator);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(10L);
        testOrder.setStatus("PENDING");
        testOrder.setTotalAmount(new BigDecimal("20.00"));
        testOrder.setShippingAddress("Marsala Tita 10");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setOrderItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setProductId(100L);
        item.setProductName("Aspirin");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setOrder(testOrder);
        testOrder.getOrderItems().add(item);

        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setProductId(100L);
        itemDTO.setProductName("Aspirin");
        itemDTO.setQuantity(2);
        itemDTO.setUnitPrice(new BigDecimal("10.00"));

        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setAmount(new BigDecimal("20.00"));
        paymentDTO.setMethod("CARD");
        paymentDTO.setStatus("PENDING");

        testCreateDTO = new OrderCreateDTO();
        testCreateDTO.setUserId(10L);
        testCreateDTO.setShippingAddress("Marsala Tita 10");
        testCreateDTO.setOrderItems(List.of(itemDTO));
        testCreateDTO.setPayment(paymentDTO);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<OrderDTO> result = orderService.findAll(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
    }

    @Test
    void getOrderById_WhenExists_ShouldReturn() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void getOrderById_WhenNotExists_ShouldThrow() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(99L));
    }

    @Test
    void getOrdersByUserId_ShouldReturnList() {
        when(orderRepository.findByUserId(10L)).thenReturn(List.of(testOrder));

        assertEquals(1, orderService.getOrdersByUserId(10L).size());
    }

    @Test
    void getOrdersByStatus_ShouldReturnList() {
        when(orderRepository.findByStatus("PENDING")).thenReturn(List.of(testOrder));

        assertEquals(1, orderService.getOrdersByStatus("PENDING").size());
    }

    @Test
    void createOrder_ShouldSucceed() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderDTO result = orderService.createOrder(testCreateDTO);

        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrdersBatch_ShouldSaveAll() {
        when(orderRepository.saveAll(any())).thenReturn(List.of(testOrder));

        List<OrderDTO> result = orderService.createOrdersBatch(List.of(testCreateDTO));

        assertEquals(1, result.size());
        verify(orderRepository).saveAll(any());
    }

    @Test
    void updateOrder_WhenExists_ShouldUpdate() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderDTO dto = new OrderDTO();
        dto.setStatus("SHIPPED");
        dto.setShippingAddress("Marsala Tita 10");
        OrderDTO result = orderService.updateOrder(1L, dto);

        assertNotNull(result);
    }

    @Test
    void updateOrder_WhenNotExists_ShouldThrow() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrder(99L, new OrderDTO()));
    }

    @Test
    void patchOrder_WithValidPatch_ShouldUpdate() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderDTO result = orderService.patchOrder(1L,
                "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"SHIPPED\"}]");

        assertNotNull(result);
    }

    @Test
    void patchOrder_WithMalformedPatch_ShouldThrow() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(PatchOperationException.class,
                () -> orderService.patchOrder(1L, "junk"));
    }

    @Test
    void deleteOrder_WhenExists_ShouldDelete() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrder_WhenNotExists_ShouldThrow() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.deleteOrder(99L));
    }
}
