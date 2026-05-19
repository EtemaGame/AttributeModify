package com.etema.attributemodify.editor.model;

import com.google.gson.JsonElement;

public final class EditableQualityLevel {
    private JsonElement value;
    private int weight;

    public EditableQualityLevel(JsonElement value, int weight) {
        this.value = value;
        this.weight = weight;
    }

    public JsonElement getValue() {
        return value;
    }

    public void setValue(JsonElement value) {
        this.value = value;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public EditableQualityLevel copy() {
        return new EditableQualityLevel(value == null ? null : value.deepCopy(), weight);
    }
}
