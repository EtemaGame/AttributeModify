package com.etema.attributemodify.service;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
            if (entry.action() == ItemAttributeDataManager.AttributeAction.SET) {
                continue;
            }

            Attribute attribute = entry.attribute();
            if (attribute == null) continue;

            switch (entry.action()) {
                case REMOVE -> removeOriginalAttributeModifiers(finalModifiers, attribute, event.getOriginalModifiers().get(attribute));
                case MODIFY -> applyModifyRule(event, attribute, entry.modifier(), finalModifiers);
                case ADD -> applyAddRule(event, attribute, entry.modifier(), finalModifiers);
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

            applySetRule(event, attribute, entry.modifier(), finalModifiers);
        }

        installOrderedModifiers(event, finalModifiers);
    }

    private static void installOrderedModifiers(ItemAttributeModifierEvent event,
            List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
        LinkedHashMultimap<Attribute, AttributeModifier> ordered = buildOrderedModifiers(finalModifiers);

        try {
            setPrivateField(event, "modifiableModifiers", ordered);
            setPrivateField(event, "unmodifiableModifiers", Multimaps.unmodifiableMultimap(ordered));
        } catch (ReflectiveOperationException ex) {
            AttributeModify.LOGGER.warn("[apply] Could not preserve attribute order for {} in slot {}: {}",
                    event.getItemStack().getItem(), event.getSlotType(), ex.getMessage());

            event.clearModifiers();
            for (java.util.Map.Entry<Attribute, AttributeModifier> e : finalModifiers) {
                try {
                    event.addModifier(e.getKey(), e.getValue());
                } catch (Exception addEx) {
                    AttributeModify.LOGGER.error("[apply] Failed to apply {} on {} in slot {}: {}",
                            e.getKey().getDescriptionId(), event.getItemStack().getItem(), event.getSlotType(), addEx.getMessage(), addEx);
                }
            }
        }
    }

    static LinkedHashMultimap<Attribute, AttributeModifier> buildOrderedModifiers(
            List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
        LinkedHashMultimap<Attribute, AttributeModifier> ordered = LinkedHashMultimap.create();
        for (java.util.Map.Entry<Attribute, AttributeModifier> entry : finalModifiers) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return ordered;
    }

    static <T> void removeOriginalAttributeModifiers(List<java.util.Map.Entry<T, AttributeModifier>> modifiers,
            T key, Collection<AttributeModifier> originalModifiers) {
        if (originalModifiers == null || originalModifiers.isEmpty()) {
            return;
        }

        Set<UUID> originalIds = new HashSet<>();
        for (AttributeModifier original : originalModifiers) {
            originalIds.add(original.getId());
        }

        modifiers.removeIf(entry -> Objects.equals(entry.getKey(), key)
                && originalIds.contains(entry.getValue().getId()));
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void applyModifyRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier, List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
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
            AttributeModify.LOGGER.debug("[semantic] MODIFY {} on {} in slot {} found modifiers, but none matched the original item identity",
                    attribute.getDescriptionId(), event.getItemStack().getItem(), event.getSlotType());
        }
    }

    private static void applyAddRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier, List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
        if (dataModifier == null) {
            AttributeModify.LOGGER.debug("[apply] ADD {} on {} in slot {} was ignored because the datapack modifier is null",
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

    private static void applySetRule(ItemAttributeModifierEvent event, Attribute attribute, AttributeModifier dataModifier,
            List<java.util.Map.Entry<Attribute, AttributeModifier>> finalModifiers) {
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
        replaceAttributePreservingOrder(finalModifiers, attribute, replacement, originalModifiers);
    }

    static <T> void replaceAttributePreservingOrder(List<java.util.Map.Entry<T, AttributeModifier>> modifiers,
            T key, AttributeModifier replacement, Collection<AttributeModifier> originalModifiers) {
        Set<UUID> originalIds = new HashSet<>();
        if (originalModifiers != null) {
            for (AttributeModifier original : originalModifiers) {
                originalIds.add(original.getId());
            }
        }

        int firstIndex = -1;
        for (int i = 0; i < modifiers.size(); i++) {
            java.util.Map.Entry<T, AttributeModifier> entry = modifiers.get(i);
            if (Objects.equals(entry.getKey(), key) && originalIds.contains(entry.getValue().getId())) {
                if (firstIndex < 0) {
                    firstIndex = i;
                }
            }
        }

        if (firstIndex < 0) {
            modifiers.add(java.util.Map.entry(key, replacement));
            return;
        }

        for (int i = modifiers.size() - 1; i >= 0; i--) {
            java.util.Map.Entry<T, AttributeModifier> entry = modifiers.get(i);
            if (Objects.equals(entry.getKey(), key) && originalIds.contains(entry.getValue().getId())) {
                modifiers.remove(i);
            }
        }

        modifiers.add(firstIndex, java.util.Map.entry(key, replacement));
    }

    static double exactSetAmount(double targetValue, double defaultValue) {
        return targetValue - defaultValue;
    }
}
