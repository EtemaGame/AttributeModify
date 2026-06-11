package com.etema.attributemodify.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.google.common.collect.LinkedHashMultimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

class AttributeApplicationServiceTest {
    @Test
    void exactSetAmountConvertsTargetValueIntoModifierDelta() {
        assertEquals(8.0, AttributeApplicationService.exactSetAmount(11.0, 3.0));
        assertEquals(0.0, AttributeApplicationService.exactSetAmount(5.0, 5.0));
        assertEquals(-2.5, AttributeApplicationService.exactSetAmount(7.5, 10.0));
    }

    @Test
    void replaceAttributePreservingOrderKeepsOriginalPositionWithoutTouchingExternalEntries() {
        Attribute attackSpeed = attribute("attack_speed");
        Attribute attackDamage = attribute("attack_damage");

        UUID originalDamageId = UUID.randomUUID();
        UUID externalDamageId = UUID.randomUUID();

        List<Map.Entry<Attribute, AttributeModifier>> modifiers = new ArrayList<>();
        modifiers.add(Map.entry(attackSpeed, new AttributeModifier(UUID.randomUUID(), "speed", 1.6,
                AttributeModifier.Operation.ADDITION)));
        modifiers.add(Map.entry(attackDamage, new AttributeModifier(originalDamageId, "damage", 6.0,
                AttributeModifier.Operation.ADDITION)));
        modifiers.add(Map.entry(attackDamage, new AttributeModifier(externalDamageId, "external_damage", 2.0,
                AttributeModifier.Operation.ADDITION)));

        AttributeApplicationService.replaceAttributePreservingOrder(modifiers, attackDamage,
                new AttributeModifier(UUID.randomUUID(), "damage_updated", 12.0,
                        AttributeModifier.Operation.ADDITION),
                List.of(new AttributeModifier(originalDamageId, "damage", 6.0,
                        AttributeModifier.Operation.ADDITION)));

        assertIterableEquals(List.of(attackSpeed, attackDamage, attackDamage),
                List.of(modifiers.get(0).getKey(), modifiers.get(1).getKey(), modifiers.get(2).getKey()));
        assertEquals(12.0, modifiers.get(1).getValue().getAmount());
        assertEquals(2.0, modifiers.get(2).getValue().getAmount());
    }

    @Test
    void removeOriginalAttributeModifiersKeepsExternalEntries() {
        Attribute attackDamage = attribute("attack_damage");

        UUID originalDamageId = UUID.randomUUID();
        UUID externalDamageId = UUID.randomUUID();

        List<Map.Entry<Attribute, AttributeModifier>> modifiers = new ArrayList<>();
        modifiers.add(Map.entry(attackDamage, new AttributeModifier(originalDamageId, "damage", 6.0,
                AttributeModifier.Operation.ADDITION)));
        modifiers.add(Map.entry(attackDamage, new AttributeModifier(externalDamageId, "external_damage", 2.0,
                AttributeModifier.Operation.ADDITION)));

        AttributeApplicationService.removeOriginalAttributeModifiers(modifiers, attackDamage,
                List.of(new AttributeModifier(originalDamageId, "damage", 6.0,
                        AttributeModifier.Operation.ADDITION)));

        assertIterableEquals(List.of(attackDamage), List.of(modifiers.get(0).getKey()));
        assertEquals(2.0, modifiers.get(0).getValue().getAmount());
    }

    @Test
    void buildOrderedModifiersKeepsInsertionOrderStable() {
        Attribute attackSpeed = attribute("attack_speed");
        Attribute attackDamage = attribute("attack_damage");

        List<Map.Entry<Attribute, AttributeModifier>> finalModifiers = new ArrayList<>();
        finalModifiers.add(Map.entry(attackSpeed, new AttributeModifier(UUID.randomUUID(), "speed", 1.6,
                AttributeModifier.Operation.ADDITION)));
        finalModifiers.add(Map.entry(attackDamage, new AttributeModifier(UUID.randomUUID(), "damage_updated", 11.0,
                AttributeModifier.Operation.ADDITION)));

        LinkedHashMultimap<Attribute, AttributeModifier> ordered = AttributeApplicationService.buildOrderedModifiers(finalModifiers);

        List<Attribute> keys = new ArrayList<>();
        for (Map.Entry<Attribute, AttributeModifier> modifier : ordered.entries()) {
            keys.add(modifier.getKey());
        }

        assertIterableEquals(List.of(attackSpeed, attackDamage), keys);
        assertEquals(11.0, ordered.get(attackDamage).iterator().next().getAmount());
    }

    private static Attribute attribute(String id) {
        return new Attribute("attribute.test." + id, 0.0) {};
    }
}
