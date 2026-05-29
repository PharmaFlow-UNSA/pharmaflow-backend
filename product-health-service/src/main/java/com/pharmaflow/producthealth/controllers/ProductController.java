package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.*;
import com.pharmaflow.producthealth.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "API for managing pharmaceutical products")
public class ProductController {

    private final ProductService productService;

    // ── Basic CRUD ────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all active products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    @GetMapping("/{id:-?\\d+}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<ProductDTO>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Get products containing a specific substance")
    public ResponseEntity<List<ProductDTO>> getBySubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(productService.getProductsBySubstance(substanceId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        return new ResponseEntity<>(productService.createProduct(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id:-?\\d+}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Update an existing product (full replacement)")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id,
                                                    @Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @PatchMapping("/{id:-?\\d+}/deactivate")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Deactivate product (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id:-?\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH - JSON Patch RFC 6902 ───────────────────────────────────────

    @PatchMapping(value = "/{id:-?\\d+}", consumes = "application/json-patch+json")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Partial update - JSON Patch (RFC 6902)",
            description = "Example: [{\"op\":\"replace\",\"path\":\"/price\",\"value\":9.99}]")
    public ResponseEntity<ProductDTO> patchProduct(@PathVariable Long id,
                                                   @RequestBody String patchDocument) {
        return ResponseEntity.ok(productService.patchProduct(id, patchDocument));
    }

    // ── Pagination and Sorting with Specifications ────────────────────────

    @GetMapping("/page")
    @Operation(summary = "Pagination with dynamic filtering",
            description = "All query parameters are optional. Example: /page?page=0&size=5&sort=price,asc&name=brufen&requiresPrescription=false")
    public ResponseEntity<Page<ProductDTO>> getProductsPageable(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Boolean requiresPrescription,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(productService.getProductsPageable(
                name, manufacturer, productType, requiresPrescription, minPrice, maxPrice, pageable));
    }

    // ── Custom Queries ────────────────────────────────────────────────────

    @GetMapping("/price-range")
    @Operation(summary = "Search by price range")
    public ResponseEntity<List<ProductDTO>> getByPriceRange(
            @RequestParam @DecimalMin(value = "0.0", message = "Minimum price must be >= 0") BigDecimal minPrice,
            @RequestParam @DecimalMin(value = "0.0", message = "Maximum price must be >= 0") BigDecimal maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter by type and prescription requirement")
    public ResponseEntity<List<ProductDTO>> getByTypeAndPrescription(
            @RequestParam String type,
            @RequestParam Boolean requiresPrescription) {
        return ResponseEntity.ok(productService.getProductsByTypeAndPrescription(type, requiresPrescription));
    }

    @GetMapping("/otc")
    @Operation(summary = "All OTC products sorted by price ascending")
    public ResponseEntity<List<ProductDTO>> getOtcProducts() {
        return ResponseEntity.ok(productService.getOtcProductsSortedByPrice());
    }

    @GetMapping("/stats/count-by-type")
    @Operation(summary = "Statistics - count of active products by type")
    public ResponseEntity<Map<String, Long>> getCountByType() {
        return ResponseEntity.ok(productService.getProductCountByType());
    }

    // ── Batch Import ──────────────────────────────────────────────────────

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Batch import of multiple products in a single transaction",
            description = "Rollback on any error. Max 50 products.")
    public ResponseEntity<List<ProductDTO>> createProductsBatch(@Valid @RequestBody ProductBatchDTO batchDTO) {
        return new ResponseEntity<>(productService.createProductsBatch(batchDTO), HttpStatus.CREATED);
    }

    // ── Transaction with multiple repositories ────────────────────────────

    @PatchMapping("/reassign-category")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'ADMIN')")
    @Operation(summary = "Reassign multiple products to another category",
            description = "Transactional operation with rollback on error.")
    public ResponseEntity<Map<String, Object>> reassignProductsToCategory(
            @Valid @RequestBody CategoryReassignDTO dto) {
        return ResponseEntity.ok(productService.reassignProductsToCategory(dto));
    }
}
