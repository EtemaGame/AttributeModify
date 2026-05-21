package com.etema.attributemodify.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

public final class EditorBaseAttributeService {
    private EditorBaseAttributeService() {
    }

    public static JsonArray collectBaseAttributes(ResourceLocation itemId) {
        JsonArray attributes = new JsonArray();
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return attributes;
        }

        Set<String> seen = new LinkedHashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            addModifiers(attributes, seen, slot, item.getDefaultAttributeModifiers(slot));
        }
        return attributes;
    }

    private static void addModifiers(JsonArray output, Set<String> seen, EquipmentSlot slot,
            Multimap<Attribute, AttributeModifier> modifiers) {
        for (var entry : modifiers.entries()) {
            ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (attributeId == null) {
                continue;
            }

            AttributeModifier modifier = entry.getValue();
            String key = attributeId + "|" + modifier.getOperation().name() + "|" + slot.getName();
            if (!seen.add(key)) {
                continue;
            }

            JsonObject object = new JsonObject();
            object.addProperty("attribute", attributeId.toString());
            object.addProperty("amount", modifier.getAmount());
            object.addProperty("operation", operationName(modifier.getOperation()));
            object.addProperty("slot", slot.getName());
            output.add(object);
        }
    }

    private static String operationName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADDITION -> "addition";
            case MULTIPLY_BASE -> "multiply_base";
            case MULTIPLY_TOTAL -> "multiply_total";
        };
    }
}
