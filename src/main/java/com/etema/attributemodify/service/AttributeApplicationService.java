package com.etema.attributemodify.service;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            if (entry.action() == ItemAttributeDataManager.AttributeAction.SET) {
                continue;
            }

            Attribute attribute = entry.attribute();
            if (attribute == null) continue;

            boolean preserveExternalOverrides = hasExternalModifiersForAttribute(event, attribute);

            switch (entry.action()) {
                case REMOVE -> {
                    if (preserveExternalOverrides) {
                        AttributeModify.LOGGER.debug("[apply] Removing external modifiers while applying REMOVE {} on {} in slot {}",
                                attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
                    }
                    applyRemoveRule(event, attribute, entry.targetOperation());
                }
                case MODIFY -> {
                    if (preserveExternalOverrides) {
                        AttributeModify.LOGGER.debug("[apply] Preserving external modifiers while applying MODIFY {} on {} in slot {}",
                                attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
                    }
                    applyModifyRule(event, attribute, entry.modifier(), preserveExternalOverrides);
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

            applySetRule(event, attribute, entry.modifier());
        }
    }

    public static void clearVanillaModifiers(ItemAttributeModifierEvent event) {
        if (event == null) {
            return;
        }

        // Decorative means inert, including modifiers supplied by another event handler.
        event.clearModifiers();
    }

    static boolean hasExternalModifiersForAttribute(ItemAttributeModifierEvent event, Attribute attribute) {
        if (event == null || attribute == null) {
            return false;
        }

        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers == null || originalModifiers.isEmpty()) {
            return false;
        }

        Collection<AttributeModifier> vanillaModifiers = event.getItemStack().getItem()
                .getDefaultAttributeModifiers(event.getSlotType()).get(attribute);
        if (vanillaModifiers == null || vanillaModifiers.isEmpty()) {
            return true;
        }

        for (AttributeModifier modifier : originalModifiers) {
            if (!containsEquivalentModifier(vanillaModifiers, modifier)) {
                return true;
            }
        }

        return false;
    }

    static boolean containsEquivalentModifier(Collection<AttributeModifier> candidates, AttributeModifier modifier) {
        if (candidates == null || candidates.isEmpty() || modifier == null) {
            return false;
        }

        for (AttributeModifier candidate : candidates) {
            if (candidate == modifier) {
                return true;
            }

            if (candidate.getId().equals(modifier.getId())
                    && candidate.getOperation() == modifier.getOperation()
                    && Double.compare(candidate.getAmount(), modifier.getAmount()) == 0
                    && Objects.equals(candidate.getName(), modifier.getName())) {
                return true;
            }
        }

        return false;
    }

    private static void applyRemoveRule(ItemAttributeModifierEvent event, Attribute attribute,
            AttributeModifier.Operation targetOperation) {
        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers == null || originalModifiers.isEmpty()) {
            return;
        }

        // A selected operation removes only that representation (flat, base %, or
        // total %). Missing operation keeps the legacy "remove everything" behavior.
        for (AttributeModifier original : List.copyOf(originalModifiers)) {
            if (targetOperation == null || original.getOperation() == targetOperation) {
                event.removeModifier(attribute, original);
            }
        }
    }

    private static void applyModifyRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier,
            boolean preserveExternalOverrides) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.debug("[apply] MODIFY {} on {} in slot {} was ignored because the datapack modifier is null",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            return;
        }

        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
        if (originalModifiers == null || originalModifiers.isEmpty()) {
            AttributeModify.LOGGER.debug(
                    "[semantic] MODIFY {} on {} in slot {} found no original item modifier to replace; falling back to ADD",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
            event.addModifier(attribute, dataModifier);
            return;
        }

        originalModifiers = List.copyOf(originalModifiers);
        boolean anyModified = false;
        Collection<AttributeModifier> vanillaModifiers = event.getItemStack().getItem()
                .getDefaultAttributeModifiers(event.getSlotType()).get(attribute);
        for (AttributeModifier original : originalModifiers) {
            if (preserveExternalOverrides && !containsEquivalentModifier(vanillaModifiers, original)) {
                continue;
            }

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
            Collection<AttributeModifier> vanillaModifiers = event.getItemStack().getItem()
                    .getDefaultAttributeModifiers(event.getSlotType()).get(attribute);
            if (originalModifiers == null || originalModifiers.isEmpty()) {
                event.addModifier(attribute, dataModifier);
                return;
            }
            originalModifiers = List.copyOf(originalModifiers);
            AttributeModifier targetToMerge = null;
            for (AttributeModifier original : originalModifiers) {
                if (original.getOperation() == AttributeModifier.Operation.ADDITION
                        && containsEquivalentModifier(vanillaModifiers, original)) {
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
            for (AttributeModifier original : List.copyOf(originalModifiers)) {
                event.removeModifier(attribute, original);
            }
        }

        event.addModifier(attribute, replacement);
    }

    static double exactSetAmount(double targetValue, double defaultValue) {
        return targetValue - defaultValue;
    }
}
