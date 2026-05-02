
package com.pharmaflow.userhealth.models.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SeverityDeserializer.class)
public enum Severity {
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    SEVERE("Severe"),
    LIFE_THREATENING("Life-Threatening");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

