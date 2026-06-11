package com.etema.attributemodify.service;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

import java.util.Collection;
import java.util.List;

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

        boolean preserveExternalOverrides = hasAttributeModifiersOverride(event.getItemStack());

        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            if (entry.action() == ItemAttributeDataManager.AttributeAction.SET) {
                continue;
            }

            Attribute attribute = entry.attribute();
            if (attribute == null) continue;

            switch (entry.action()) {
                case REMOVE -> {
                    if (preserveExternalOverrides) {
                        AttributeModify.LOGGER.debug("[apply] Skipping REMOVE {} on {} in slot {} because the stack already carries AttributeModifiers",
                                attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
                    } else {
                        applyRemoveRule(event, attribute);
                    }
                }
                case MODIFY -> {
                    if (preserveExternalOverrides) {
                        AttributeModify.LOGGER.debug("[apply] Skipping MODIFY {} on {} in slot {} because the stack already carries AttributeModifiers",
                                attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
                    } else {
                        applyModifyRule(event, attribute, entry.modifier());
                    }
                }
                case ADD -> applyAddRule(event, attribute, entry.modifier(), preserveExternalOverrides);
                case SET -> {
                    // handled in the second pass
                }
            }
        }

        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            if (entry.action() != ItemAttributeDataManager.AttributeAction.SET) {
                continue;
            }

            Attribute attribute = entry.attribute();
            if (attribute == null) {
                continue;
            }

            if (preserveExternalOverrides) {
                AttributeModify.LOGGER.debug("[apply] Skipping SET {} on {} in slot {} because the stack already carries AttributeModifiers",
                        attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
                continue;
            }

            applySetRule(event, attribute, entry.modifier());
        }
    }

    private static boolean hasAttributeModifiersOverride(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return false;
        }

        // If the stack already carries attribute overrides, stay conservative so we do not
        // flatten or replace modifiers injected by other systems.
        return stack.getTag().contains("AttributeModifiers", Tag.TAG_LIST);
    }

    private static void applyRemoveRule(ItemAttributeModifierEvent event, Attribute attribute) {
        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers == null || originalModifiers.isEmpty()) {
            return;
        }

        for (AttributeModifier original : originalModifiers) {
            event.removeModifier(attribute, original);
        }
    }

    private static void applyModifyRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.debug("[apply] MODIFY {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers.isEmpty()) {
            AttributeModify.LOGGER.debug("[semantic] MODIFY {} on {} in slot {} found no original item modifier to replace",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        boolean anyModified = false;
        for (AttributeModifier original : originalModifiers) {
            event.removeModifier(attribute, original);
            event.addModifier(attribute, new AttributeModifier(
                    original.getId(),
                    original.getName(),
                    dataModifier.getAmount(),
                    dataModifier.getOperation()
            ));
            anyModified = true;
        }

        if (!anyModified) {
            AttributeModify.LOGGER.debug("[semantic] MODIFY {} on {} in slot {} found modifiers, but none matched the original item identity",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
        }
    }

    private static void applyAddRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier,
            boolean preserveExternalOverrides) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.debug("[apply] ADD {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        if (!preserveExternalOverrides && dataModifier.getOperation() == AttributeModifier.Operation.ADDITION) {
            Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
            AttributeModifier targetToMerge = null;
            for (AttributeModifier original : originalModifiers) {
                if (original.getOperation() == AttributeModifier.Operation.ADDITION) {
                    targetToMerge = original;
                    break;
                }
            }

            if (targetToMerge != null) {
                event.removeModifier(attribute, targetToMerge);
                event.addModifier(attribute, new AttributeModifier(
                        targetToMerge.getId(),
                        targetToMerge.getName(),
                        targetToMerge.getAmount() + dataModifier.getAmount(),
                        targetToMerge.getOperation()
                ));
                return;
            }
        }

        event.addModifier(attribute, dataModifier);
    }

    private static void applySetRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.debug("[apply] SET {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        double amount = exactSetAmount(dataModifier.getAmount(), attribute.getDefaultValue());
        AttributeModifier replacement = new AttributeModifier(
                dataModifier.getId(),
                dataModifier.getName(),
                amount,
                AttributeModifier.Operation.ADDITION);
        if (originalModifiers != null) {
            for (AttributeModifier original : originalModifiers) {
                event.removeModifier(attribute, original);
            }
        }

        event.addModifier(attribute, replacement);
    }

    static double exactSetAmount(double targetValue, double defaultValue) {
        return targetValue - defaultValue;
    }
}
