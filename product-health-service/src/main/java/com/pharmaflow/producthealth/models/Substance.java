package com.pharmaflow.producthealth.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "substances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Substance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // INN (International Nonproprietary Name) npr. "ibuprofen", "paracetamol"
    @NotBlank
    @Column(nullable = false, unique = true)
    private String inn;

    @Column(name = "common_name")
    private String commonName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ATC kod za klasifikaciju (npr. "N02BE01" za paracetamol)
    @Column(name = "atc_code")
    private String atcCode;

    @ManyToMany(mappedBy = "substances")
    private List<Product> products;
}