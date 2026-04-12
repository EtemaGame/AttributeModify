package com.etema.attributemodify;

import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Customizes attribute tooltip lines.
 */
public class AttributeTooltipHandler {
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();
    private static final Map<EquipmentSlot, String> SLOT_HEADER_KEYS = new EnumMap<>(EquipmentSlot.class);

    private static final Map<Attribute, Double> PLAYER_BASE_VALUES = new HashMap<>();

    static {
        PLAYER_BASE_VALUES.put(Attributes.ATTACK_DAMAGE, 1.0);
        PLAYER_BASE_VALUES.put(Attributes.ATTACK_SPEED, 4.0);
        PLAYER_BASE_VALUES.put(Attributes.ARMOR, 0.0);
        PLAYER_BASE_VALUES.put(Attributes.ARMOR_TOUGHNESS, 0.0);
        PLAYER_BASE_VALUES.put(Attributes.KNOCKBACK_RESISTANCE, 0.0);
        PLAYER_BASE_VALUES.put(Attributes.MAX_HEALTH, 20.0);
        PLAYER_BASE_VALUES.put(Attributes.MOVEMENT_SPEED, 0.1);
        PLAYER_BASE_VALUES.put(Attributes.LUCK, 0.0);

        if (ForgeRegistries.ATTRIBUTES.containsKey(ResourceLocation.tryParse("minecraft:generic.flying_speed"))) {
            PLAYER_BASE_VALUES.put(Attributes.FLYING_SPEED, 0.4);
        }

        for (EquipmentSlot slot : SLOTS) {
            SLOT_HEADER_KEYS.put(slot, "item.modifiers." + slot.getName());
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) {
            return;
        }

        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        if (!dataManager.hasCustomAttributes(itemStack.getItem())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        Map<EquipmentSlot, List<AttributeInfo>> modifiersBySlot = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : SLOTS) {
            List<AttributeInfo> infos = getModifiedAttributesForSlot(itemStack, slot, dataManager);
            if (!infos.isEmpty()) {
                modifiersBySlot.put(slot, infos);
            }
        }

        if (modifiersBySlot.isEmpty()) {
            return;
        }

        EquipmentSlot currentSlot = null;
        Set<Attribute> processedInSlot = null;
        Map<Attribute, Integer> duplicateBudget = null;
        List<Integer> linesToRemove = null;

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);

            EquipmentSlot detectedSlot = detectSlotHeader(line);
            if (detectedSlot != null) {
                currentSlot = detectedSlot;
                if (processedInSlot == null) {
                    processedInSlot = new HashSet<>();
                } else {
                    processedInSlot.clear();
                }

                if (duplicateBudget == null) {
                    duplicateBudget = new HashMap<>();
                } else {
                    duplicateBudget.clear();
                }

                if (modifiersBySlot.containsKey(currentSlot)) {
                    for (AttributeInfo aInfo : modifiersBySlot.get(currentSlot)) {
                        if (aInfo.action == ItemAttributeDataManager.AttributeAction.ADD) {
                            duplicateBudget.merge(aInfo.attribute, 1, Integer::sum);
                        }
                    }
                }
            }

            if (currentSlot == null || !modifiersBySlot.containsKey(currentSlot)) {
                continue;
            }

            TooltipLineInfo lineInfo = extractTooltipLineInfo(line);
            if (lineInfo == null) {
                continue;
            }

            List<AttributeInfo> attributes = modifiersBySlot.get(currentSlot);
            for (AttributeInfo info : attributes) {
                boolean isRemoveAction = info.action == ItemAttributeDataManager.AttributeAction.REMOVE;
                if (!lineMatchesAttribute(lineInfo, info, isRemoveAction)) {
                    continue;
                }

                if (!processedInSlot.contains(info.attribute)) {
                    if (isRemoveAction) {
                        if (linesToRemove == null) {
                            linesToRemove = new ArrayList<>();
                        }
                        linesToRemove.add(i);
                    } else {
                        double playerBase = PLAYER_BASE_VALUES.getOrDefault(info.attribute, 0.0);
                        double itemVanillaBase = getVanillaItemBaseValue(itemStack.getItem(), info.attribute, currentSlot);
                        double fullVanilla = playerBase + itemVanillaBase;

                        double displayTotal = 0.0;
                        double displayItemParams = 0.0;

                        if (info.action == ItemAttributeDataManager.AttributeAction.MODIFY) {
                            switch (info.operation) {
                                case ADDITION -> {
                                    displayTotal = playerBase + info.amount;
                                    displayItemParams = info.amount;
                                }
                                case MULTIPLY_BASE, MULTIPLY_TOTAL -> {
                                    if (playerBase == 0 && itemVanillaBase == 0) {
                                        displayTotal = info.amount;
                                        displayItemParams = info.amount;
                                    } else {
                                        displayTotal = playerBase * (1.0 + info.amount);
                                        displayItemParams = displayTotal - playerBase;
                                    }
                                }
                            }
                        } else {
                            switch (info.operation) {
                                case ADDITION -> {
                                    displayTotal = fullVanilla + info.amount;
                                    displayItemParams = itemVanillaBase + info.amount;
                                }
                                case MULTIPLY_BASE, MULTIPLY_TOTAL -> {
                                    if (fullVanilla == 0) {
                                        displayTotal = info.amount;
                                        displayItemParams = info.amount;
                                    } else {
                                        displayTotal = fullVanilla * (1.0 + info.amount);
                                        double addedBonus = fullVanilla * info.amount;
                                        displayItemParams = itemVanillaBase + addedBonus;
                                    }
                                }
                            }
                        }

                        boolean isNewAttribute = itemVanillaBase == 0;
                        MutableComponent newLine = createFormattedLine(
                                displayTotal,
                                Component.translatable(info.attribute.getDescriptionId()),
                                displayItemParams,
                                info.operation,
                                displayItemParams >= 0,
                                currentSlot,
                                isNewAttribute,
                                info.attribute);

                        tooltip.set(i, newLine);
                    }

                    processedInSlot.add(info.attribute);
                } else {
                    if (isRemoveAction) {
                        if (linesToRemove == null) {
                            linesToRemove = new ArrayList<>();
                        }
                        linesToRemove.add(i);
                    } else {
                        int budget = duplicateBudget.getOrDefault(info.attribute, 0);
                        if (budget > 0) {
                            if (linesToRemove == null) {
                                linesToRemove = new ArrayList<>();
                            }
                            linesToRemove.add(i);
                            duplicateBudget.put(info.attribute, budget - 1);
                        }
                    }
                }
                break;
            }
        }

        if (linesToRemove != null) {
            for (int i = linesToRemove.size() - 1; i >= 0; i--) {
                int index = linesToRemove.get(i);
                if (index < tooltip.size()) {
                    tooltip.remove(index);
                }
            }
        }
    }

    private List<AttributeInfo> getModifiedAttributesForSlot(ItemStack itemStack, EquipmentSlot slot,
            ItemAttributeDataManager dataManager) {
        List<AttributeInfo> result = new ArrayList<>();
        Collection<ItemAttributeDataManager.AttributeEntry> entries =
                dataManager.getEntriesForSlot(itemStack.getItem(), slot);

        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            if (entry.attribute() == null) {
                continue;
            }
            if (entry.action() != ItemAttributeDataManager.AttributeAction.REMOVE && entry.modifier() == null) {
                continue;
            }
            if (!entry.matches(itemStack)) {
                continue;
            }

            double amount = entry.modifier() != null ? entry.modifier().getAmount() : 0;
            AttributeModifier.Operation operation = entry.modifier() != null
                    ? entry.modifier().getOperation()
                    : AttributeModifier.Operation.ADDITION;

            result.add(new AttributeInfo(entry.attribute(), amount, operation, entry.action()));
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private double getVanillaItemBaseValue(Item item, Attribute attribute, EquipmentSlot slot) {
        if (item instanceof ArmorItem armorItem) {
            if (armorItem.getEquipmentSlot() == slot) {
                if (attribute.equals(Attributes.ARMOR)) {
                    return armorItem.getDefense();
                } else if (attribute.equals(Attributes.ARMOR_TOUGHNESS)) {
                    return armorItem.getToughness();
                } else if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) {
                    return armorItem.getMaterial().getKnockbackResistance();
                }
            }
        }

        Multimap<Attribute, AttributeModifier> modifiers = item.getDefaultAttributeModifiers(slot);
        double total = 0.0;
        for (var entry : modifiers.entries()) {
            if (entry.getKey().equals(attribute)) {
                total += entry.getValue().getAmount();
            }
        }

        return total;
    }

    private MutableComponent createFormattedLine(double total, Component attributeName, double modifierValue,
            AttributeModifier.Operation operation, boolean isPositive, EquipmentSlot slot,
            boolean isNewAttribute, Attribute attribute) {
        boolean isAttackSpeed = attribute.equals(Attributes.ATTACK_SPEED);

        ChatFormatting color;
        if (isAttackSpeed) {
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                color = ChatFormatting.DARK_GREEN;
            } else {
                color = ChatFormatting.BLUE;
            }
        } else if (!isPositive) {
            color = ChatFormatting.RED;
        } else if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            color = ChatFormatting.DARK_GREEN;
        } else {
            color = ChatFormatting.BLUE;
        }

        boolean isArmorSlot = slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;

        int opIndex = switch (operation) {
            case ADDITION -> 0;
            case MULTIPLY_BASE -> 1;
            case MULTIPLY_TOTAL -> 2;
        };

        double displayValue;
        if (operation == AttributeModifier.Operation.MULTIPLY_BASE
                || operation == AttributeModifier.Operation.MULTIPLY_TOTAL) {
            displayValue = Math.abs(modifierValue * 100);
        } else {
            displayValue = Math.abs(isNewAttribute ? modifierValue : total);
        }

        String valueStr = formatNumber(displayValue);
        if (isNewAttribute || (isArmorSlot && isPositive)) {
            String translationKey = isPositive
                    ? "attribute.modifier.plus." + opIndex
                    : "attribute.modifier.take." + opIndex;
            return Component.translatable(translationKey, valueStr, attributeName).withStyle(color);
        }

        MutableComponent formattedLine = Component
                .translatable("attribute.modifier.equals." + opIndex, valueStr, attributeName)
                .withStyle(color);
        return Component.literal(" ").append(formattedLine);
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        if (Math.abs(value) < 0.1 && value != 0) {
            return String.format("%.2f", value);
        }
        return String.format("%.1f", value);
    }

    private String getAttributeDisplayName(Attribute attribute) {
        return Component.translatable(attribute.getDescriptionId()).getString();
    }

    private EquipmentSlot detectSlotHeader(Component line) {
        for (Map.Entry<EquipmentSlot, String> entry : SLOT_HEADER_KEYS.entrySet()) {
            if (containsTranslationKey(line, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean containsTranslationKey(Component component, String expectedKey) {
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

    private TooltipLineInfo extractTooltipLineInfo(Component component) {
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

    private TooltipLineInfo extractTooltipLineInfo(TranslatableContents translatable) {
        String key = translatable.getKey();
        if (!key.startsWith("attribute.modifier.")) {
            return null;
        }

        Object[] args = translatable.getArgs();
        if (args.length < 2) {
            return null;
        }

        return new TooltipLineInfo(
                extractTranslationKey(args[1]),
                extractRenderedText(args[1]),
                key.endsWith(".1") || key.endsWith(".2"));
    }

    private String extractTranslationKey(Object argument) {
        if (argument instanceof Component component) {
            return extractTranslationKey(component);
        }
        return null;
    }

    private String extractTranslationKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }

        for (Component sibling : component.getSiblings()) {
            String key = extractTranslationKey(sibling);
            if (key != null) {
                return key;
            }
        }

        return null;
    }

    private String extractRenderedText(Object argument) {
        if (argument instanceof Component component) {
            return component.getString();
        }
        return argument == null ? "" : String.valueOf(argument);
    }

    private boolean lineMatchesAttribute(TooltipLineInfo lineInfo, AttributeInfo info, boolean matchAnyFormat) {
        String expectedKey = info.attribute.getDescriptionId();
        String expectedText = getAttributeDisplayName(info.attribute);
        boolean sameAttribute = expectedKey.equals(lineInfo.attributeTranslationKey())
                || expectedText.equals(lineInfo.attributeRenderedText());
        if (!sameAttribute) {
            return false;
        }

        if (matchAnyFormat) {
            return true;
        }

        boolean isPercentageOp = info.operation != AttributeModifier.Operation.ADDITION;
        return lineInfo.percentage() == isPercentageOp;
    }

    private record AttributeInfo(Attribute attribute, double amount, AttributeModifier.Operation operation,
            ItemAttributeDataManager.AttributeAction action) {
    }

    private record TooltipLineInfo(String attributeTranslationKey, String attributeRenderedText, boolean percentage) {
    }
}
