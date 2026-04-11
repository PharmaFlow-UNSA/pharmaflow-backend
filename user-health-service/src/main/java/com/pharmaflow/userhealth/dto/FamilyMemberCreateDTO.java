package com.pharmaflow.userhealth.dto;

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
public class FamilyMemberCreateDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$",
             message = "First name can only contain letters, spaces, hyphens and apostrophes")
    private String firstName;

    @NotBlank(message = "Relationship is required")
    @Size(min = 2, max = 50, message = "Relationship must be between 2 and 50 characters")
    @Pattern(regexp = "^(Parent|Child|Sibling|Spouse|Partner|Grandparent|Grandchild|Other)$",
             message = "Relationship must be one of: Parent, Child, Sibling, Spouse, Partner, Grandparent, Grandchild, Other")
    private String relationship;

    @Valid
    private PatientProfileDTO patientProfile;

    @NotNull(message = "User ID is required")
    private Long userId;
}

