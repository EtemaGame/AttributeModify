package com.etema.attributemodify.compat;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorativeItemPolicyTest {
    @Test
    void existingDecorativeRulesKeepEveryBlockEnabled() {
        assertEquals(DecorativeItemPolicy.DEFAULT, DecorativeItemPolicy.fromItemJson(new JsonObject()));
    }

    @Test
    void explicitOptionsCanPreserveEffectsWithoutEnablingCombat() {
        JsonObject options = new JsonObject();
        options.addProperty("block_all_effects", false);
        options.addProperty("clear_existing_effects", false);
        options.addProperty("block_attack", true);
        JsonObject item = new JsonObject();
        item.add("decorative_options", options);

        DecorativeItemPolicy policy = DecorativeItemPolicy.fromItemJson(item);

        assertFalse(policy.blockAllEffects());
        assertFalse(policy.clearExistingEffects());
        assertTrue(policy.blockAttack());
        assertTrue(policy.blockUse());
    }

    @Test
    void malformedOptionsFallBackToSafeDefaults() {
        JsonObject options = new JsonObject();
        options.addProperty("block_use", "not-a-boolean");
        JsonObject item = new JsonObject();
        item.add("decorative_options", options);

        assertTrue(DecorativeItemPolicy.fromItemJson(item).blockUse());
    }
}
