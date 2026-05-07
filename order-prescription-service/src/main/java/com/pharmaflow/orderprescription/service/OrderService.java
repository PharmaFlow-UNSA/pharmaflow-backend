package com.pharmaflow.orderprescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.pharmaflow.orderprescription.dto.OrderCreateDTO;
import com.pharmaflow.orderprescription.dto.OrderDTO;
import com.pharmaflow.orderprescription.dto.OrderItemDTO;
import com.pharmaflow.orderprescription.dto.PaymentDTO;
import com.pharmaflow.orderprescription.exception.PatchOperationException;
import com.pharmaflow.orderprescription.exception.ResourceNotFoundException;
import com.pharmaflow.orderprescription.models.Order;
import com.pharmaflow.orderprescription.models.OrderItem;
import com.pharmaflow.orderprescription.models.Payment;
import com.pharmaflow.orderprescription.models.Prescription;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import com.pharmaflow.orderprescription.specifications.OrderSpecs;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OrderService(OrderRepository orderRepository,
                        PrescriptionRepository prescriptionRepository,
                        ModelMapper modelMapper,
                        ObjectMapper objectMapper,
                        Validator validator) {
        this.orderRepository = orderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> findAll(Long userId, String status, Pageable pageable) {
        long startTime = System.currentTimeMillis();

        Specification<Order> spec = Specification
                .where(OrderSpecs.hasUserId(userId))
                .and(OrderSpecs.hasStatus(status));

        Page<OrderDTO> result = orderRepository.findAll(spec, pageable)
                .map(this::convertToDTO);

        log.info("Order findAll executed in {} ms, returned {} of {} total",
                System.currentTimeMillis() - startTime,
                result.getNumberOfElements(),
                result.getTotalElements());

        return result;
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public OrderDTO createOrder(OrderCreateDTO dto) {
        Order order = buildOrderFromDTO(dto);
        Order saved = orderRepository.save(order);
        log.info("Order created with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public List<OrderDTO> createOrdersBatch(List<OrderCreateDTO> dtos) {
        long startTime = System.currentTimeMillis();
        List<Order> orders = new ArrayList<>();

        for (OrderCreateDTO dto : dtos) {
            orders.add(buildOrderFromDTO(dto));
        }

        List<Order> saved = orderRepository.saveAll(orders);
        log.info("Batch created {} orders in {} ms",
                saved.size(), System.currentTimeMillis() - startTime);

        return saved.stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public OrderDTO updateOrder(Long id, OrderDTO dto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(dto.getStatus());
        order.setShippingAddress(dto.getShippingAddress());
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        log.info("Order updated with id: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Transactional
    public OrderDTO patchOrder(Long id, String patchDocument) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

            OrderDTO currentDTO = convertToDTO(order);
            JsonNode currentJson = objectMapper.valueToTree(currentDTO);
            JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchDocument));
            JsonNode patchedJson = patch.apply(currentJson);
            OrderDTO patchedDTO = objectMapper.treeToValue(patchedJson, OrderDTO.class);

            // Re-run Jakarta Bean Validation on the patched DTO so constraints apply after JSON Patch.
            Set<ConstraintViolation<OrderDTO>> violations = validator.validate(patchedDTO);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }

            order.setUserId(patchedDTO.getUserId());
            order.setStatus(patchedDTO.getStatus());
            order.setShippingAddress(patchedDTO.getShippingAddress());
            order.setUpdatedAt(LocalDateTime.now());

            Order saved = orderRepository.save(order);
            log.info("Order patched with id: {}", saved.getId());
            return convertToDTO(saved);

        } catch (JsonPatchException | java.io.IOException e) {
            throw new PatchOperationException("Error applying patch: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
        log.info("Order deleted with id: {}", id);
    }

    private Order buildOrderFromDTO(OrderCreateDTO dto) {
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setStatus("PENDING");
        order.setShippingAddress(dto.getShippingAddress());
        order.setCreatedAt(LocalDateTime.now());

        if (dto.getPrescriptionId() != null) {
            Prescription prescription = prescriptionRepository.findById(dto.getPrescriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prescription not found with id: " + dto.getPrescriptionId()));
            order.setPrescription(prescription);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (dto.getOrderItems() != null) {
            for (OrderItemDTO itemDTO : dto.getOrderItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDTO.getProductId());
                item.setProductName(itemDTO.getProductName());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                item.setOrder(order);
                orderItems.add(item);

                totalAmount = totalAmount.add(
                        itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()))
                );
            }
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        if (dto.getPayment() != null) {
            Payment payment = new Payment();
            payment.setAmount(dto.getPayment().getAmount());
            payment.setMethod(dto.getPayment().getMethod());
            payment.setStatus(dto.getPayment().getStatus());
            payment.setTransactionId(dto.getPayment().getTransactionId());
            payment.setPaidAt(dto.getPayment().getPaidAt());
            order.setPayment(payment);
        }

        return order;
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (order.getOrderItems() != null) {
            dto.setOrderItems(
                    order.getOrderItems().stream()
                            .map(this::convertOrderItemToDTO)
                            .toList()
            );
        }

        if (order.getPayment() != null) {
            dto.setPayment(convertPaymentToDTO(order.getPayment()));
        }

        if (order.getPrescription() != null) {
            dto.setPrescriptionId(order.getPrescription().getId());
        }

        return dto;
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem item) {
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

    private PaymentDTO convertPaymentToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }
}
