package com.pharmaflow.userhealth.dto;

import com.pharmaflow.userhealth.models.enums.BloodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDTO {

    private Long id;

    @DecimalMin(value = "0.5", message = "Weight must be at least 0.5 kg")
    @DecimalMax(value = "500.0", message = "Weight must not exceed 500 kg")
    private Double weight;

    @DecimalMin(value = "20.0", message = "Height must be at least 20 cm")
    @DecimalMax(value = "300.0", message = "Height must not exceed 300 cm")
    private Double height;

    private BloodType bloodType;

    @Valid
    private List<AllergyDTO> allergies = new ArrayList<>();

    @Valid
    private List<TherapyDTO> therapies = new ArrayList<>();
}

