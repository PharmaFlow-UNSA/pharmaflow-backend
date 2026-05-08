package com.pharmaflow.smartfeatures.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiErrorResponse {

  private final LocalDateTime timestamp;
  private final int status;
  private final String error;
  private final String errorCode;
  private final String message;
  private final String path;
  private final Map<String, String> errors;
}
