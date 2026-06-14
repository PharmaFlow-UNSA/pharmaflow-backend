package com.pharmaflow.pharmacyinventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyCreateDTO {

    @NotBlank(message = "Pharmacy name is required")
    @Size(min = 2, max = 100, message = "Pharmacy name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$", message = "City can only contain letters, spaces, hyphens and apostrophes")
    private String city;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[0-9\\s-]{7,20}$", message = "Phone number must be a valid format")
    private String phoneNumber;

    @Email(message = "Email must be valid", regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Opening hours must not exceed 50 characters")
    private String openingHours;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
}
