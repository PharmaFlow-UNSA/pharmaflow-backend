package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus("PENDING");
        testOrder.setTotalAmount(BigDecimal.valueOf(25.00));
        testOrder.setShippingAddress("Ulica 1, Sarajevo");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setOrderItems(new ArrayList<>());
        testOrder.setPayment(null);
        testOrder.setPrescription(null);
    }

    @Test
    void getAllOrders_WhenOrdersExist_ShouldReturnOrderList() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        List<OrderDTO> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getId(), result.get(0).getId());
        assertEquals(testOrder.getUserId(), result.get(0).getUserId());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderDTO result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        assertEquals(testOrder.getUserId(), result.getUserId());
        assertEquals(testOrder.getStatus(), result.getStatus());
        assertEquals(testOrder.getTotalAmount(), result.getTotalAmount());
        assertEquals(testOrder.getShippingAddress(), result.getShippingAddress());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_WhenOrderNotExists_ShouldThrowResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
        verify(orderRepository, times(1)).findById(99L);
    }

    @Test
    void getOrdersByUserId_WhenOrdersExist_ShouldReturnOrderList() {
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(testOrder));

        List<OrderDTO> result = orderService.getOrdersByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getUserId(), result.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(1L);
    }

    @Test
    void createOrder_WhenValidInput_ShouldReturnCreatedOrder() {
        OrderCreateDTO createDTO = new OrderCreateDTO();
        createDTO.setUserId(1L);
        createDTO.setShippingAddress("Ulica 1, Sarajevo");
        createDTO.setPrescriptionId(null);
        createDTO.setOrderItems(new ArrayList<>());
        createDTO.setPayment(null);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        OrderDTO result = orderService.createOrder(createDTO);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(createDTO.getUserId(), result.getUserId());
        assertEquals(createDTO.getShippingAddress(), result.getShippingAddress());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrder_WhenOrderExists_ShouldReturnUpdatedOrder() {
        OrderDTO updateDTO = new OrderDTO();
        updateDTO.setStatus("CONFIRMED");
        updateDTO.setShippingAddress("Ulica 2, Mostar");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.updateOrder(1L, updateDTO);

        assertNotNull(result);
        assertEquals(updateDTO.getStatus(), result.getStatus());
        assertEquals(updateDTO.getShippingAddress(), result.getShippingAddress());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrder_WhenOrderNotExists_ShouldThrowResourceNotFoundException() {
        OrderDTO updateDTO = new OrderDTO();

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrder(99L, updateDTO));
        verify(orderRepository, times(1)).findById(99L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrder_WhenOrderExists_ShouldDeleteSuccessfully() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.deleteOrder(1L));
        verify(orderRepository, times(1)).existsById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteOrder_WhenOrderNotExists_ShouldThrowResourceNotFoundException() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(99L));
        verify(orderRepository, times(1)).existsById(99L);
        verify(orderRepository, never()).deleteById(any());
    }
}
