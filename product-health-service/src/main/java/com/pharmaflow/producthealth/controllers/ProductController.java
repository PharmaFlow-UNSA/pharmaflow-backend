package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.ProductCreateDTO;
import com.pharmaflow.producthealth.dto.ProductDTO;
import com.pharmaflow.producthealth.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "API za upravljanje farmaceutskim proizvodima")
public class ProductController {

    private final ProductService productService;

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
    @Operation(summary = "Pretraži proizvode po nazivu")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Dohvati proizvode po kategoriji")
    public ResponseEntity<List<ProductDTO>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/substance/{substanceId}")
    @Operation(summary = "Dohvati proizvode koji sadrže datu supstancu")
    public ResponseEntity<List<ProductDTO>> getBySubstance(@PathVariable Long substanceId) {
        return ResponseEntity.ok(productService.getProductsBySubstance(substanceId));
    }

    @PostMapping
    @Operation(summary = "Kreiraj novi proizvod")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductCreateDTO dto) {
        return new ResponseEntity<>(productService.createProduct(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ažuriraj postojeći proizvod")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id,
                                                     @Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deaktiviraj proizvod")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Obriši proizvod")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
