package com.pharmaflow.producthealth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubstanceDTO {

    private Long id;

    @NotBlank(message = "INN (International Nonproprietary Name) is required")
    @Size(min = 2, max = 100, message = "INN must be between 2 and 100 characters")
    private String inn;

    @Size(max = 150, message = "Common name must not exceed 150 characters")
    private String commonName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$|^$",
             message = "ATC code must follow the format: letter + 2 digits + 2 letters + 2 digits (e.g. N02BE01)")
    private String atcCode;
}
