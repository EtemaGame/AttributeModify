package com.etema.attributemodify.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Locale;

public class AttributeTooltipHelper {

    /**
     * Formats a number for tooltip display (removes trailing .0 if integer, otherwise 1 decimal).
     */
    public static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        if (Math.abs(value) < 0.1 && value != 0) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Selects the appropriate translation key based on operation and value sign.
     */
    public static String getTranslationKey(AttributeModifier.Operation operation, boolean isPositive, boolean isEqualStyle) {
        int opIndex = switch (operation) {
            case ADDITION -> 0;
            case MULTIPLY_BASE -> 1;
            case MULTIPLY_TOTAL -> 2;
        };

        if (isEqualStyle) {
            return "attribute.modifier.equals." + opIndex;
        }

        return isPositive
                ? "attribute.modifier.plus." + opIndex
                : "attribute.modifier.take." + opIndex;
    }

    /**
     * Determines the display color for an attribute.
     */
    public static ChatFormatting getAttributeColor(Attribute attribute, boolean isPositive, EquipmentSlot slot) {
        if (attribute.equals(Attributes.ATTACK_SPEED)) {
            return (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) 
                ? ChatFormatting.DARK_GREEN 
                : ChatFormatting.BLUE;
        }

        if (!isPositive) {
            return ChatFormatting.RED;
        }

        return (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) 
            ? ChatFormatting.DARK_GREEN 
            : ChatFormatting.BLUE;
    }

    /**
     * Basic check for armor slots.
     */
    public static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    /**
     * Computes the final value for display based on the operation.
     */
    public static double computeDisplayValue(double total, double modifierValue, AttributeModifier.Operation operation, boolean isNewAttribute, boolean isArmorPositive) {
        if (operation == AttributeModifier.Operation.MULTIPLY_BASE || operation == AttributeModifier.Operation.MULTIPLY_TOTAL) {
            return Math.abs(modifierValue * 100);
        }
        return Math.abs(isNewAttribute || isArmorPositive ? modifierValue : total);
    }

    /**
     * Creates a standard formatted tooltip line.
     */
    public static MutableComponent createFormattedLine(double total, Component attributeName, double modifierValue, 
                                                       AttributeModifier.Operation operation, EquipmentSlot slot, 
                                                       boolean isNewAttribute, Attribute attribute) {
        
        boolean isPositive = modifierValue >= 0;
        boolean isArmorPos = isArmorSlot(slot) && isPositive;
        boolean isEqualStyle = !(isNewAttribute || isArmorPos);
        
        ChatFormatting color = getAttributeColor(attribute, isPositive, slot);
        double displayValue = computeDisplayValue(total, modifierValue, operation, isNewAttribute, isArmorPos);
        String valueStr = formatNumber(displayValue);
        
        String translationKey = getTranslationKey(operation, isPositive, isEqualStyle);
        MutableComponent line = Component.translatable(translationKey, valueStr, attributeName).withStyle(color);

        if (isEqualStyle) {
            return Component.literal(" ").append(line);
        }
        return line;
    }
}
