package com.pharmaflow.producthealth.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String barcode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Brend naziv (npr. "Brufen") vs generički (npr. "Ibuprofen 400mg")
    @Column(name = "brand_name")
    private String brandName;

    @Column(nullable = false)
    private String manufacturer;

    // Da li je potreban recept
    @Column(name = "requires_prescription", nullable = false)
    private Boolean requiresPrescription = false;

    // Da li je lijek ili OTC (over-the-counter) proizvod
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Veličina pakovanja (npr. "20 tableta", "200ml")
    @Column(name = "package_size")
    private String packageSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Aktivne supstance u lijeku (many-to-many)
    @ManyToMany
    @JoinTable(
            name = "product_substances",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "substance_id")
    )
    private List<Substance> substances;

    // Zamjenski lijekovi (self-referencing)
    @OneToMany(mappedBy = "originalProduct", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductSubstitute> substitutes;

    public enum ProductType {
        MEDICATION,       // Lijek (sa ili bez recepta)
        SUPPLEMENT,       // Vitamini, suplementi
        COSMETIC,         // Kozmetika
        MEDICAL_DEVICE,   // Medicinski uređaj
        OTHER
    }
}