package com.etema.attributemodify.editor;

import com.etema.attributemodify.durability.DurabilityRule;
import com.etema.attributemodify.handler.MiningTierHandler;
import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableDurabilityModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableMiningOverride;
import com.etema.attributemodify.editor.model.EditableSlotType;
import com.etema.attributemodify.editor.model.EditableValidationResult;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class EditorRuleValidator {
    private final EditorValidationContext context;

    public EditorRuleValidator(EditorValidationContext context) {
        this.context = context;
    }

    public EditableValidationResult validate(EditableItemRule rule) {
        EditableValidationResult result = new EditableValidationResult();
        if (rule == null) {
            result.error("Rule is missing.");
            return result;
        }

        if (rule.getTargetId() == null) {
            result.error("Item id is missing or invalid.");
        } else if (!rule.isTagTarget() && !context.itemIds().contains(rule.getTargetId())) {
            result.error("Item does not exist: " + rule.getTargetId());
        }

        validateAttributes(rule, result);
        validateDurability(rule.getDurability(), result);
        validateMining(rule, result);

        if (result.isValid()) {
            try {
                JsonObject ignored = EditorRuleSerializer.toItemObject(rule);
            } catch (RuntimeException e) {
                result.error("Rule cannot be serialized: " + e.getMessage());
            }
        }

        return result;
    }

    private void validateAttributes(EditableItemRule rule, EditableValidationResult result) {
        Set<String> duplicateKeys = new HashSet<>();
        for (EditableAttributeModifier attribute : rule.getAttributes()) {
            if (attribute == null) {
                result.error("Attribute entry is missing.");
                continue;
            }

            if (attribute.getAttributeId() == null) {
                result.error("Attribute id is missing or invalid.");
            } else if (!context.attributeIds().contains(attribute.getAttributeId())) {
                result.error("Attribute does not exist: " + attribute.getAttributeId());
            }

            EditableAttributeAction action = attribute.getAction();
            if (action == null) {
                result.error("Attribute action is invalid.");
            }

            if (action != EditableAttributeAction.REMOVE) {
                Double amount = attribute.getAmount();
                if (amount == null || !Double.isFinite(amount)) {
                    result.error("Attribute amount must be a finite number.");
                }
                if (action != EditableAttributeAction.SET && attribute.getOperation() == null) {
                    result.error("Attribute operation is invalid.");
                }
            }

            validateSlot(attribute, result);

            String duplicateKey = attribute.duplicateKey().toLowerCase(Locale.ROOT);
            if (!duplicateKeys.add(duplicateKey)) {
                result.error("Duplicate attribute for same operation and slot: " + attribute.getAttributeId());
            }
        }
    }

    private void validateSlot(EditableAttributeModifier attribute, EditableValidationResult result) {
        EditableSlotType slotType = attribute.getSlotType();
        if (slotType == EditableSlotType.AUTO) {
            return;
        }

        String slot = attribute.getSlot();
        if (slot == null || slot.isBlank()) {
            result.error("Slot is required for explicit slot entries.");
            return;
        }

        if (slotType == EditableSlotType.STANDARD && !context.hasEquipmentSlot(slot)) {
            result.error("Invalid equipment slot: " + slot);
        }
        if (slotType == EditableSlotType.CURIOS && !context.hasCuriosSlot(slot)) {
            result.error("Invalid Curios slot or Curios is not available: " + slot);
        }
        if (slotType == EditableSlotType.ACCESSORIES && !context.hasAccessoriesSlot(slot)) {
            result.error("Invalid Accessories slot or Accessories is not available: " + slot);
        }
    }

    private void validateDurability(EditableDurabilityModifier durability, EditableValidationResult result) {
        if (durability == null) {
            return;
        }

        Integer value = durability.getDurability();
        if (value == null || value < 1) {
            result.error("Durability must be at least 1.");
        }

        for (String trigger : durability.getTriggers()) {
            if (!DurabilityRule.isSupportedTrigger(trigger)) {
                result.error("Unsupported durability trigger: " + trigger);
            }
        }
    }

    private void validateMining(EditableItemRule rule, EditableValidationResult result) {
        for (EditableMiningOverride override : rule.getMiningOverrides()) {
            if (override == null) {
                result.error("Mining override is missing.");
                continue;
            }

            boolean hasSpeed = override.getSpeed() != null;
            boolean hasTier = override.getTier() != null && !override.getTier().isBlank();
            if (!hasSpeed && !hasTier) {
                result.error("Mining override must define speed, tier, or both.");
            }

            if (hasSpeed && (!Float.isFinite(override.getSpeed()) || override.getSpeed() < 0.0f)) {
                result.error("Mining speed must be a finite non-negative number.");
            }

            if (hasTier && !isSupportedMiningTier(override.getTier())) {
                result.error("Unsupported mining tier: " + override.getTier());
            }
        }
    }

    private boolean isSupportedMiningTier(String tier) {
        return context.hasMiningTier(MiningTierHandler.resolveTierId(tier));
    }
}
