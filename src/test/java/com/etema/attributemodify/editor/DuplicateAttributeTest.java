package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
