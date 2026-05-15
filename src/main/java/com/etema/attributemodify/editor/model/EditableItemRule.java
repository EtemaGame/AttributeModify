package com.etema.attributemodify.editor.model;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class EditableItemRule {
    private ResourceLocation targetId;
    private boolean tagTarget;
    private final List<EditableAttributeModifier> attributes = new ArrayList<>();
    private EditableDurabilityModifier durability;

    public EditableItemRule(ResourceLocation targetId, boolean tagTarget) {
        this.targetId = targetId;
        this.tagTarget = tagTarget;
    }

    public ResourceLocation getTargetId() {
        return targetId;
    }

    public void setTargetId(ResourceLocation targetId) {
        this.targetId = targetId;
    }

    public boolean isTagTarget() {
        return tagTarget;
    }

    public void setTagTarget(boolean tagTarget) {
        this.tagTarget = tagTarget;
    }

    public List<EditableAttributeModifier> getAttributes() {
        return attributes;
    }

    public EditableDurabilityModifier getDurability() {
        return durability;
    }

    public void setDurability(EditableDurabilityModifier durability) {
        this.durability = durability;
    }

    public String serializedTargetKey() {
        return tagTarget ? "#" + targetId : targetId.toString();
    }

    public EditableItemRule copy() {
        EditableItemRule copy = new EditableItemRule(targetId, tagTarget);
        for (EditableAttributeModifier attribute : attributes) {
            copy.getAttributes().add(attribute.copy());
        }
        copy.setDurability(durability == null ? null : durability.copy());
        return copy;
    }
}
