package com.pharmaflow.userhealth.dto;

import com.pharmaflow.userhealth.models.enums.Relationship;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$",
             message = "First name can only contain letters, spaces, hyphens and apostrophes")
    private String firstName;

    @NotNull(message = "Relationship is required")
    private Relationship relationship;

    @Valid
    private PatientProfileDTO patientProfile;

    private Long userId;
}

