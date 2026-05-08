package com.pharmaflow.userhealth.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fine-grained permissions for role-based access control.
 * Each role has a set of permissions.
 */
public enum Permission {
    // User permissions
    USER_READ,
    USER_WRITE,
    USER_DELETE,

    // Health profile permissions
    HEALTH_READ,
    HEALTH_WRITE,

    // Prescription permissions
    PRESCRIPTION_READ,
    PRESCRIPTION_WRITE,
    PRESCRIPTION_APPROVE,

    // Inventory permissions
    INVENTORY_READ,
    INVENTORY_WRITE,

    // Order permissions
    ORDER_READ,
    ORDER_WRITE,
    ORDER_CANCEL,

    // Admin permissions
    ADMIN_ACCESS;

    /**
     * Get permissions for a given role.
     */
    public static Set<Permission> getPermissionsForRole(Role role) {
        return switch (role) {
            case ROLE_ADMIN -> Set.of(values()); // All permissions
            case ROLE_DOCTOR -> Set.of(
                USER_READ, HEALTH_READ, HEALTH_WRITE,
                PRESCRIPTION_READ, PRESCRIPTION_WRITE, PRESCRIPTION_APPROVE,
                ORDER_READ
            );
            case ROLE_PHARMACIST -> Set.of(
                USER_READ, PRESCRIPTION_READ,
                INVENTORY_READ, INVENTORY_WRITE,
                ORDER_READ, ORDER_WRITE
            );
            case ROLE_USER -> Set.of(
                USER_READ, HEALTH_READ, PRESCRIPTION_READ, ORDER_READ
            );
        };
    }

    /**
     * Get all permissions for a list of roles.
     */
    public static Set<Permission> getPermissionsForRoles(List<String> roleStrings) {
        return roleStrings.stream()
            .map(Role::valueOf)
            .flatMap(role -> getPermissionsForRole(role).stream())
            .collect(Collectors.toSet());
    }
}

