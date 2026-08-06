package com.etema.attributemodify;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAttributeDataManagerTest {

    @Test
    void discoversAllConcreteCuriosSlotsDeterministically() {
        List<String> slots = ItemAttributeDataManager.selectCuriosSlotIds(List.of(
                ResourceLocation.tryParse("curios:ring"),
                ResourceLocation.tryParse("curios:hands"),
                ResourceLocation.tryParse("curios:bracelet"),
                ResourceLocation.tryParse("curios:hands"),
                ResourceLocation.tryParse("artifacts:slot/hands")));

        assertEquals(List.of("bracelet", "hands", "ring"), slots);
    }

    @Test
    void ignoresGenericCuriosAggregateTags() {
        List<String> slots = ItemAttributeDataManager.selectCuriosSlotIds(List.of(
                ResourceLocation.tryParse("curios:all"),
                ResourceLocation.tryParse("curios:curio")));

        assertEquals(List.of(), slots);
    }

    @Test
    void universalCurioSlotCollectsEveryConfiguredSlot() {
        Map<String, List<String>> slots = new LinkedHashMap<>();
        slots.put("ring", List.of("ring-rule"));
        slots.put("hands", List.of("hands-rule"));
        slots.put("curio", List.of("universal-rule"));

        assertEquals(List.of("universal-rule", "hands-rule", "ring-rule"),
                ItemAttributeDataManager.selectEntriesForUniversalSlot(slots, "curio", "curio"));
        assertEquals(List.of("ring-rule"),
                ItemAttributeDataManager.selectEntriesForUniversalSlot(slots, "RING", "curio"));
    }

    @Test
    void removeOperationOnlySuppressesMatchingRepresentation() {
        assertTrue(ItemAttributeDataManager.removalOperationMatches(
                null, AttributeModifier.Operation.MULTIPLY_TOTAL));
        assertTrue(ItemAttributeDataManager.removalOperationMatches(
                AttributeModifier.Operation.ADDITION, AttributeModifier.Operation.ADDITION));
        assertFalse(ItemAttributeDataManager.removalOperationMatches(
                AttributeModifier.Operation.ADDITION, AttributeModifier.Operation.MULTIPLY_BASE));
    }
}
