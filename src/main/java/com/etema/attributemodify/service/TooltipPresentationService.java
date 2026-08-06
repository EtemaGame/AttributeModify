package com.etema.attributemodify.service;

import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.util.AttributeTooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Presentation layer for attribute tooltips.
 *
 * MODIFY is intentionally left to vanilla rendering. The logical layer preserves
 * modifier identity, so this layer only removes hidden attributes and fills in
 * genuinely new ADD lines if Forge/vanilla did not already render them.
 */
public class TooltipPresentationService {

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private static final Map<EquipmentSlot, String> SLOT_HEADER_KEYS = new EnumMap<>(EquipmentSlot.class);

    static {
        for (EquipmentSlot slot : SLOTS) {
            SLOT_HEADER_KEYS.put(slot, "item.modifiers." + slot.getName());
        }
    }

    public static void handleTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (ItemAttributeDataManager.getInstance().isDecorative(itemStack.getItem())) {
            if (ItemAttributeDataManager.getInstance().getDecorativeTooltipMode(itemStack.getItem())
                    == ItemAttributeDataManager.DecorativeTooltipMode.PRESERVE) {
                stripAttributeSections(tooltip);
            } else {
                trimDecorativeTooltip(tooltip);
            }
            return;
        }

        Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot = collectTooltipRules(itemStack);
        if (rulesBySlot.isEmpty()) {
            mergeDuplicateAttributeSections(tooltip);
            return;
        }

        rewriteExactAttributes(tooltip, rulesBySlot);
        removeHiddenAttributes(tooltip, rulesBySlot);
        addMissingNewAttributes(itemStack, tooltip, rulesBySlot);
        mergeDuplicateAttributeSections(tooltip);
    }

    private static void trimDecorativeTooltip(List<Component> tooltip) {
        if (tooltip.isEmpty()) {
            return;
        }

        Component title = tooltip.get(0);
        tooltip.clear();
        tooltip.add(title);
    }

    static void stripAttributeSections(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            if (detectLogicalSlotHeader(tooltip.get(i)) == null) {
                continue;
            }

            int sectionStart = i;
            int sectionEnd = i + 1;
            while (sectionEnd < tooltip.size() && extractTooltipLineInfo(tooltip.get(sectionEnd)) != null) {
                sectionEnd++;
            }

            if (sectionStart > 0 && tooltip.get(sectionStart - 1).getString().isBlank()) {
                sectionStart--;
            }
            tooltip.subList(sectionStart, sectionEnd).clear();
            i = sectionStart - 1;
        }
    }

    /**
     * Curios and an item provider can both render a complete attribute section for
     * the same logical slot. Keep the first header and move any later attribute
     * lines into that section so metadata and unrelated tooltip text stay below it.
     */
    static void mergeDuplicateAttributeSections(List<Component> tooltip) {
        Map<String, Integer> firstHeaders = new HashMap<>();

        for (int i = 0; i < tooltip.size(); i++) {
            String slot = detectLogicalSlotHeader(tooltip.get(i));
            if (slot == null) {
                continue;
            }

            Integer firstHeader = firstHeaders.putIfAbsent(slot, i);
            if (firstHeader == null) {
                continue;
            }

            int sectionEnd = i + 1;
            List<Component> attributeLines = new ArrayList<>();
            while (sectionEnd < tooltip.size()
                    && extractTooltipLineInfo(tooltip.get(sectionEnd)) != null) {
                attributeLines.add(tooltip.get(sectionEnd));
                sectionEnd++;
            }

            int removalStart = i > 0 && tooltip.get(i - 1).getString().isBlank() ? i - 1 : i;
            tooltip.subList(removalStart, sectionEnd).clear();

            int insertionIndex = firstHeader + 1;
            Set<String> existingLines = new HashSet<>();
            while (insertionIndex < tooltip.size()
                    && extractTooltipLineInfo(tooltip.get(insertionIndex)) != null) {
                existingLines.add(attributeLineIdentity(tooltip.get(insertionIndex)));
                insertionIndex++;
            }

            for (Component attributeLine : attributeLines) {
                if (existingLines.add(attributeLineIdentity(attributeLine))) {
                    tooltip.add(insertionIndex++, attributeLine);
                }
            }

            i = Math.max(firstHeader, insertionIndex - 1);
        }
    }

    private static String attributeLineIdentity(Component line) {
        TooltipLineInfo info = extractTooltipLineInfo(line);
        return info == null ? line.getString()
                : info.attributeTranslationKey() + "|" + info.operation() + "|" + line.getString();
    }

    private static String detectLogicalSlotHeader(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key.startsWith("curios.modifiers.")) {
                String slot = key.substring("curios.modifiers.".length());
                if (!slot.isBlank() && !slot.contains(".")) {
                    return slot;
                }
            }
            if (key.startsWith("item.modifiers.")) {
                String slot = key.substring("item.modifiers.".length());
                if (!slot.isBlank()) {
                    return slot;
                }
            }
        }

        for (Component sibling : component.getSiblings()) {
            String slot = detectLogicalSlotHeader(sibling);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    private static Map<EquipmentSlot, List<AttributeInfo>> collectTooltipRules(ItemStack itemStack) {
        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot = new EnumMap<>(EquipmentSlot.class);

        for (EquipmentSlot slot : SLOTS) {
            List<AttributeInfo> slotRules = new ArrayList<>();

            for (ItemAttributeDataManager.AttributeEntry entry : dataManager.getEntriesForSlot(itemStack.getItem(), slot)) {
                if (entry.attribute() == null || !entry.matches(itemStack)) {
                    continue;
                }

                if (entry.action() == ItemAttributeDataManager.AttributeAction.REMOVE) {
                    slotRules.add(new AttributeInfo(entry.attribute(), null, entry.action(), entry.targetOperation()));
                    continue;
                }

                if ((entry.action() == ItemAttributeDataManager.AttributeAction.ADD
                        || entry.action() == ItemAttributeDataManager.AttributeAction.SET)
                        && entry.modifier() != null) {
                    slotRules.add(new AttributeInfo(entry.attribute(), entry.modifier(), entry.action(), null));
                }
            }

            if (!slotRules.isEmpty()) {
                rulesBySlot.put(slot, slotRules);
            }
        }

        return rulesBySlot;
    }

    private static void removeHiddenAttributes(List<Component> tooltip, Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot) {
        EquipmentSlot currentSlot = null;
        List<Integer> linesToRemove = new ArrayList<>();

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            EquipmentSlot detectedSlot = detectSlotHeader(line);
            if (detectedSlot != null) {
                currentSlot = detectedSlot;
                continue;
            }

            if (currentSlot == null || !rulesBySlot.containsKey(currentSlot)) {
                continue;
            }

            TooltipLineInfo lineInfo = extractTooltipLineInfo(line);
            if (lineInfo == null) {
                continue;
            }

            for (AttributeInfo info : rulesBySlot.get(currentSlot)) {
                if (info.action() == ItemAttributeDataManager.AttributeAction.REMOVE
                        && lineMatchesAttribute(lineInfo, info.attribute())
                        && (info.targetOperation() == null || info.targetOperation() == lineInfo.operation())) {
                    linesToRemove.add(i);
                    break;
                }
            }
        }

        for (int i = linesToRemove.size() - 1; i >= 0; i--) {
            int index = linesToRemove.get(i);
            if (index < tooltip.size()) {
                tooltip.remove(index);
            }
        }
    }

    private static void rewriteExactAttributes(List<Component> tooltip, Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot) {
        EquipmentSlot currentSlot = null;

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            EquipmentSlot detectedSlot = detectSlotHeader(line);
            if (detectedSlot != null) {
                currentSlot = detectedSlot;
                continue;
            }

            if (currentSlot == null || !rulesBySlot.containsKey(currentSlot)) {
                continue;
            }

            TooltipLineInfo lineInfo = extractTooltipLineInfo(line);
            if (lineInfo == null) {
                continue;
            }

            for (AttributeInfo info : rulesBySlot.get(currentSlot)) {
                if (info.action() != ItemAttributeDataManager.AttributeAction.SET
                        || !lineMatchesAttribute(lineInfo, info.attribute())) {
                    continue;
                }

                tooltip.set(i, createExactAttributeLine(info, currentSlot));
                break;
            }
        }
    }

    private static void addMissingNewAttributes(ItemStack itemStack, List<Component> tooltip,
            Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot) {
        TooltipScan scan = scanRenderedAttributes(tooltip, rulesBySlot);

        for (Map.Entry<EquipmentSlot, List<AttributeInfo>> slotEntry : rulesBySlot.entrySet()) {
            EquipmentSlot slot = slotEntry.getKey();

            for (AttributeInfo info : slotEntry.getValue()) {
                if (info.action() != ItemAttributeDataManager.AttributeAction.ADD
                        && info.action() != ItemAttributeDataManager.AttributeAction.SET
                        || info.modifier() == null
                        || (info.action() == ItemAttributeDataManager.AttributeAction.ADD
                            && hasVanillaAttribute(itemStack.getItem(), slot, info.attribute()))) {
                    continue;
                }

                Map<Attribute, Integer> renderedInSlot = scan.renderedCounts()
                        .computeIfAbsent(slot, ignored -> new HashMap<>());
                int renderedCount = renderedInSlot.getOrDefault(info.attribute(), 0);
                if (renderedCount > 0) {
                    renderedInSlot.put(info.attribute(), renderedCount - 1);
                    continue;
                }

                insertAttributeLine(tooltip, scan.insertionIndices(), slot, createNewAttributeLine(info, slot));
            }
        }
    }

    private static TooltipScan scanRenderedAttributes(List<Component> tooltip,
            Map<EquipmentSlot, List<AttributeInfo>> rulesBySlot) {
        EquipmentSlot currentSlot = null;
        Map<EquipmentSlot, Map<Attribute, Integer>> renderedCounts = new EnumMap<>(EquipmentSlot.class);
        Map<EquipmentSlot, Integer> insertionIndices = new EnumMap<>(EquipmentSlot.class);

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            EquipmentSlot detectedSlot = detectSlotHeader(line);
            if (detectedSlot != null) {
                currentSlot = detectedSlot;
                insertionIndices.put(currentSlot, i + 1);
                renderedCounts.computeIfAbsent(currentSlot, ignored -> new HashMap<>());
                continue;
            }

            if (currentSlot == null || !rulesBySlot.containsKey(currentSlot)) {
                continue;
            }

            TooltipLineInfo lineInfo = extractTooltipLineInfo(line);
            if (lineInfo == null) {
                continue;
            }

            insertionIndices.put(currentSlot, i + 1);

            for (AttributeInfo info : rulesBySlot.get(currentSlot)) {
                if (lineMatchesAttribute(lineInfo, info.attribute())) {
                    renderedCounts
                            .computeIfAbsent(currentSlot, ignored -> new HashMap<>())
                            .merge(info.attribute(), 1, Integer::sum);
                    break;
                }
            }
        }

        return new TooltipScan(renderedCounts, insertionIndices);
    }

    @SuppressWarnings("deprecation")
    private static boolean hasVanillaAttribute(Item item, EquipmentSlot slot, Attribute attribute) {
        return item.getDefaultAttributeModifiers(slot).containsKey(attribute);
    }

    private static Component createNewAttributeLine(AttributeInfo info, EquipmentSlot slot) {
        AttributeModifier modifier = info.modifier();
        return AttributeTooltipHelper.createFormattedLine(
                modifier.getAmount(),
                Component.translatable(info.attribute().getDescriptionId()),
                modifier.getAmount(),
                modifier.getOperation(),
                slot,
                true,
                info.attribute());
    }

    private static Component createExactAttributeLine(AttributeInfo info, EquipmentSlot slot) {
        AttributeModifier modifier = info.modifier();
        double exactValue = modifier.getAmount();
        return AttributeTooltipHelper.createFormattedLine(
                exactValue,
                Component.translatable(info.attribute().getDescriptionId()),
                modifier.getAmount(),
                AttributeModifier.Operation.ADDITION,
                slot,
                false,
                info.attribute());
    }

    private static void insertAttributeLine(List<Component> tooltip, Map<EquipmentSlot, Integer> insertionIndices,
            EquipmentSlot slot, Component line) {
        Integer insertionIndex = insertionIndices.get(slot);
        if (insertionIndex == null) {
            if (!tooltip.isEmpty()) {
                tooltip.add(Component.empty());
            }
            tooltip.add(Component.translatable(SLOT_HEADER_KEYS.get(slot)).withStyle(ChatFormatting.GRAY));
            insertionIndex = tooltip.size();
            insertionIndices.put(slot, insertionIndex);
        }

        int index = Math.max(0, Math.min(insertionIndex, tooltip.size()));
        tooltip.add(index, line);
        shiftInsertionIndices(insertionIndices, index);
    }

    private static void shiftInsertionIndices(Map<EquipmentSlot, Integer> insertionIndices, int insertedAt) {
        for (Map.Entry<EquipmentSlot, Integer> entry : insertionIndices.entrySet()) {
            if (entry.getValue() >= insertedAt) {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    private static EquipmentSlot detectSlotHeader(Component line) {
        for (Map.Entry<EquipmentSlot, String> entry : SLOT_HEADER_KEYS.entrySet()) {
            if (containsTranslationKey(line, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean containsTranslationKey(Component component, String expectedKey) {
        if (component.getContents() instanceof TranslatableContents translatable
                && expectedKey.equals(translatable.getKey())) {
            return true;
        }

        for (Component sibling : component.getSiblings()) {
            if (containsTranslationKey(sibling, expectedKey)) {
                return true;
            }
        }

        return false;
    }

    private static TooltipLineInfo extractTooltipLineInfo(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            TooltipLineInfo info = extractTooltipLineInfo(translatable);
            if (info != null) {
                return info;
            }
        }

        for (Component sibling : component.getSiblings()) {
            TooltipLineInfo info = extractTooltipLineInfo(sibling);
            if (info != null) {
                return info;
            }
        }

        return null;
    }

    private static TooltipLineInfo extractTooltipLineInfo(TranslatableContents translatable) {
        String key = translatable.getKey();
        Object[] args = translatable.getArgs();
        if (!key.startsWith("attribute.modifier.") || args.length < 2) {
            return null;
        }

        return new TooltipLineInfo(extractTranslationKey(args[1]), operationFromTranslationKey(key));
    }

    private static AttributeModifier.Operation operationFromTranslationKey(String key) {
        if (key.endsWith(".0")) return AttributeModifier.Operation.ADDITION;
        if (key.endsWith(".1")) return AttributeModifier.Operation.MULTIPLY_BASE;
        if (key.endsWith(".2")) return AttributeModifier.Operation.MULTIPLY_TOTAL;
        return null;
    }

    private static String extractTranslationKey(Object argument) {
        if (argument instanceof Component component) {
            if (component.getContents() instanceof TranslatableContents translatable) {
                return translatable.getKey();
            }

            for (Component sibling : component.getSiblings()) {
                String key = extractTranslationKey(sibling);
                if (key != null) {
                    return key;
                }
            }
        }

        return null;
    }

    private static boolean lineMatchesAttribute(TooltipLineInfo lineInfo, Attribute attribute) {
        return attribute.getDescriptionId().equals(lineInfo.attributeTranslationKey());
    }

    private record AttributeInfo(Attribute attribute, AttributeModifier modifier,
            ItemAttributeDataManager.AttributeAction action, AttributeModifier.Operation targetOperation) {
    }

    private record TooltipLineInfo(String attributeTranslationKey, AttributeModifier.Operation operation) {
    }

    private record TooltipScan(Map<EquipmentSlot, Map<Attribute, Integer>> renderedCounts,
            Map<EquipmentSlot, Integer> insertionIndices) {
    }
}
