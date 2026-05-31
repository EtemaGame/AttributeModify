package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateAttributeTest {
    @Test
    void rejectsSameAttributeOperationAndSlot() {
        EditableItemRule rule = new EditableItemRule(EditorRuleValidatorTest.id("minecraft:diamond_sword"), false);
        ResourceLocation attribute = EditorRuleValidatorTest.id("minecraft:generic.attack_damage");
        rule.getAttributes().add(new EditableAttributeModifier(attribute, EditableAttributeAction.ADD, 1.0,
                EditableOperationType.ADDITION, EditableSlotType.STANDARD, "mainhand"));
        rule.getAttributes().add(new EditableAttributeModifier(attribute, EditableAttributeAction.ADD, 2.0,
                EditableOperationType.ADDITION, EditableSlotType.STANDARD, "mainhand"));

        assertFalse(EditorRuleValidatorTest.validator().validate(rule).isValid());
    }

    @Test
    void allowsDifferentActionsForSameAttributeAndSlot() {
        EditableItemRule rule = new EditableItemRule(EditorRuleValidatorTest.id("minecraft:diamond_sword"), false);
        ResourceLocation attribute = EditorRuleValidatorTest.id("minecraft:generic.attack_damage");
        rule.getAttributes().add(new EditableAttributeModifier(attribute, EditableAttributeAction.MODIFY, 1.0,
                EditableOperationType.ADDITION, EditableSlotType.STANDARD, "mainhand"));
        rule.getAttributes().add(new EditableAttributeModifier(attribute, EditableAttributeAction.SET, 5.0,
                null, EditableSlotType.STANDARD, "mainhand"));

        assertTrue(EditorRuleValidatorTest.validator().validate(rule).isValid());
    }
}
