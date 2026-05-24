package com.pharmaflow.smartfeatures.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {

  private List<T> content;
  private long totalElements;
  private int totalPages;
  private int number;
  private int size;
  private boolean first;
  private boolean last;
}
