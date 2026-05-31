package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableCondition;
import com.etema.attributemodify.editor.model.EditableDurabilityModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EditorRuleSerializer {
    private EditorRuleSerializer() {
    }

    public static List<EditableItemRule> fromDocument(JsonObject document) {
        List<EditableItemRule> rules = new ArrayList<>();
        if (document == null) {
            return rules;
        }

        for (Map.Entry<String, JsonElement> entry : document.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            String key = entry.getKey();
            boolean tagTarget = key.startsWith("#");
            ResourceLocation targetId = ResourceLocation.tryParse(tagTarget ? key.substring(1) : key);
            if (targetId == null) {
                continue;
            }

            EditableItemRule rule = fromItemObject(targetId, tagTarget, entry.getValue().getAsJsonObject());
            rules.add(rule);
        }
        return rules;
    }

    public static EditableItemRule fromItemObject(ResourceLocation targetId, boolean tagTarget, JsonObject itemObject) {
        EditableItemRule rule = new EditableItemRule(targetId, tagTarget);

        if (itemObject == null) {
            return rule;
        }

        if (itemObject.has("equipment_slots") && itemObject.get("equipment_slots").isJsonObject()) {
            JsonObject slots = itemObject.getAsJsonObject("equipment_slots");
            for (Map.Entry<String, JsonElement> slotEntry : slots.entrySet()) {
                if (slotEntry.getValue().isJsonArray()) {
                    readAttributeArray(rule, slotEntry.getValue().getAsJsonArray(), EditableSlotType.STANDARD, slotEntry.getKey());
                }
            }
        }

        if (itemObject.has("curios_slots") && itemObject.get("curios_slots").isJsonObject()) {
            JsonObject slots = itemObject.getAsJsonObject("curios_slots");
            for (Map.Entry<String, JsonElement> slotEntry : slots.entrySet()) {
                if (slotEntry.getValue().isJsonArray()) {
                    readAttributeArray(rule, slotEntry.getValue().getAsJsonArray(), EditableSlotType.CURIOS, slotEntry.getKey());
                }
            }
        }

        if (itemObject.has("attributes") && itemObject.get("attributes").isJsonArray()) {
            readAttributeArray(rule, itemObject.getAsJsonArray("attributes"), EditableSlotType.AUTO, null);
        }

        if (itemObject.has("attribute")) {
            JsonArray single = new JsonArray();
            single.add(itemObject);
            readAttributeArray(rule, single, EditableSlotType.AUTO, null);
        }

        if (itemObject.has("durability") && itemObject.get("durability").isJsonPrimitive()) {
            EditableDurabilityModifier durability = new EditableDurabilityModifier(itemObject.get("durability").getAsInt());
            readDurabilityTriggers(durability, itemObject);
            rule.setDurability(durability);
        }

        return rule;
    }

    public static JsonObject toDocument(List<EditableItemRule> rules) {
        JsonObject document = new JsonObject();
        if (rules == null) {
            return document;
        }

        for (EditableItemRule rule : rules) {
            if (rule == null || rule.getTargetId() == null) {
                continue;
            }
            document.add(rule.serializedTargetKey(), toItemObject(rule));
        }
        return document;
    }

    public static JsonObject toItemObject(EditableItemRule rule) {
        JsonObject itemObject = new JsonObject();
        JsonObject equipmentSlots = new JsonObject();
        JsonObject curiosSlots = new JsonObject();
        JsonArray autoAttributes = new JsonArray();

        for (EditableAttributeModifier attribute : rule.getAttributes()) {
            if (attribute == null) {
                continue;
            }

            JsonObject attributeObject = toAttributeObject(attribute);
            if (attribute.getSlotType() == EditableSlotType.STANDARD) {
                String slot = blankToDefault(attribute.getSlot(), "mainhand");
                JsonArray slotArray = equipmentSlots.has(slot) ? equipmentSlots.getAsJsonArray(slot) : new JsonArray();
                slotArray.add(attributeObject);
                equipmentSlots.add(slot, slotArray);
            } else if (attribute.getSlotType() == EditableSlotType.CURIOS) {
                String slot = blankToDefault(attribute.getSlot(), "curio");
                JsonArray slotArray = curiosSlots.has(slot) ? curiosSlots.getAsJsonArray(slot) : new JsonArray();
                slotArray.add(attributeObject);
                curiosSlots.add(slot, slotArray);
            } else {
                autoAttributes.add(attributeObject);
            }
        }

        if (equipmentSlots.size() > 0) {
            itemObject.add("equipment_slots", equipmentSlots);
        }
        if (curiosSlots.size() > 0) {
            itemObject.add("curios_slots", curiosSlots);
        }
        if (autoAttributes.size() > 0) {
            itemObject.add("attributes", autoAttributes);
        }

        EditableDurabilityModifier durability = rule.getDurability();
        if (durability != null && durability.getDurability() != null) {
            itemObject.addProperty("durability", durability.getDurability());
            if (!durability.getTriggers().isEmpty()) {
                JsonArray triggers = new JsonArray();
                for (String trigger : durability.getTriggers()) {
                    triggers.add(trigger);
                }
                itemObject.add("durability_triggers", triggers);
            }
        }

        return itemObject;
    }

    private static void readAttributeArray(EditableItemRule rule, JsonArray attributes, EditableSlotType slotType, String slot) {
        for (JsonElement element : attributes) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            if (!object.has("attribute")) {
                continue;
            }

            ResourceLocation attributeId = ResourceLocation.tryParse(object.get("attribute").getAsString());
            EditableAttributeAction action = EditableAttributeAction.fromString(readString(object, "action"));
            EditableOperationType operation = EditableOperationType.fromString(readString(object, "operation"));
            Double amount = object.has("amount") && object.get("amount").isJsonPrimitive()
                    ? object.get("amount").getAsDouble()
                    : null;

            EditableAttributeModifier modifier = new EditableAttributeModifier(attributeId, action, amount, operation, slotType, slot);
            modifier.setModifierId(readString(object, "modifier_id"));
            modifier.setUuid(readUuid(object));
            modifier.setCondition(readCondition(object));
            rule.getAttributes().add(modifier);
        }
    }

    private static JsonObject toAttributeObject(EditableAttributeModifier attribute) {
        JsonObject object = new JsonObject();
        if (attribute.getAttributeId() != null) {
            object.addProperty("attribute", attribute.getAttributeId().toString());
        }

        EditableAttributeAction action = attribute.getAction();
        if (action != null && action != EditableAttributeAction.ADD) {
            object.addProperty("action", action.serializedName());
        } else if (action == EditableAttributeAction.ADD) {
            object.addProperty("action", action.serializedName());
        }

        if (action != EditableAttributeAction.REMOVE && attribute.getAmount() != null) {
            object.addProperty("amount", attribute.getAmount());
        }
        if (action != EditableAttributeAction.REMOVE && action != EditableAttributeAction.SET && attribute.getOperation() != null) {
            object.addProperty("operation", attribute.getOperation().serializedName());
        }
        if (attribute.getModifierId() != null && !attribute.getModifierId().isBlank()) {
            object.addProperty("modifier_id", attribute.getModifierId());
        }
        if (attribute.getUuid() != null) {
            object.addProperty("uuid", attribute.getUuid().toString());
        }
        if (attribute.getCondition() != null) {
            object.add("nbt", toConditionObject(attribute.getCondition()));
        }
        return object;
    }

    private static EditableCondition readCondition(JsonObject object) {
        if (!object.has("nbt") || !object.get("nbt").isJsonObject()) {
            return null;
        }

        JsonObject nbt = object.getAsJsonObject("nbt");
        String path = nbt.has("path") ? nbt.get("path").getAsString() : readString(nbt, "key");
        if (path == null || path.isBlank()) {
            return null;
        }

        String operator = readString(nbt, "operator");
        JsonElement value = nbt.has("value") ? nbt.get("value").deepCopy() : null;
        return new EditableCondition(path, operator, value);
    }

    private static JsonObject toConditionObject(EditableCondition condition) {
        JsonObject object = new JsonObject();
        object.addProperty("path", condition.getPath());
        object.addProperty("operator", condition.getOperator());
        if (condition.getValue() != null) {
            object.add("value", condition.getValue().deepCopy());
        }
        return object;
    }

    private static void readDurabilityTriggers(EditableDurabilityModifier durability, JsonObject object) {
        if (!object.has("durability_triggers") || !object.get("durability_triggers").isJsonArray()) {
            return;
        }

        for (JsonElement trigger : object.getAsJsonArray("durability_triggers")) {
            if (trigger.isJsonPrimitive() && trigger.getAsJsonPrimitive().isString()) {
                durability.getTriggers().add(trigger.getAsString());
            }
        }
    }

    private static UUID readUuid(JsonObject object) {
        String value = readString(object, "uuid");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String readString(JsonObject object, String member) {
        if (!object.has(member) || !object.get(member).isJsonPrimitive()) {
            return null;
        }
        return object.get(member).getAsString();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
