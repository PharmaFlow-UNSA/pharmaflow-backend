package com.pharmaflow.smartfeatures.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmaflow.smartfeatures.enums.chat.FaqCategory;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaqEntryResponseDto {

  private Long id;
  private String question;
  private String answer;
  private FaqCategory category;
  private String keywords;

  @JsonProperty("isActive")
  private Boolean active;

  private LocalDateTime updatedAt;
}
