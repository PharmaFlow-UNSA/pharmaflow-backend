package com.pharmaflow.smartfeatures.dto.symptom;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SymptomSearchResponseDto {

    private Long id;
    private Long userId;
    private Long patientProfileId;
    private String searchQuery;
    private LocalDateTime searchedAt;
}
