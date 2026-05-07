package com.pharmaflow.smartfeatures.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BaseApiException {

  public ExternalServiceException(String message) {
    super(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_UNAVAILABLE", message);
  }
}
