package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderItem;
import com.pharmaflow.orderprescription.repositories.OrderItemRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private Validator validator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderItemService orderItemService;

    private Order testOrder;
    private OrderItem testItem;
    private OrderItemDTO testDTO;

    @BeforeEach
    void setUp() {
        orderItemService = new OrderItemService(orderItemRepository, orderRepository, modelMapper, objectMapper, validator);

        testOrder = new Order();
        testOrder.setId(1L);

        testItem = new OrderItem();
        testItem.setId(1L);
        testItem.setProductId(100L);
        testItem.setProductName("Aspirin");
        testItem.setQuantity(2);
        testItem.setUnitPrice(new BigDecimal("10.00"));
        testItem.setOrder(testOrder);

        testDTO = new OrderItemDTO();
        testDTO.setProductId(100L);
        testDTO.setProductName("Aspirin");
        testDTO.setQuantity(2);
        testDTO.setUnitPrice(new BigDecimal("10.00"));
        testDTO.setOrderId(1L);
    }

    @Test
    void findAll_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderItem> page = new PageImpl<>(List.of(testItem));
        when(orderItemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<OrderItemDTO> result = orderItemService.findAll(null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrderItemById_WhenExists_ShouldReturn() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        assertEquals("Aspirin", result.getProductName());
    }

    @Test
    void getOrderItemById_WhenNotExists_ShouldThrow() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.getOrderItemById(99L));
    }

    @Test
    void getOrderItemsByOrderId_ShouldReturnList() {
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        assertEquals(1, orderItemService.getOrderItemsByOrderId(1L).size());
    }

    @Test
    void createOrderItem_WhenOrderExists_ShouldSucceed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testItem);

        OrderItemDTO result = orderItemService.createOrderItem(testDTO);

        assertNotNull(result);
    }

    @Test
    void createOrderItem_WhenOrderNotExists_ShouldThrow() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.createOrderItem(testDTO));
    }

    @Test
    void createOrderItemsBatch_ShouldSaveAll() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.saveAll(any())).thenReturn(List.of(testItem));

        List<OrderItemDTO> result = orderItemService.createOrderItemsBatch(List.of(testDTO));

        assertEquals(1, result.size());
    }

    @Test
    void updateOrderItem_WhenExists_ShouldUpdate() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testItem);

        OrderItemDTO result = orderItemService.updateOrderItem(1L, testDTO);

        assertNotNull(result);
    }

    @Test
    void updateOrderItem_WhenNotExists_ShouldThrow() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.updateOrderItem(99L, testDTO));
    }

    @Test
    void patchOrderItem_WithValidPatch_ShouldUpdate() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testItem);

        OrderItemDTO result = orderItemService.patchOrderItem(1L,
                "[{\"op\":\"replace\",\"path\":\"/quantity\",\"value\":5}]");

        assertNotNull(result);
    }

    @Test
    void patchOrderItem_WithMalformedPatch_ShouldThrow() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        assertThrows(PatchOperationException.class,
                () -> orderItemService.patchOrderItem(1L, "junk"));
    }

    @Test
    void deleteOrderItem_WhenExists_ShouldDelete() {
        when(orderItemRepository.existsById(1L)).thenReturn(true);

        orderItemService.deleteOrderItem(1L);

        verify(orderItemRepository).deleteById(1L);
    }

    @Test
    void deleteOrderItem_WhenNotExists_ShouldThrow() {
        when(orderItemRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.deleteOrderItem(99L));
    }
}
