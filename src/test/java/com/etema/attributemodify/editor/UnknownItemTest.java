package com.etema.attributemodify.editor;

import com.etema.attributemodify.editor.model.EditableItemRule;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UnknownItemTest {
    @Test
    void rejectsUnknownItem() {
        EditableItemRule rule = new EditableItemRule(EditorRuleValidatorTest.id("example:missing"), false);

        assertFalse(EditorRuleValidatorTest.validator().validate(rule).isValid());
    }
}
