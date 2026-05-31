package com.etema.attributemodify.service;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service specialized in resolving which attribute rules apply to a specific item and slot context.
 */
public class AttributeResolutionService {

    /**
     * Resolves and filters applicable attribute entries for a given item stack and slot.
     */
    public static List<ItemAttributeDataManager.AttributeEntry> resolveEntries(ItemStack stack, EquipmentSlot slot) {
        return resolveEntries(ItemAttributeDataManager.getInstance(), stack, slot);
    }

    public static List<ItemAttributeDataManager.AttributeEntry> resolveEntries(ItemAttributeDataManager dataManager,
            ItemStack stack, EquipmentSlot slot) {
        Collection<ItemAttributeDataManager.AttributeEntry> entries = dataManager.getEntriesForSlot(stack.getItem(),
                slot);
        if (entries.isEmpty()) {
            return List.of();
        }

        List<ItemAttributeDataManager.AttributeEntry> matches = new ArrayList<>(entries.size());
        for (ItemAttributeDataManager.AttributeEntry entry : entries) {
            if (entry.attribute() != null && entry.matches(stack)) {
                matches.add(entry);
            }
        }
        return matches.isEmpty() ? List.of() : matches;
    }

    /**
     * Checks if an item has any custom attribute data at all.
     */
    public static boolean hasCustomAttributes(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ItemAttributeDataManager.getInstance().hasCustomAttributes(stack.getItem());
    }
    
    /**
     * Checks if an item is marked as decorative (all modifiers removed).
     */
    public static boolean isDecorative(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ItemAttributeDataManager.getInstance().isDecorative(stack.getItem());
    }
}
