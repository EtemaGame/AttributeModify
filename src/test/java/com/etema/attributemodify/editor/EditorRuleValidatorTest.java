package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableMiningOverride;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import com.etema.attributemodify.editor.model.EditableValidationResult;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorRuleValidatorTest {
    @Test
    void acceptsValidAttributeRule() {
        EditableItemRule rule = new EditableItemRule(id("minecraft:diamond_sword"), false);
        rule.getAttributes().add(new EditableAttributeModifier(
                id("minecraft:generic.attack_damage"),
                EditableAttributeAction.MODIFY,
                10.0,
                EditableOperationType.ADDITION,
                EditableSlotType.STANDARD,
                "mainhand"));

        EditableValidationResult result = validator().validate(rule);

        assertTrue(result.isValid(), () -> String.join(", ", result.errors()));
    }

    @Test
    void acceptsBodyAliasAndMiningRule() {
        EditableItemRule rule = new EditableItemRule(id("minecraft:diamond_sword"), false);
        rule.getAttributes().add(new EditableAttributeModifier(
                id("minecraft:generic.attack_damage"),
                EditableAttributeAction.ADD,
                1.0,
                EditableOperationType.ADDITION,
                EditableSlotType.STANDARD,
                "body"));
        rule.getMiningOverrides().add(new EditableMiningOverride(8.0f, "iron"));

        EditableValidationResult result = validator().validate(rule);

        assertTrue(result.isValid(), () -> String.join(", ", result.errors()));
    }

    static EditorRuleValidator validator() {
        return new EditorRuleValidator(EditorValidationContext.simple(
                Set.of(id("minecraft:diamond_sword")),
                Set.of(id("minecraft:generic.attack_damage"))));
    }

    static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException(value);
        }
        return id;
    }
}
