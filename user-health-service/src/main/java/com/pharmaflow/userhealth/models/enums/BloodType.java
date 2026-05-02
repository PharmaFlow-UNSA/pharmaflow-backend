package com.pharmaflow.userhealth.models.enums;

public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String displayName;

    BloodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BloodType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (BloodType type : BloodType.values()) {
            if (type.displayName.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown blood type: " + value);
    }
}

