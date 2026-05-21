package com.pharmaflow.smartfeatures.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error ->
                        error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Invalid value",
                    (existing, replacement) -> existing + "; " + replacement,
                    LinkedHashMap::new));

    logger.warn("Validation error on {}: {}", request.getRequestURI(), fieldErrors);

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Validation Error",
        "VALIDATION_ERROR",
        "Request validation failed. Check 'errors' for details.",
        request.getRequestURI(),
        fieldErrors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    Map<String, String> violations =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage,
                    (existing, replacement) -> existing + "; " + replacement,
                    LinkedHashMap::new));

    logger.warn("Constraint violation on {}: {}", request.getRequestURI(), violations);

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Constraint Violation",
        "CONSTRAINT_VIOLATION",
        "Request constraints violated. Check 'errors' for details.",
        request.getRequestURI(),
        violations);
  }

  @ExceptionHandler(BaseApiException.class)
  public ResponseEntity<ApiErrorResponse> handleBaseApiException(
      BaseApiException ex, HttpServletRequest request) {
    logger.warn("Application error on {}: {}", request.getRequestURI(), ex.getMessage());

    return buildErrorResponse(
        ex.getStatus(),
        ex.getStatus().getReasonPhrase(),
        ex.getErrorCode(),
        ex.getMessage(),
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    logger.warn("Authentication failure on {}: {}", request.getRequestURI(), ex.getMessage());

    return buildErrorResponse(
        HttpStatus.UNAUTHORIZED,
        "Unauthorized",
        "UNAUTHORIZED",
        ex.getMessage(),
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    logger.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

    return buildErrorResponse(
        HttpStatus.FORBIDDEN,
        "Forbidden",
        "FORBIDDEN",
        ex.getMessage(),
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    String causeMessage =
        ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();
    logger.error("Data integrity violation on {}: {}", request.getRequestURI(), causeMessage);

    String message = "Database constraint violation. The operation violates data integrity rules.";
    if (causeMessage != null && causeMessage.toLowerCase().contains("normalized_name")) {
      message = "A symptom with the same name already exists.";
    } else if (causeMessage != null && causeMessage.toLowerCase().contains("normalized_question")) {
      message = "An FAQ entry with the same question already exists.";
    } else if (causeMessage != null
        && causeMessage.toLowerCase().contains("normalized_rule_name")) {
      message = "A fraud rule with the same name already exists.";
    } else if (causeMessage != null && causeMessage.toLowerCase().contains("rule_code")) {
      message = "A fraud rule with the same code already exists.";
    } else if (causeMessage != null
        && causeMessage.toLowerCase().contains("uk_symptom_search_item_search_symptom")) {
      message = "The symptom is already linked to this search.";
    } else if (causeMessage != null
        && causeMessage.toLowerCase().contains("uk_symptom_product_match_symptom_product")) {
      message = "The product is already matched to this symptom.";
    } else if (causeMessage != null
        && causeMessage.toLowerCase().contains("uk_recommendation_active_dimension_key")) {
      message = "An active recommendation with the same dimensions already exists.";
    } else if (causeMessage != null
        && causeMessage.toLowerCase().contains("active_dimension_key")) {
      message = "An active recommendation with the same dimensions already exists.";
    }

    return buildErrorResponse(
        HttpStatus.CONFLICT,
        "Data Integrity Violation",
        "DATA_INTEGRITY_VIOLATION",
        message,
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    logger.warn("Malformed JSON on {}: {}", request.getRequestURI(), ex.getMessage());

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Malformed Request",
        "MALFORMED_REQUEST",
        "Request body is malformed or contains invalid values.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParams(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    logger.warn("Missing parameter on {}: {}", request.getRequestURI(), ex.getParameterName());

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Missing Parameter",
        "MISSING_PARAMETER",
        "Required parameter '" + ex.getParameterName() + "' is missing.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    logger.warn("Type mismatch on {}: {}", request.getRequestURI(), ex.getMessage());

    String requiredType =
        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Invalid Parameter Type",
        "INVALID_PARAMETER_TYPE",
        "Parameter '" + ex.getName() + "' has invalid type. Expected " + requiredType + ".",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    logger.warn("Method not supported on {}: {}", request.getRequestURI(), ex.getMethod());

    return buildErrorResponse(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Method Not Allowed",
        "METHOD_NOT_ALLOWED",
        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    logger.warn("Unsupported media type on {}: {}", request.getRequestURI(), ex.getContentType());

    return buildErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Unsupported Media Type",
        "UNSUPPORTED_MEDIA_TYPE",
        "Media type '" + ex.getContentType() + "' is not supported.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    logger.error("Unexpected error on {}", request.getRequestURI(), ex);

    return buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred.",
        request.getRequestURI(),
        null);
  }

  private ResponseEntity<ApiErrorResponse> buildErrorResponse(
      HttpStatus status,
      String error,
      String errorCode,
      String message,
      String path,
      Map<String, String> errors) {
    ApiErrorResponse response =
        ApiErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .errorCode(errorCode)
            .message(message)
            .path(path)
            .errors(errors)
            .build();

    return ResponseEntity.status(status).body(response);
  }
}
