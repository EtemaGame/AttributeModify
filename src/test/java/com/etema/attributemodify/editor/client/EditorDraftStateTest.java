package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.editor.model.EditableAttributeAction;
import com.etema.attributemodify.editor.model.EditableAttributeModifier;
import com.etema.attributemodify.editor.model.EditableOperationType;
import com.etema.attributemodify.editor.model.EditableSlotType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorDraftStateTest {
    @Test
    void resetAndRegistryDraftSetExpectedDefaults() {
        EditorDraftState state = new EditorDraftState();

        state.selectedBaseAttributeIndex(3);
        state.selectedModifierIndex(4);
        state.draftSource(DraftSource.BASE);
        state.action(EditableAttributeAction.REMOVE);
        state.operationType(EditableOperationType.MULTIPLY_TOTAL);
        state.slot("offhand");

        state.resetDraft();

        assertEquals(DraftSource.NONE, state.draftSource());
        assertEquals(EditableAttributeAction.ADD, state.action());
        assertEquals(EditableOperationType.ADDITION, state.operationType());
        assertEquals(EditableSlotType.AUTO, state.slotType());
        assertEquals("mainhand", state.slot());
        assertEquals(-1, state.selectedBaseAttributeIndex());
        assertEquals(-1, state.selectedModifierIndex());

        state.startNewAttributeDraft();

        assertEquals(DraftSource.REGISTRY, state.draftSource());
        assertEquals(EditableAttributeAction.ADD, state.action());
        assertEquals(EditableOperationType.ADDITION, state.operationType());
        assertEquals(EditableSlotType.AUTO, state.slotType());
        assertEquals("mainhand", state.slot());
        assertEquals(-1, state.selectedBaseAttributeIndex());
        assertEquals(-1, state.selectedModifierIndex());
    }

    @Test
    void loadModifierAndBaseAttributeCarryValuesIntoState() {
        EditorDraftState state = new EditorDraftState();
        ResourceLocation attribute = ResourceLocation.tryParse("minecraft:generic.attack_damage");
        if (attribute == null) {
            throw new IllegalStateException("Failed to parse attribute id");
        }

        state.loadModifierIntoDraft(new EditableAttributeModifier(
                attribute,
                EditableAttributeAction.REMOVE,
                2.5,
                EditableOperationType.MULTIPLY_BASE,
                EditableSlotType.STANDARD,
                "chest"
        ));

        assertEquals(DraftSource.MODIFIER, state.draftSource());
        assertEquals(EditableAttributeAction.REMOVE, state.action());
        assertEquals(EditableOperationType.MULTIPLY_BASE, state.operationType());
        assertEquals(EditableSlotType.STANDARD, state.slotType());
        assertEquals("chest", state.slot());

        state.loadBaseAttributeIntoDraft(attribute, 4.0, EditableOperationType.MULTIPLY_TOTAL, "legs");

        assertEquals(DraftSource.BASE, state.draftSource());
        assertEquals(EditableAttributeAction.MODIFY, state.action());
        assertEquals(EditableOperationType.MULTIPLY_TOTAL, state.operationType());
        assertEquals(EditableSlotType.STANDARD, state.slotType());
        assertEquals("legs", state.slot());
    }
}
