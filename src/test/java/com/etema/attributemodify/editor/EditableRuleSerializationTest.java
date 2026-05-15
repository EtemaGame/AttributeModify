package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableItemRule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditableRuleSerializationTest {
    @Test
    void parsesAndSerializesStableFields() {
        JsonObject input = JsonParser.parseString("""
                {
                  "minecraft:diamond_sword": {
                    "equipment_slots": {
                      "mainhand": [
                        {
                          "attribute": "minecraft:generic.attack_damage",
                          "action": "modify",
                          "amount": 10.0,
                          "operation": "addition",
                          "modifier_id": "attributemodify:test_modifier",
                          "uuid": "00000000-0000-0000-0000-000000000001",
                          "nbt": {
                            "path": "quality",
                            "operator": "equals",
                            "value": "legendary"
                          }
                        }
                      ]
                    },
                    "durability": 2000,
                    "durability_triggers": ["melee_hit"]
                  }
                }
                """).getAsJsonObject();

        List<EditableItemRule> rules = EditorRuleSerializer.fromDocument(input);
        JsonObject output = EditorRuleSerializer.toDocument(rules);

        JsonObject item = output.getAsJsonObject("minecraft:diamond_sword");
        JsonObject attribute = item.getAsJsonObject("equipment_slots")
                .getAsJsonArray("mainhand")
                .get(0)
                .getAsJsonObject();

        assertEquals("modify", attribute.get("action").getAsString());
        assertEquals("minecraft:generic.attack_damage", attribute.get("attribute").getAsString());
        assertEquals("addition", attribute.get("operation").getAsString());
        assertEquals("attributemodify:test_modifier", attribute.get("modifier_id").getAsString());
        assertTrue(attribute.has("nbt"));
        assertEquals(2000, item.get("durability").getAsInt());
        assertEquals("melee_hit", item.getAsJsonArray("durability_triggers").get(0).getAsString());
    }
}
