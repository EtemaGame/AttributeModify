package com.etema.attributemodify;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemAttributeHandler {
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack itemStack = event.getItemStack();
        EquipmentSlot slot = event.getSlotType();

        if (itemStack.isEmpty()) {
            return;
        }

        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();

        if (!dataManager.hasCustomAttributes(itemStack.getItem())) {
            return;
        }

        if (dataManager.isDecorative(itemStack.getItem())) {
            event.clearModifiers();
            // Log removed to reduce spam
            return;
        }

        try {
            Collection<ItemAttributeDataManager.AttributeEntry> entries = dataManager
                    .getEntriesForSlot(itemStack.getItem(), slot);

            if (entries.isEmpty()) {
                return;
            }

            for (ItemAttributeDataManager.AttributeEntry entry : entries) {
                Attribute attribute = entry.attribute();
                if (attribute == null) {
                    if (AttributeModify.DEBUG_MODE) {
                        AttributeModify.LOGGER.warn("[Handler] Null attribute found for item {} in slot {}",
                                itemStack.getItem(),
                                slot);
                    }
                    continue;
                }

                boolean nbtMatch = entry.matches(itemStack);
                if (!nbtMatch) {
                    // Log removed to reduce spam
                    continue;
                }

                switch (entry.action()) {
                    case REMOVE -> {
                        boolean had = event.getModifiers().containsKey(attribute);
                        event.removeAttribute(attribute);
                        // Log removed to reduce spam
                    }
                    case MODIFY -> {
                        // Only modify if the item currently has this attribute in this slot
                        if (!event.getModifiers().containsKey(attribute)) {
                                // Log removed to reduce spam
                            continue;
                        }
                        // Replace only the base modifier (from vanilla/item), preserve bonus modifiers
                        // from other mods
                        Collection<AttributeModifier> currentModifiers = event.getModifiers().get(attribute);
                        Collection<AttributeModifier> originalModifiers = event.getOriginalModifiers().get(attribute);
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null && !currentModifiers.isEmpty()) {
                            List<AttributeModifier> extrasToKeep = new ArrayList<>();
                            for (AttributeModifier current : currentModifiers) {
                                boolean isOriginal = false;
                                for (AttributeModifier orig : originalModifiers) {
                                    if (current.getId().equals(orig.getId())) {
                                        isOriginal = true;
                                        break;
                                    }
                                }
                                if (!isOriginal) {
                                    extrasToKeep.add(current);
                                }
                            }
                            event.removeAttribute(attribute);
                            event.addModifier(attribute, modifier);
                            for (AttributeModifier extra : extrasToKeep) {
                                event.addModifier(attribute, extra);
                            }
                                // Log removed to reduce spam
                        }
                    }
                    case ADD -> {
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null) {
                            event.addModifier(attribute, modifier);
                            // Log removed to reduce spam
                        } else {
                            if (AttributeModify.DEBUG_MODE) {
                                AttributeModify.LOGGER.warn("[Handler] ADD {} on {} in slot {} -> modifier is NULL!",
                                        attribute.getDescriptionId(), itemStack.getItem(), slot);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (AttributeModify.DEBUG_MODE) {
                AttributeModify.LOGGER.error("[Handler] Error applying attributes for {} in slot {}: {}",
                        itemStack.getItem(), slot, e.getMessage(), e);
            }
        }
    }

}
