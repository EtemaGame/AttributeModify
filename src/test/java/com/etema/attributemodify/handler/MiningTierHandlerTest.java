package com.etema.attributemodify.handler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MiningTierHandlerTest {
    @Test
    void configuredSpeedActsAsBaseReference() {
        assertEquals(8.0f, MiningTierHandler.computeConfiguredSpeed(8.0f, 10.0f, 10.0f));
    }

    @Test
    void speedBonusesRemainProportional() {
        assertEquals(12.0f, MiningTierHandler.computeConfiguredSpeed(8.0f, 15.0f, 10.0f));
    }

    @Test
    void speedPenaltiesRemainProportional() {
        assertEquals(4.0f, MiningTierHandler.computeConfiguredSpeed(8.0f, 5.0f, 10.0f));
    }

    @Test
    void normalizesVanillaAliasesAndModdedIds() {
        assertEquals("wood", MiningTierHandler.normalizeTierName("wooden"));
        assertEquals("gold", MiningTierHandler.normalizeTierName("GOLDEN"));
        assertEquals("examplemod:my_tier", MiningTierHandler.normalizeTierName(" ExampleMod:My_Tier "));
        assertNull(MiningTierHandler.normalizeTierName(" "));
    }
}
