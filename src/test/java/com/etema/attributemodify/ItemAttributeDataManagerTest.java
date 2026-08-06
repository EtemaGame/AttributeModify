package com.etema.attributemodify;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void decorativeTooltipModeDefaultsToStrictForExistingDatapacks() {
        assertEquals(ItemAttributeDataManager.DecorativeTooltipMode.STRICT,
                ItemAttributeDataManager.DecorativeTooltipMode.parse(new JsonObject()));
    }

    @Test
    void decorativeTooltipModeAcceptsPreserveOptIn() {
        JsonObject itemData = new JsonObject();
        itemData.addProperty("decorative_tooltip", "preserve");

        assertEquals(ItemAttributeDataManager.DecorativeTooltipMode.PRESERVE,
                ItemAttributeDataManager.DecorativeTooltipMode.parse(itemData));
    }

    @Test
    void parsesDocumentedQualitySystemConfiguration() {
        JsonObject common = new JsonObject();
        common.addProperty("value", "common");
        common.addProperty("weight", 3);
        JsonObject rare = new JsonObject();
        rare.addProperty("value", "rare");
        rare.addProperty("weight", 1);
        JsonArray levels = new JsonArray();
        levels.add(common);
        levels.add(rare);
        JsonArray triggers = new JsonArray();
        triggers.add("craft");
        triggers.add("loot");
        JsonObject quality = new JsonObject();
        quality.addProperty("tag_path", "attributemodify.quality");
        quality.add("triggers", triggers);
        quality.add("levels", levels);

        ItemAttributeDataManager.QualityConfig config = ItemAttributeDataManager.getInstance()
                .parseQualityConfig(null, quality);

        assertEquals("attributemodify.quality", config.tagPath());
        assertEquals(Set.of("craft", "loot"), config.triggers());
        assertEquals(4, config.totalWeight());
        assertEquals(2, config.levels().size());
    }
}
