package com.etema.attributemodify.editor.model;

public final class EditableMiningOverride {
    private Float speed;
    private String tier;
    private EditableCondition condition;

    public EditableMiningOverride(Float speed, String tier) {
        this.speed = speed;
        this.tier = tier;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public EditableCondition getCondition() {
        return condition;
    }

    public void setCondition(EditableCondition condition) {
        this.condition = condition;
    }

    public EditableMiningOverride copy() {
        EditableMiningOverride copy = new EditableMiningOverride(speed, tier);
        copy.setCondition(condition == null ? null : condition.copy());
        return copy;
    }
}
