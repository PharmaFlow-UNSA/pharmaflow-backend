package com.pharmaflow.pharmacyinventory.exception;

public class PatchOperationException extends RuntimeException {
    public PatchOperationException(String message) {
        super(message);
    }

    public PatchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
