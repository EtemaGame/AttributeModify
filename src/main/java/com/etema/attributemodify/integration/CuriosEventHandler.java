package com.etema.attributemodify.integration;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.service.AttributeResolutionService;
import com.etema.attributemodify.service.AttributeApplicationService;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;

public class CuriosEventHandler {
    @SubscribeEvent
    public void onCurioAttributeModifierEvent(CurioAttributeModifierEvent event) {
        try {
            ItemStack itemStack = event.getItemStack();
            if (itemStack.isEmpty())
                return;

            var ctx = event.getSlotContext();
            String slotIdentifier = ctx.identifier();
            int slotIndex = ctx.index();

            ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
            if (!dataManager.hasCustomAttributes(itemStack.getItem()))
                return;

            Collection<ItemAttributeDataManager.AttributeEntry> entries = dataManager
                    .getEntriesForCuriosSlot(itemStack.getItem(), slotIdentifier);

            for (ItemAttributeDataManager.AttributeEntry entry : entries) {
                if (entry.action() == ItemAttributeDataManager.AttributeAction.SET) {
                    continue;
                }

                Attribute attribute = entry.attribute();
                if (attribute == null)
                    continue;

                if (!entry.matches(itemStack)) {
                    continue;
                }

                switch (entry.action()) {
                    case REMOVE -> {
                        removeAttribute(event, attribute, entry.targetOperation());
                    }
                    case MODIFY -> {
                        event.removeAttribute(attribute);
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null) {
                            event.addModifier(attribute, makeCuriosInstanceUnique(modifier, slotIdentifier, slotIndex));
                        }
                    }
                    case ADD -> {
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null) {
                            event.addModifier(attribute, makeCuriosInstanceUnique(modifier, slotIdentifier, slotIndex));
                        }
                    }
                    case SET -> {
                        // handled in the second pass
                    }
                }
            }

            for (ItemAttributeDataManager.AttributeEntry entry : entries) {
                if (entry.action() != ItemAttributeDataManager.AttributeAction.SET) {
                    continue;
                }

                Attribute attribute = entry.attribute();
                if (attribute == null || !entry.matches(itemStack)) {
                    continue;
                }

                event.removeAttribute(attribute);
                AttributeModifier modifier = entry.modifier();
                if (modifier != null) {
                    AttributeModifier exact = new AttributeModifier(
                            modifier.getId(),
                            modifier.getName(),
                            modifier.getAmount() - attribute.getDefaultValue(),
                            AttributeModifier.Operation.ADDITION
                    );
                    event.addModifier(attribute, makeCuriosInstanceUnique(exact, slotIdentifier, slotIndex));
                }
            }

        } catch (Exception e) {
            AttributeModify.LOGGER.error("Error processing Curios event: {}", e.getMessage(), e);
        }
    }

    private static void removeAttribute(CurioAttributeModifierEvent event, Attribute attribute,
            AttributeModifier.Operation targetOperation) {
        for (AttributeModifier modifier : java.util.List.copyOf(event.getModifiers().get(attribute))) {
            if (ItemAttributeDataManager.removalOperationMatches(targetOperation, modifier.getOperation())) {
                event.removeModifier(attribute, modifier);
            }
        }
    }

    /**
     * Genera un UUID único para el modificador basado en el slot e índice.
     * Importante para evitar conflictos cuando se equipan items iguales en slots
     * diferentes (ej: 2 anillos).
     */
    private static AttributeModifier makeCuriosInstanceUnique(AttributeModifier base, String slotId, int index) {
        UUID oldId = base.getId();
        String slotSafe = slotId.replace(":", "_").replace(".", "_").toLowerCase();

        // Generar nuevo UUID determinista basado en ID original + slot + index
        String newIdString = oldId.toString() + "_" + slotSafe + "_" + index;
        UUID newUuid = UUID.nameUUIDFromBytes(newIdString.getBytes(StandardCharsets.UTF_8));

        return new AttributeModifier(newUuid, base.getName(), base.getAmount(), base.getOperation());
    }
}
