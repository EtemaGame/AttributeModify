package com.etema.attributemodify.editor.model;

import java.util.Locale;

public enum EditableOperationType {
    ADDITION("addition"),
    MULTIPLY_BASE("multiply_base"),
    MULTIPLY_TOTAL("multiply_total");

    private final String serializedName;

    EditableOperationType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static EditableOperationType fromString(String value) {
        if (value == null || value.isBlank()) {
            return ADDITION;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "addition", "add", "add_value" -> ADDITION;
            case "multiply_base", "multiply", "add_multiplied_base" -> MULTIPLY_BASE;
            case "multiply_total", "add_multiplied_total" -> MULTIPLY_TOTAL;
            default -> null;
        };
    }
}
