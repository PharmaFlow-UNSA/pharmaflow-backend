package com.pharmaflow.smartfeatures.dto.fraud.external;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExternalPrescriptionDto {

  private Long id;
  private Long userId;
  private String imageUrl;
  private String status;
  private LocalDateTime uploadedAt;
  private LocalDateTime reviewedAt;
  private String reviewerNotes;
  private List<Long> orderIds = new ArrayList<>();
  private List<Long> autoRefillSubscriptionIds = new ArrayList<>();
}
