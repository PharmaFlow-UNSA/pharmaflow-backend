package com.pharmaflow.orderprescription.service;

import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderItem;
import com.pharmaflow.orderprescription.repositories.OrderItemRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderItemService orderItemService;

    private OrderItem testOrderItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);

        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setProductId(100L);
        testOrderItem.setProductName("Brufen 400mg");
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(8.50));
        testOrderItem.setOrder(testOrder);
    }

    @Test
    void getAllOrderItems_WhenOrderItemsExist_ShouldReturnOrderItemList() {
        when(orderItemRepository.findAll()).thenReturn(List.of(testOrderItem));

        List<OrderItemDTO> result = orderItemService.getAllOrderItems();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrderItem.getId(), result.get(0).getId());
        assertEquals(testOrderItem.getProductName(), result.get(0).getProductName());
        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    void getOrderItemById_WhenOrderItemExists_ShouldReturnOrderItem() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem));

        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        assertNotNull(result);
        assertEquals(testOrderItem.getId(), result.getId());
        assertEquals(testOrderItem.getProductId(), result.getProductId());
        assertEquals(testOrderItem.getProductName(), result.getProductName());
        assertEquals(testOrderItem.getQuantity(), result.getQuantity());
        assertEquals(testOrderItem.getUnitPrice(), result.getUnitPrice());
        verify(orderItemRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderItemById_WhenOrderItemNotExists_ShouldThrowResourceNotFoundException() {
        when(orderItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.getOrderItemById(99L));
        verify(orderItemRepository, times(1)).findById(99L);
    }

    @Test
    void getOrderItemsByOrderId_WhenOrderItemsExist_ShouldReturnOrderItemList() {
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testOrderItem));

        List<OrderItemDTO> result = orderItemService.getOrderItemsByOrderId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getId(), result.get(0).getOrderId());
        verify(orderItemRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void createOrderItem_WhenOrderExists_ShouldReturnCreatedOrderItem() {
        OrderItemDTO createDTO = new OrderItemDTO();
        createDTO.setProductId(100L);
        createDTO.setProductName("Brufen 400mg");
        createDTO.setQuantity(2);
        createDTO.setUnitPrice(BigDecimal.valueOf(8.50));
        createDTO.setOrderId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        OrderItemDTO result = orderItemService.createOrderItem(createDTO);

        assertNotNull(result);
        assertEquals(createDTO.getProductId(), result.getProductId());
        assertEquals(createDTO.getProductName(), result.getProductName());
        assertEquals(createDTO.getQuantity(), result.getQuantity());
        assertEquals(createDTO.getUnitPrice(), result.getUnitPrice());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void createOrderItem_WhenOrderNotExists_ShouldThrowResourceNotFoundException() {
        OrderItemDTO createDTO = new OrderItemDTO();
        createDTO.setOrderId(99L);

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.createOrderItem(createDTO));
        verify(orderRepository, times(1)).findById(99L);
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void deleteOrderItem_WhenOrderItemExists_ShouldDeleteSuccessfully() {
        when(orderItemRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> orderItemService.deleteOrderItem(1L));
        verify(orderItemRepository, times(1)).existsById(1L);
        verify(orderItemRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteOrderItem_WhenOrderItemNotExists_ShouldThrowResourceNotFoundException() {
        when(orderItemRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.deleteOrderItem(99L));
        verify(orderItemRepository, times(1)).existsById(99L);
        verify(orderItemRepository, never()).deleteById(any());
    }
}
