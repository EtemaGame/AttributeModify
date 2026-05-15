package com.etema.attributemodify.editor;

import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public final class EditorConflictDetector {
    private EditorConflictDetector() {
    }

    public static boolean hasRuntimeRuleForItem(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }

        ItemAttributeDataManager manager = ItemAttributeDataManager.getInstance();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!manager.getEntriesForSlot(item, slot).isEmpty()) {
                return true;
            }
        }

        return manager.getDurabilityRule(item) != null || !manager.getCuriosAttributesForSync().getOrDefault(item, java.util.Map.of()).isEmpty();
    }
}
