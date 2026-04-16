package com.pharmaflow.orderprescription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Image URL is required")
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String imageUrl;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|APPROVED|REJECTED)$",
            message = "Status must be one of: PENDING, APPROVED, REJECTED")
    private String status;

    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    @Size(max = 500, message = "Reviewer notes must not exceed 500 characters")
    private String reviewerNotes;

    private List<Long> orderIds = new ArrayList<>();

    private List<Long> autoRefillSubscriptionIds = new ArrayList<>();
}
