package com.pharmaflow.smartfeatures.dto.chat;

import com.pharmaflow.smartfeatures.enums.chat.ChatSessionType;
import com.pharmaflow.smartfeatures.validation.NullablePositive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionRequestDto {

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be positive")
    private Long userId;

    @NullablePositive(message = "patientProfileId must be positive")
    private Long patientProfileId;

    @NotNull(message = "sessionType is required")
    private ChatSessionType sessionType;
}
