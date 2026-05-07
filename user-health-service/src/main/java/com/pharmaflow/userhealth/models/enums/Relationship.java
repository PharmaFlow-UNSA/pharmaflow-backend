package com.pharmaflow.userhealth.models.enums;

public enum Relationship {
    SPOUSE("Spouse"),
    CHILD("Child"),
    PARENT("Parent"),
    SIBLING("Sibling"),
    GRANDPARENT("Grandparent"),
    GRANDCHILD("Grandchild"),
    OTHER("Other");

    private final String displayName;

    Relationship(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

