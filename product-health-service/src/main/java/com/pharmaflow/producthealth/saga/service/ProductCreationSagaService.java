package com.pharmaflow.producthealth.saga.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.saga.events.*;
import com.pharmaflow.producthealth.saga.model.SagaState;
import com.pharmaflow.producthealth.saga.publisher.SagaEventPublisher;
import com.pharmaflow.producthealth.saga.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga Orchestrator for Product Creation flow.
 *
 * Saga Flow:
 * 1. Product is created and saved in product-health-service (Local Transaction 1)
 * 2. ProductCreatedEvent is published to RabbitMQ
 * 3. Pharmacy Inventory Service receives event and creates stock entry (Local Transaction 2)
 * 4a. If stock creation succeeds → ProductStockCreatedEvent → Saga COMPLETED (final)
 * 4b. If stock creation fails → ProductStockCreationFailedEvent → Delete product (Compensating Transaction) → Saga COMPENSATED
 *
 * Implements Saga Choreography pattern: no central coordinator,
 * each service reacts to events from other services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCreationSagaService {

    private final SagaStateRepository sagaStateRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SagaEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Start saga: Save product and publish ProductCreatedEvent.
     * This is Local Transaction 1.
     * This is Local Transaction 1.
     */
    @Transactional
    public String startProductCreationSaga(ProductCreateDTO dto) {
        String sagaId = UUID.randomUUID().toString();
        log.info("🚀 Starting Product Creation Saga: sagaId={}", sagaId);

        try {
            // 1. Create and save saga state
            SagaState sagaState = new SagaState();
            sagaState.setSagaId(sagaId);
            sagaState.setStatus(SagaState.SagaStatus.STARTED);
            sagaState.setSagaType("PRODUCT_CREATION");
            sagaState.setCurrentStep("STARTED");
            sagaState.setPayload("{}");
            sagaStateRepository.save(sagaState);
            log.info("📝 Saga state created: sagaId={}", sagaId);

            // 2. Validate category exists
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                            "Category with ID " + dto.getCategoryId() + " not found."));

            // 3. Save product (Local Transaction 1)
            Product product = new Product();
            product.setName(dto.getName());
            product.setBarcode(dto.getBarcode());
            product.setDescription(dto.getDescription());
            product.setPrice(dto.getPrice());
            product.setBrandName(dto.getBrandName());
            product.setManufacturer(dto.getManufacturer());
            product.setRequiresPrescription(dto.getRequiresPrescription());
            product.setProductType(Product.ProductType.valueOf(dto.getProductType()));
            product.setImageUrl(dto.getImageUrl());
            product.setPackageSize(dto.getPackageSize());
            product.setCategory(category);
            product.setIsActive(true);
            product.setSubstances(new java.util.ArrayList<>());

            Product savedProduct = productRepository.saveAndFlush(product);
            log.info("✅ Product saved: productId={}", savedProduct.getId());

            // 4. Update saga state to PRODUCT_CREATED
            sagaState.setStatus(SagaState.SagaStatus.PRODUCT_CREATED);
            sagaState.setEntityId(savedProduct.getId());
            sagaState.setCurrentStep("PRODUCT_CREATED");
            sagaStateRepository.save(sagaState);

            // 5. Publish ProductCreatedEvent to trigger stock entry creation (Local Transaction 2)
            ProductCreatedEvent event = new ProductCreatedEvent(
                    sagaId,
                    savedProduct.getId(),
                    savedProduct.getName(),
                    savedProduct.getBarcode(),
                    savedProduct.getManufacturer(),
                    savedProduct.getPrice(),
                    savedProduct.getProductType().name()
            );
            eventPublisher.publishProductCreatedEvent(event);

            // 6. Update saga state to STOCK_PENDING
            sagaState.setStatus(SagaState.SagaStatus.STOCK_PENDING);
            sagaState.setCurrentStep("STOCK_PENDING");
            sagaStateRepository.save(sagaState);

            log.info("✅ Saga initiated successfully: sagaId={}", sagaId);
            return sagaId;

        } catch (Exception e) {
            log.error("❌ Failed to start saga: sagaId={}, error={}", sagaId, e.getMessage(), e);
            markSagaAsFailed(sagaId, "Failed to start saga: " + e.getMessage());
            throw new RuntimeException("Failed to start product creation saga", e);
        }
    }

    /**
     * Handle successful stock entry creation.
     * Marks saga as COMPLETED (final state).
     */
    @Transactional
    public void handleProductStockCreated(ProductStockCreatedEvent event) {
        String sagaId = event.getSagaId();
        log.info("✅ Handling successful stock creation: sagaId={}", sagaId);

        Optional<SagaState> sagaStateOpt = sagaStateRepository.findBySagaId(sagaId);
        if (sagaStateOpt.isEmpty()) {
            log.warn("⚠️  Saga state not found: sagaId={}", sagaId);
            return;
        }

        SagaState sagaState = sagaStateOpt.get();

        // Idempotency check
        if (sagaState.getStatus() == SagaState.SagaStatus.COMPLETED) {
            log.info("ℹ️  Saga already completed (idempotent): sagaId={}", sagaId);
            return;
        }

        // Mark as COMPLETED — this is the final state
        sagaState.setStatus(SagaState.SagaStatus.COMPLETED);
        sagaState.setCurrentStep("COMPLETED");
        sagaState.setCompletedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);

        log.info("🎉 Saga completed successfully: sagaId={}, productId={}", sagaId, event.getProductId());
    }

    /**
     * Handle stock entry creation failure.
     * Triggers compensating transaction: delete the product.
     */
    @Transactional
    public void handleProductStockCreationFailed(ProductStockCreationFailedEvent event) {
        String sagaId = event.getSagaId();
        log.warn("⚠️  Handling stock creation failure: sagaId={}, reason={}", sagaId, event.getReason());

        Optional<SagaState> sagaStateOpt = sagaStateRepository.findBySagaId(sagaId);
        if (sagaStateOpt.isEmpty()) {
            log.warn("⚠️  Saga state not found: sagaId={}", sagaId);
            return;
        }

        SagaState sagaState = sagaStateOpt.get();

        // Idempotency check
        if (sagaState.getStatus() == SagaState.SagaStatus.COMPENSATED ||
            sagaState.getStatus() == SagaState.SagaStatus.COMPENSATING) {
            log.info("ℹ️  Saga already compensating/compensated (idempotent): sagaId={}", sagaId);
            return;
        }

        try {
            // 1. Mark saga as COMPENSATING
            sagaState.setStatus(SagaState.SagaStatus.COMPENSATING);
            sagaState.setCurrentStep("COMPENSATING");
            sagaState.setErrorMessage(event.getReason() + ": " + event.getErrorDetails());
            sagaStateRepository.save(sagaState);

            // 2. Delete product (Compensating Transaction)
            Long productId = event.getProductId();
            Optional<Product> productOpt = productRepository.findById(productId);

            if (productOpt.isPresent()) {
                productRepository.deleteById(productId);
                log.info("🔄 Product deleted as compensation: productId={}", productId);

                // 3. Publish ProductDeletedEvent to notify pharmacy service to clean up
                ProductDeletedEvent productDeletedEvent = new ProductDeletedEvent(
                        sagaId, productId, "COMPENSATION");
                eventPublisher.publishProductDeletedEvent(productDeletedEvent);

                // 4. Mark saga as COMPENSATED
                sagaState.setStatus(SagaState.SagaStatus.COMPENSATED);
                sagaState.setCurrentStep("COMPENSATED");
                sagaState.setCompletedAt(LocalDateTime.now());
                sagaStateRepository.save(sagaState);

                log.info("✅ Saga compensated successfully: sagaId={}", sagaId);
            } else {
                log.warn("⚠️  Product not found for compensation: productId={}", productId);
                sagaState.setStatus(SagaState.SagaStatus.FAILED);
                sagaState.setErrorMessage("Product not found for compensation");
                sagaStateRepository.save(sagaState);
            }

        } catch (Exception e) {
            log.error("❌ Failed to compensate saga: sagaId={}, error={}", sagaId, e.getMessage(), e);
            markSagaAsFailed(sagaId, "Compensation failed: " + e.getMessage());
        }
    }

    private void markSagaAsFailed(String sagaId, String errorMessage) {
        sagaStateRepository.findBySagaId(sagaId).ifPresent(sagaState -> {
            sagaState.setStatus(SagaState.SagaStatus.FAILED);
            sagaState.setErrorMessage(errorMessage);
            sagaState.setCompletedAt(LocalDateTime.now());
            sagaStateRepository.save(sagaState);
            log.error("❌ Saga marked as FAILED: sagaId={}", sagaId);
        });
    }

    public Optional<SagaState> getSagaState(String sagaId) {
        return sagaStateRepository.findBySagaId(sagaId);
    }
}
