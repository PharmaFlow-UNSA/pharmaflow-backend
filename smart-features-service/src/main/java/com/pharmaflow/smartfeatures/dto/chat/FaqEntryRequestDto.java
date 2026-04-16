package com.pharmaflow.smartfeatures.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import com.pharmaflow.smartfeatures.validation.SanitizedTextSize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaqEntryRequestDto {

    @SanitizedTextSize(
            min = 5,
            max = 200,
            allowBlank = false,
            message = "question must be between 5 and 200 characters after trimming")
    private String question;

    @SanitizedTextSize(
            min = 5,
            max = 2000,
            allowBlank = false,
            message = "answer must be between 5 and 2000 characters after trimming")
    private String answer;

    @NotNull(message = "category is required")
    private FaqCategory category;

    @SanitizedTextSize(max = 500, message = "keywords must not exceed 500 characters")
    private String keywords;

    @JsonProperty("isActive")
    @NotNull(message = "isActive is required")
    private Boolean active;
}
