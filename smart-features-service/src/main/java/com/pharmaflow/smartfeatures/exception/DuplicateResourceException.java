package com.pharmaflow.smartfeatures.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseApiException {

  public DuplicateResourceException(String message) {
    super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
  }
}
