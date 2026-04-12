package com.etema.attributemodify.service;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service specialized in applying resolved attribute rules to the Forge event.
 */
public class AttributeApplicationService {

    /**
     * Applies a list of resolved entries to the modifier event.
     */
    public static void applyRules(ItemAttributeModifierEvent event, List<ItemAttributeDataManager.AttributeEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers = new ArrayList<>(event.getModifiers().entries());

        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            Attribute attribute = entry.attribute();
            if (attribute == null) continue;

            switch (entry.action()) {
                case REMOVE -> finalModifiers.removeIf(e -> e.getKey().equals(attribute));
                case MODIFY -> applyModifyRule(event, attribute, entry.modifier(), finalModifiers);
                case ADD -> applyAddRule(event, attribute, entry.modifier(), finalModifiers);
            }
        }

        event.clearModifiers();
        for (java.util.Map.Entry<Attribute, AttributeModifier> e : finalModifiers) {
            try {
                event.addModifier(e.getKey(), e.getValue());
            } catch (Exception ex) {
                AttributeModify.LOGGER.error("[apply] Failed to apply {} on {} in slot {}: {}",
                        e.getKey().getDescriptionId(), event.getItemStack().getItem(), event.getSlotType(), ex.getMessage(), ex);
            }
        }
    }

    private static void applyModifyRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier, List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.warn("[apply] MODIFY {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers.isEmpty()) {
            AttributeModify.LOGGER.warn("[semantic] MODIFY {} on {} in slot {} found no original item modifier to replace",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        Set<UUID> originalIds = new HashSet<>();
        for (AttributeModifier original : originalModifiers) {
            originalIds.add(original.getId());
        }

        boolean anyModified = false;
        for (int i = 0; i < finalModifiers.size(); i++) {
            java.util.Map.Entry<Attribute, AttributeModifier> e = finalModifiers.get(i);
            if (e.getKey().equals(attribute) && originalIds.contains(e.getValue().getId())) {
                finalModifiers.set(i, java.util.Map.entry(attribute, new AttributeModifier(
                        e.getValue().getId(),
                        e.getValue().getName(),
                        dataModifier.getAmount(),
                        dataModifier.getOperation()
                )));
                anyModified = true;
            }
        }

        if (!anyModified) {
            AttributeModify.LOGGER.warn("[semantic] MODIFY {} on {} in slot {} found modifiers, but none matched the original item identity",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
        }
    }

    private static void applyAddRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier, List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.warn("[apply] ADD {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        boolean merged = false;
        if (dataModifier.getOperation() == AttributeModifier.Operation.ADDITION) {
            Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
            AttributeModifier targetToMerge = null;
            for (AttributeModifier original : originalModifiers) {
                if (original.getOperation() == AttributeModifier.Operation.ADDITION) {
                    targetToMerge = original;
                    break;
                }
            }

            if (targetToMerge != null) {
                for (int i = 0; i < finalModifiers.size(); i++) {
                    java.util.Map.Entry<Attribute, AttributeModifier> e = finalModifiers.get(i);
                    if (e.getKey().equals(attribute) && e.getValue().getId().equals(targetToMerge.getId())) {
                        finalModifiers.set(i, java.util.Map.entry(attribute, new AttributeModifier(
                                e.getValue().getId(),
                                e.getValue().getName(),
                                e.getValue().getAmount() + dataModifier.getAmount(),
                                e.getValue().getOperation()
                        )));
                        merged = true;
                        break;
                    }
                }
            }
        }

        if (!merged) {
            finalModifiers.add(java.util.Map.entry(attribute, dataModifier));
        }
    }
}
