package com.pharmaflow.orderprescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionCreateDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Image URL is required")
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String imageUrl;
}
