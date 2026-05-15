package com.etema.attributemodify.editor.model;

import java.util.LinkedHashSet;
import java.util.Set;

public final class EditableDurabilityModifier {
    private Integer durability;
    private final Set<String> triggers = new LinkedHashSet<>();

    public EditableDurabilityModifier(Integer durability) {
        this.durability = durability;
    }

    public Integer getDurability() {
        return durability;
    }

    public void setDurability(Integer durability) {
        this.durability = durability;
    }

    public Set<String> getTriggers() {
        return triggers;
    }

    public EditableDurabilityModifier copy() {
        EditableDurabilityModifier copy = new EditableDurabilityModifier(durability);
        copy.getTriggers().addAll(triggers);
        return copy;
    }
}
