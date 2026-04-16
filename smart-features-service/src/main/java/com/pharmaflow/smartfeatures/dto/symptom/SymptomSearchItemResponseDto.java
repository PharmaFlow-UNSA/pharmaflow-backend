package com.pharmaflow.smartfeatures.dto.symptom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SymptomSearchItemResponseDto {

    private Long id;
    private Long searchId;
    private Long symptomId;
    private String symptomName;
}
