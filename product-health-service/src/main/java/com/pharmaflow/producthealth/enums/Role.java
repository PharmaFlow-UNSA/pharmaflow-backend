package com.pharmaflow.producthealth.enums;

/**
 * User roles with hierarchical permissions.
 * ADMIN > PHARMACIST > DOCTOR > USER
 * Must match roles defined in user-health-service.
 */
public enum Role {
    ROLE_USER,          // Basic user - can view products, categories, substances
    ROLE_DOCTOR,        // Can view drug interactions and contraindications
    ROLE_PHARMACIST,    // Can create/update products, categories, substances
    ROLE_ADMIN          // Full access - can delete and manage everything
}
