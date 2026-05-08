package com.pharmaflow.userhealth.enums;

/**
 * User roles with hierarchical permissions.
 * ADMIN > PHARMACIST > DOCTOR > USER
 */
public enum Role {
    ROLE_USER,          // Basic user - can view own data
    ROLE_DOCTOR,        // Can access patient health profiles
    ROLE_PHARMACIST,    // Can access inventory and prescriptions
    ROLE_ADMIN          // Full access to all services
}

