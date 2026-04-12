package com.etema.attributemodify.service;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service specialized in resolving which attribute rules apply to a specific item and slot context.
 */
public class AttributeResolutionService {

    /**
     * Resolves and filters applicable attribute entries for a given item stack and slot.
     */
    public static List<ItemAttributeDataManager.AttributeEntry> resolveEntries(ItemStack stack, EquipmentSlot slot) {
        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        return dataManager.getEntriesForSlot(stack.getItem(), slot)
                .stream()
                .filter(entry -> entry.attribute() != null)
                .filter(entry -> entry.matches(stack))
                .collect(Collectors.toList());
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
