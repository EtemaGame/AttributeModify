package com.etema.attributemodify.service;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributeApplicationServiceTest {

    @Test
    void exactSetAmountConvertsTargetToDelta() {
        assertEquals(3.5, AttributeApplicationService.exactSetAmount(10.0, 6.5));
    }

    @Test
    void equivalentModifierMatchesOnAllRelevantFields() {
        AttributeModifier reference = new AttributeModifier(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "test_modifier",
                2.0,
                AttributeModifier.Operation.ADDITION);

        AttributeModifier same = new AttributeModifier(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "test_modifier",
                2.0,
                AttributeModifier.Operation.ADDITION);

        assertTrue(AttributeApplicationService.containsEquivalentModifier(List.of(reference), same));
    }

    @Test
    void differentModifierDoesNotMatch() {
        AttributeModifier reference = new AttributeModifier(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "test_modifier",
                2.0,
                AttributeModifier.Operation.ADDITION);

        AttributeModifier different = new AttributeModifier(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "test_modifier",
                2.0,
                AttributeModifier.Operation.ADDITION);

        assertFalse(AttributeApplicationService.containsEquivalentModifier(List.of(reference), different));
    }
}
