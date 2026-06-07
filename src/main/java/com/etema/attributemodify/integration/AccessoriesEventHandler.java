package com.etema.attributemodify.integration;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.AttributeModificationData;
import io.wispforest.accessories.api.events.AdjustAttributeModifierCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AccessoriesEventHandler {

    public AccessoriesEventHandler() {
        AdjustAttributeModifierCallback.EVENT.register(this::onAdjustAttributeModifierEvent);
    }

    private void onAdjustAttributeModifierEvent(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        try {
            if (stack.isEmpty()) {
                return;
            }

            String slotIdentifier = reference.slotName();
            int slotIndex = reference.slot();

            ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
            if (!dataManager.hasCustomAttributes(stack.getItem())) {
                return;
            }

            Collection<ItemAttributeDataManager.AttributeEntry> entries = new ArrayList<>(
                    dataManager.getEntriesForAccessoriesSlot(stack.getItem(), slotIdentifier));

            for (ItemAttributeDataManager.AttributeEntry entry : entries) {
                if (entry.action() == ItemAttributeDataManager.AttributeAction.SET) {
                    continue;
                }

                Attribute attribute = entry.attribute();
                if (attribute == null || !entry.matches(stack)) {
                    continue;
                }

                switch (entry.action()) {
                    case REMOVE -> removeAttribute(builder, attribute);
                    case MODIFY -> {
                        removeAttribute(builder, attribute);
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null) {
                            addExclusive(builder, attribute, modifier, slotIdentifier, slotIndex);
                        }
                    }
                    case ADD -> {
                        AttributeModifier modifier = entry.modifier();
                        if (modifier != null) {
                            addExclusive(builder, attribute, modifier, slotIdentifier, slotIndex);
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
                if (attribute == null || !entry.matches(stack)) {
                    continue;
                }

                removeAttribute(builder, attribute);
                AttributeModifier modifier = entry.modifier();
                if (modifier != null) {
                    AttributeModifier exact = new AttributeModifier(
                            modifier.getId(),
                            modifier.getName(),
                            modifier.getAmount() - attribute.getDefaultValue(),
                            AttributeModifier.Operation.ADDITION);
                    addExclusive(builder, attribute, exact, slotIdentifier, slotIndex);
                }
            }
        } catch (Exception e) {
            AttributeModify.LOGGER.error("Error processing Accessories event: {}", e.getMessage(), e);
        }
    }

    private static void removeAttribute(AccessoryAttributeBuilder builder, Attribute attribute) {
        List<ResourceLocation> exclusiveIds = builder.exclusiveAttributes()
                .getOrDefault(attribute, java.util.Map.of())
                .keySet()
                .stream()
                .toList();
        for (ResourceLocation id : exclusiveIds) {
            builder.removeExclusive(attribute, id);
        }

        for (AttributeModificationData data : List.copyOf(builder.stackedAttributes().get(attribute))) {
            ResourceLocation id = modifierLocation(data.modifier());
            builder.removeStacks(attribute, id);
        }
    }

    private static void addExclusive(AccessoryAttributeBuilder builder, Attribute attribute,
                                     AttributeModifier base, String slotId, int index) {
        AttributeModifier unique = makeAccessoriesInstanceUnique(base, slotId, index);
        builder.addExclusive(attribute, modifierLocation(unique), unique.getAmount(), unique.getOperation());
    }

    private static ResourceLocation modifierLocation(AttributeModifier modifier) {
        ResourceLocation parsed = ResourceLocation.tryParse(modifier.getName());
        if (parsed != null) {
            return parsed;
        }

        return new ResourceLocation(AttributeModify.MODID, sanitizePath(modifier.getName()));
    }

    private static AttributeModifier makeAccessoriesInstanceUnique(AttributeModifier base, String slotId, int index) {
        String slotSafe = sanitizePath(slotId);
        String modifierName = base.getName() + "_" + slotSafe + "_" + index;
        UUID uuid = UUID.nameUUIDFromBytes((base.getId() + "_" + slotSafe + "_" + index)
                .getBytes(StandardCharsets.UTF_8));
        return new AttributeModifier(uuid, modifierName, base.getAmount(), base.getOperation());
    }

    private static String sanitizePath(String value) {
        String safe = (value == null ? "unknown" : value)
                .replace(":", "_")
                .replace(".", "_")
                .toLowerCase(Locale.ROOT);
        return safe.replaceAll("[^a-z0-9_/.-]", "_");
    }
}
