package com.etema.attributemodify.editor.model;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public final class EditableAttributeModifier {
    private ResourceLocation attributeId;
    private EditableAttributeAction action;
    private Double amount;
    private EditableOperationType operation;
    private EditableSlotType slotType;
    private String slot;
    private String modifierId;
    private UUID uuid;
    private EditableCondition condition;

    public EditableAttributeModifier(ResourceLocation attributeId, EditableAttributeAction action, Double amount,
            EditableOperationType operation, EditableSlotType slotType, String slot) {
        this.attributeId = attributeId;
        this.action = action == null ? EditableAttributeAction.ADD : action;
        this.amount = amount;
        this.operation = operation == null ? EditableOperationType.ADDITION : operation;
        this.slotType = slotType == null ? EditableSlotType.AUTO : slotType;
        this.slot = slot;
    }

    public ResourceLocation getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(ResourceLocation attributeId) {
        this.attributeId = attributeId;
    }

    public EditableAttributeAction getAction() {
        return action;
    }

    public void setAction(EditableAttributeAction action) {
        this.action = action == null ? EditableAttributeAction.ADD : action;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public EditableOperationType getOperation() {
        return operation;
    }

    public void setOperation(EditableOperationType operation) {
        this.operation = operation == null ? EditableOperationType.ADDITION : operation;
    }

    public EditableSlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(EditableSlotType slotType) {
        this.slotType = slotType == null ? EditableSlotType.AUTO : slotType;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getModifierId() {
        return modifierId;
    }

    public void setModifierId(String modifierId) {
        this.modifierId = modifierId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public EditableCondition getCondition() {
        return condition;
    }

    public void setCondition(EditableCondition condition) {
        this.condition = condition;
    }

    public EditableAttributeModifier copy() {
        EditableAttributeModifier copy = new EditableAttributeModifier(attributeId, action, amount, operation, slotType, slot);
        copy.setModifierId(modifierId);
        copy.setUuid(uuid);
        copy.setCondition(condition == null ? null : condition.copy());
        return copy;
    }

    public String duplicateKey() {
        return Objects.toString(attributeId, "") + "|" + Objects.toString(operation, "") + "|"
                + Objects.toString(slotType, "") + "|" + Objects.toString(slot, "");
    }
}
