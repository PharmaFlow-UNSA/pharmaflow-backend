package com.pharmaflow.producthealth.controllers;

import com.pharmaflow.producthealth.dto.ProductSubstituteDTO;
import com.pharmaflow.producthealth.services.ProductSubstituteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/substitutes")
@RequiredArgsConstructor
@Tag(name = "Product Substitutes", description = "API for managing product substitutes")
public class ProductSubstituteController {

    private final ProductSubstituteService productSubstituteService;

    @GetMapping
    @Operation(summary = "Get all substitutes")
    public ResponseEntity<List<ProductSubstituteDTO>> getAllSubstitutes() {
        return ResponseEntity.ok(productSubstituteService.getAllSubstitutes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get substitute by ID")
    public ResponseEntity<ProductSubstituteDTO> getSubstituteById(@PathVariable Long id) {
        return ResponseEntity.ok(productSubstituteService.getSubstituteById(id));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all substitutes for a given product")
    public ResponseEntity<List<ProductSubstituteDTO>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productSubstituteService.getSubstitutesByProduct(productId));
    }

    @PostMapping
    @Operation(summary = "Create a new substitute")
    public ResponseEntity<ProductSubstituteDTO> createSubstitute(@Valid @RequestBody ProductSubstituteDTO dto) {
        return new ResponseEntity<>(productSubstituteService.createSubstitute(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update substitute")
    public ResponseEntity<ProductSubstituteDTO> updateSubstitute(@PathVariable Long id,
                                                                 @Valid @RequestBody ProductSubstituteDTO dto) {
        return ResponseEntity.ok(productSubstituteService.updateSubstitute(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete substitute")
    public ResponseEntity<Void> deleteSubstitute(@PathVariable Long id) {
        productSubstituteService.deleteSubstitute(id);
        return ResponseEntity.noContent().build();
    }
}