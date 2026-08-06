package com.etema.attributemodify.service;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipPresentationServiceTest {

    @Test
    void mergesRepeatedCuriosSlotSectionsBeforeMetadata() {
        Component speed = attributeLine("attribute.name.generic.attack_speed", "0.4");
        Component damage = attributeLine("attribute.name.generic.attack_damage", "10");
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.literal("Feral Claws"),
                Component.translatable("curios.modifiers.hands"),
                speed,
                Component.literal("artifacts:feral_claws"),
                Component.empty(),
                Component.translatable("curios.modifiers.hands"),
                damage));

        TooltipPresentationService.mergeDuplicateAttributeSections(tooltip);

        assertEquals(5, tooltip.size());
        assertEquals(speed, tooltip.get(2));
        assertEquals(damage, tooltip.get(3));
        assertEquals("artifacts:feral_claws", tooltip.get(4).getString());
    }

    @Test
    void doesNotRepeatAnIdenticalAttributeLine() {
        Component damage = attributeLine("attribute.name.generic.attack_damage", "10");
        List<Component> tooltip = new ArrayList<>(List.of(
                Component.translatable("curios.modifiers.hands"),
                damage,
                Component.empty(),
                Component.translatable("curios.modifiers.hands"),
                damage.copy()));

        TooltipPresentationService.mergeDuplicateAttributeSections(tooltip);

        assertEquals(2, tooltip.size());
        assertEquals(damage.getString(), tooltip.get(1).getString());
    }

    private static Component attributeLine(String attributeKey, String amount) {
        return Component.translatable("attribute.modifier.plus.0", amount, Component.translatable(attributeKey));
    }
}
