package com.etema.attributemodify.editor.model;

import java.util.Locale;

public enum EditableAttributeAction {
    ADD("add"),
    MODIFY("modify"),
    SET("set"),
    REMOVE("remove");

    private final String serializedName;

    EditableAttributeAction(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static EditableAttributeAction fromString(String value) {
        if (value == null || value.isBlank()) {
            return ADD;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "add" -> ADD;
            case "modify" -> MODIFY;
            case "set", "exact" -> SET;
            case "remove" -> REMOVE;
            default -> null;
        };
    }
}
