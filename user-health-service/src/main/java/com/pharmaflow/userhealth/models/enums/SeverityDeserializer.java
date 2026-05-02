package com.pharmaflow.userhealth.models.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class SeverityDeserializer extends JsonDeserializer<Severity> {
    @Override
    public Severity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null) return null;
        for (Severity s : Severity.values()) {
            if (s.name().equalsIgnoreCase(value) || s.getDisplayName().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown severity: " + value);
    }
}

