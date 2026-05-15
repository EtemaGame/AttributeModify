package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.EditorCatalogService.EditorCatalog;
import com.etema.attributemodify.editor.EditorCatalogService.EditorAttributeInfo;
import com.etema.attributemodify.editor.EditorCatalogService.EditorItemInfo;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class EditorJsonPayloads {
    private EditorJsonPayloads() {
    }

    public static JsonObject catalogToJson(EditorCatalog catalog) {
        JsonObject root = new JsonObject();
        JsonArray items = new JsonArray();
        for (EditorItemInfo item : catalog.items()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", item.id().toString());
            object.addProperty("descriptionId", item.descriptionId());
            object.addProperty("maxDamage", item.maxDamage());
            object.addProperty("damageable", item.damageable());
            object.addProperty("namespace", item.namespace());
            items.add(object);
        }
        root.add("items", items);

        JsonArray attributes = new JsonArray();
        for (EditorAttributeInfo attribute : catalog.attributes()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", attribute.id().toString());
            object.addProperty("descriptionId", attribute.descriptionId());
            attributes.add(object);
        }
        root.add("attributes", attributes);

        root.add("equipmentSlots", stringArray(catalog.equipmentSlots()));
        root.add("curiosSlots", stringArray(catalog.curiosSlots()));
        root.add("namespaces", stringArray(catalog.namespaces()));

        JsonArray tags = new JsonArray();
        catalog.itemTags().forEach(tag -> tags.add(tag.toString()));
        root.add("itemTags", tags);
        root.addProperty("curiosLoaded", catalog.curiosLoaded());
        return root;
    }

    public static String ruleToPayload(EditableItemRule rule) {
        return ruleToPayload(rule, false, new JsonArray());
    }

    public static String ruleToPayload(EditableItemRule rule, boolean externalConflict) {
        return ruleToPayload(rule, externalConflict, new JsonArray());
    }

    public static String ruleToPayload(EditableItemRule rule, boolean externalConflict, JsonArray baseAttributes) {
        JsonObject root = new JsonObject();
        if (rule != null) {
            root.addProperty("targetId", rule.getTargetId().toString());
            root.addProperty("tagTarget", rule.isTagTarget());
            root.add("rule", EditorRuleSerializer.toItemObject(rule));
        }
        root.addProperty("externalConflict", externalConflict);
        root.add("baseAttributes", baseAttributes == null ? new JsonArray() : baseAttributes.deepCopy());
        return root.toString();
    }

    public static Optional<EditableItemRule> ruleFromPayload(String payload) {
        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            ResourceLocation targetId = ResourceLocation.tryParse(root.get("targetId").getAsString());
            boolean tagTarget = root.has("tagTarget") && root.get("tagTarget").getAsBoolean();
            if (targetId == null || !root.has("rule") || !root.get("rule").isJsonObject()) {
                return Optional.empty();
            }
            return Optional.of(EditorRuleSerializer.fromItemObject(targetId, tagTarget, root.getAsJsonObject("rule")));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public static JsonObject saveResult(boolean success, String message) {
        JsonObject root = new JsonObject();
        root.addProperty("success", success);
        root.addProperty("message", message == null ? "" : message);
        return root;
    }

    private static JsonArray stringArray(Iterable<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
