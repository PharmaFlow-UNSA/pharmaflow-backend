package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO za transakcijsko premještanje više proizvoda u drugu kategoriju.
 * Koristi se u servisu koji poziva više repository metoda u jednoj transakciji.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReassignDTO {

    @NotEmpty(message = "Lista ID-eva proizvoda ne smije biti prazna")
    private List<@Positive(message = "ID proizvoda mora biti pozitivan") Long> productIds;

    @NotNull(message = "ID ciljne kategorije je obavezan")
    @Positive(message = "ID kategorije mora biti pozitivan broj")
    private Long targetCategoryId;
}
