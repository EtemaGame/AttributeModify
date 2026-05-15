package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UnknownAttributeTest {
    @Test
    void rejectsUnknownAttribute() {
        EditableItemRule rule = new EditableItemRule(EditorRuleValidatorTest.id("minecraft:diamond_sword"), false);
        rule.getAttributes().add(new EditableAttributeModifier(
                EditorRuleValidatorTest.id("example:missing_attribute"),
                EditableAttributeAction.ADD,
                1.0,
                EditableOperationType.ADDITION,
                EditableSlotType.STANDARD,
                "mainhand"));

        assertFalse(EditorRuleValidatorTest.validator().validate(rule).isValid());
    }
}
