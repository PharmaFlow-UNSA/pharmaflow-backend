package com.pharmaflow.pharmacyinventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing + "; " + replacement
                ));

        logger.warn("Validation error on {}: {}", request.getRequestURI(), fieldErrors);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Request validation failed. Check 'errors' for details.",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        Map<String, String> violations = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        logger.warn("Constraint violation on {}: {}", request.getRequestURI(), violations);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                "Request constraints violated. Check 'errors' for details.",
                request.getRequestURI(),
                violations
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        logger.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        logger.warn("Duplicate resource on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Duplicate Resource",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(PatchOperationException.class)
    public ResponseEntity<Map<String, Object>> handlePatchOperation(
            PatchOperationException ex,
            HttpServletRequest request) {

        logger.warn("Patch operation error on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Patch Operation Failed",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        logger.error("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        String message = "Database constraint violation. The operation violates data integrity rules.";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            if (ex.getCause().getMessage().contains("unique constraint") ||
                ex.getCause().getMessage().contains("Unique index")) {
                message = "A record with this value already exists.";
            }
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Data Integrity Violation",
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        logger.warn("Malformed JSON on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                "Request body is malformed or invalid JSON format.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        logger.warn("Missing parameter on {}: {}", request.getRequestURI(), ex.getParameterName());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                String.format("Required parameter '%s' is missing.", ex.getParameterName()),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        logger.warn("Type mismatch on {}: {} should be {}",
                request.getRequestURI(), ex.getName(), ex.getRequiredType());

        String message = String.format("Parameter '%s' has invalid type. Expected %s.",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter Type",
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        logger.warn("Method not supported on {}: {}", request.getRequestURI(), ex.getMethod());

        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                ex.getSupportedHttpMethods());

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        logger.warn("Unsupported media type on {}: {}", request.getRequestURI(), ex.getContentType());

        String message = String.format("Media type '%s' is not supported. Supported types: %s",
                ex.getContentType(),
                ex.getSupportedMediaTypes());

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        logger.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint Not Found",
                String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        logger.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Argument",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        logger.error("Unexpected error on {}: ", request.getRequestURI(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists.",
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            Map<String, String> errors) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().format(formatter));
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", path);

        if (errors != null && !errors.isEmpty()) {
            response.put("errors", errors);
        }

        return new ResponseEntity<>(response, status);
    }
}
