package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderItem;
import com.pharmaflow.orderprescription.repositories.OrderItemRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.specifications.OrderItemSpecs;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrderItemService {

    private static final Logger log = LoggerFactory.getLogger(OrderItemService.class);

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OrderItemService(OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository,
                            ModelMapper modelMapper,
                            ObjectMapper objectMapper,
                            Validator validator) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<OrderItemDTO> findAll(Long orderId, Long productId, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<OrderItem> spec = Specification
                .where(OrderItemSpecs.hasOrderId(orderId))
                .and(OrderItemSpecs.hasProductId(productId));

        Page<OrderItemDTO> result = orderItemRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("OrderItem findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public OrderItemDTO getOrderItemById(Long id) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));
        return convertToDTO(orderItem);
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public OrderItemDTO createOrderItem(OrderItemDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + dto.getOrderId()));

        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(dto.getProductId());
        orderItem.setProductName(dto.getProductName());
        orderItem.setQuantity(dto.getQuantity());
        orderItem.setUnitPrice(dto.getUnitPrice());
        orderItem.setOrder(order);

        OrderItem saved = orderItemRepository.save(orderItem);
        log.info("Order item created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<OrderItemDTO> createOrderItemsBatch(List<OrderItemDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO dto : dtos) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order not found with id: " + dto.getOrderId()));

            OrderItem item = new OrderItem();
            item.setProductId(dto.getProductId());
            item.setProductName(dto.getProductName());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setOrder(order);
            orderItems.add(item);
        }

        List<OrderItem> saved = orderItemRepository.saveAll(orderItems);
        log.info("Batch created {} order items in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public OrderItemDTO updateOrderItem(Long id, OrderItemDTO dto) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));

        orderItem.setProductId(dto.getProductId());
        orderItem.setProductName(dto.getProductName());
        orderItem.setQuantity(dto.getQuantity());
        orderItem.setUnitPrice(dto.getUnitPrice());

        OrderItem saved = orderItemRepository.save(orderItem);
        log.info("Order item updated with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public OrderItemDTO patchOrderItem(Long id, String patchDocument) {
        try {
            OrderItem orderItem = orderItemRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));

            OrderItemDTO currentDTO = convertToDTO(orderItem);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            OrderItemDTO patchedDTO = objectMapper.treeToValue(patchedJson, OrderItemDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<OrderItemDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            orderItem.setProductId(patchedDTO.getProductId());
            orderItem.setProductName(patchedDTO.getProductName());
            orderItem.setQuantity(patchedDTO.getQuantity());
            orderItem.setUnitPrice(patchedDTO.getUnitPrice());

            OrderItem saved = orderItemRepository.save(orderItem);
            log.info("Order item patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteOrderItem(Long id) {
        if (!orderItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order item not found with id: " + id);
        }
        orderItemRepository.deleteById(id);
        log.info("Order item deleted with id: {}", id);
    }

    private OrderItemDTO convertToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        if (item.getOrder() != null) {
            dto.setOrderId(item.getOrder().getId());
        }
        return dto;
    }
}
