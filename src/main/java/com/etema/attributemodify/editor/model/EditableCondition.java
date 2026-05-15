package com.etema.attributemodify.editor.model;

import com.google.gson.JsonElement;

public final class EditableCondition {
    private String path;
    private String operator;
    private JsonElement value;

    public EditableCondition(String path, String operator, JsonElement value) {
        this.path = path;
        this.operator = operator == null || operator.isBlank() ? "equals" : operator;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator == null || operator.isBlank() ? "equals" : operator;
    }

    public JsonElement getValue() {
        return value;
    }

    public void setValue(JsonElement value) {
        this.value = value;
    }

    public EditableCondition copy() {
        return new EditableCondition(path, operator, value == null ? null : value.deepCopy());
    }
}
