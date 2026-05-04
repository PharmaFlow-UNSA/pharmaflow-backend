package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.*;
import com.pharmaflow.producthealth.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Products", description = "API za upravljanje farmaceutskim proizvodima")
public class ProductController {

    private final ProductService productService;

    // ── Osnovni CRUD ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Dohvati sve aktivne proizvode")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dohvati proizvod po ID-u")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Pretrazi proizvode po nazivu")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Dohvati proizvode po kategoriji")
    public ResponseEntity<List<ProductDTO>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Dohvati proizvode koji sadrze datu supstancu")
    public ResponseEntity<List<ProductDTO>> getBySubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(productService.getProductsBySubstance(substanceId));
    }

    @PostMapping
    @Operation(summary = "Kreiraj novi proizvod")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        return new ResponseEntity<>(productService.createProduct(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Azuriraj postojeci proizvod (potpuna zamjena)")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id,
                                                     @Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deaktiviraj proizvod (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Trajno obrisi proizvod")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH: parcijalno azuriranje ──────────────────────────────────────

    @PatchMapping("/{id}")
    @Operation(summary = "Parcijalno azuriraj proizvod - samo proslijedjena polja se mijenjaju")
    public ResponseEntity<ProductDTO> patchProduct(@PathVariable Long id,
                                                    @Valid @RequestBody ProductPatchDTO dto) {
        return ResponseEntity.ok(productService.patchProduct(id, dto));
    }

    // ── Paginacija i sortiranje ───────────────────────────────────────────

    @GetMapping("/page")
    @Operation(summary = "Dohvati aktivne proizvode sa paginacijom i sortiranjem",
               description = "Parametri: page (0-based), size, sortBy (name/price/manufacturer), direction (asc/desc)")
    public ResponseEntity<ProductPageDTO> getProductsPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(productService.getProductsPageable(page, size, sortBy, direction));
    }

    @GetMapping("/category/{categoryId}/page")
    @Operation(summary = "Dohvati proizvode kategorije sa paginacijom i sortiranjem")
    public ResponseEntity<ProductPageDTO> getProductsByCategoryPageable(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(
                productService.getProductsByCategoryPageable(categoryId, page, size, sortBy, direction));
    }

    // ── Custom upiti ──────────────────────────────────────────────────────

    @GetMapping("/price-range")
    @Operation(summary = "Pretrazi proizvode po opsegu cijena",
               description = "Primjer: /api/products/price-range?minPrice=2.00&maxPrice=10.00")
    public ResponseEntity<List<ProductDTO>> getByPriceRange(
            @RequestParam @DecimalMin(value = "0.0", message = "Minimalna cijena mora biti >= 0") BigDecimal minPrice,
            @RequestParam @DecimalMin(value = "0.0", message = "Maksimalna cijena mora biti >= 0") BigDecimal maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filtriraj proizvode po tipu i potrebi za receptom",
               description = "Primjer: /api/products/filter?type=MEDICATION&requiresPrescription=false")
    public ResponseEntity<List<ProductDTO>> getByTypeAndPrescription(
            @RequestParam String type,
            @RequestParam Boolean requiresPrescription) {
        return ResponseEntity.ok(productService.getProductsByTypeAndPrescription(type, requiresPrescription));
    }

    @GetMapping("/otc")
    @Operation(summary = "Dohvati sve OTC proizvode sortirane po cijeni uzlazno")
    public ResponseEntity<List<ProductDTO>> getOtcProducts() {
        return ResponseEntity.ok(productService.getOtcProductsSortedByPrice());
    }

    @GetMapping("/stats/count-by-type")
    @Operation(summary = "Statistika - broj aktivnih proizvoda po tipu")
    public ResponseEntity<Map<String, Long>> getCountByType() {
        return ResponseEntity.ok(productService.getProductCountByType());
    }

    // ── Batch unos ────────────────────────────────────────────────────────

    @PostMapping("/batch")
    @Operation(summary = "Batch unos vise proizvoda u jednoj transakciji",
               description = "Ako bilo koji proizvod ne prode validaciju, cijeli batch se rollback-uje. Max 50 proizvoda.")
    public ResponseEntity<List<ProductDTO>> createProductsBatch(@Valid @RequestBody ProductBatchDTO batchDTO) {
        return new ResponseEntity<>(productService.createProductsBatch(batchDTO), HttpStatus.CREATED);
    }

    // ── Transakcija sa vise repozitorija ──────────────────────────────────

    @PatchMapping("/reassign-category")
    @Operation(summary = "Premjesti vise proizvoda u drugu kategoriju",
               description = "Transakcijska operacija: validira kategoriju i sve proizvode, zatim radi bulk update. Rollback na bilo kojoj gresci.")
    public ResponseEntity<Map<String, Object>> reassignProductsToCategory(
            @Valid @RequestBody CategoryReassignDTO dto) {
        return ResponseEntity.ok(productService.reassignProductsToCategory(dto));
    }
}
