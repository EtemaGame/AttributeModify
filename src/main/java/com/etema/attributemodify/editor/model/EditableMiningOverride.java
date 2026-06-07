package com.etema.attributemodify.editor.model;

public final class EditableMiningOverride {
    private Float speed;
    private String tier;

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

    public EditableMiningOverride copy() {
        return new EditableMiningOverride(speed, tier);
    }
}
