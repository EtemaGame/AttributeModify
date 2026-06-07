package com.etema.attributemodify.editor;

import com.etema.attributemodify.integration.CuriosIntegration;
import com.etema.attributemodify.integration.AccessoriesIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record EditorValidationContext(
        Set<ResourceLocation> itemIds,
        Set<ResourceLocation> attributeIds,
        Set<String> equipmentSlots,
        Set<String> curiosSlots,
        Set<String> accessoriesSlots,
        Set<ResourceLocation> miningTiers,
        boolean curiosEnabled,
        boolean accessoriesEnabled) {

    public static EditorValidationContext live() {
        Set<ResourceLocation> items = new LinkedHashSet<>();
        ForgeRegistries.ITEMS.getKeys().forEach(items::add);

        Set<ResourceLocation> attributes = new LinkedHashSet<>();
        ForgeRegistries.ATTRIBUTES.getKeys().forEach(attributes::add);

        Set<String> standardSlots = new LinkedHashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            standardSlots.add(slot.getName());
        }

        Set<ResourceLocation> miningTiers = new LinkedHashSet<>();
        for (var tier : TierSortingRegistry.getSortedTiers()) {
            ResourceLocation id = TierSortingRegistry.getName(tier);
            if (id != null) {
                miningTiers.add(id);
            }
        }

        return new EditorValidationContext(
                Set.copyOf(items),
                Set.copyOf(attributes),
                Set.copyOf(standardSlots),
                Set.of(),
                Set.of(),
                Set.copyOf(miningTiers),
                CuriosIntegration.shouldProcessCuriosSlots(),
                AccessoriesIntegration.shouldProcessAccessoriesSlots());
    }

    public static EditorValidationContext simple(Set<ResourceLocation> itemIds, Set<ResourceLocation> attributeIds) {
        return new EditorValidationContext(
                itemIds == null ? Set.of() : Set.copyOf(itemIds),
                attributeIds == null ? Set.of() : Set.copyOf(attributeIds),
                Set.of("mainhand", "offhand", "feet", "legs", "chest", "head"),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                false);
    }

    public boolean hasEquipmentSlot(String slot) {
        return slot != null && equipmentSlots.contains(slot.toLowerCase(Locale.ROOT));
    }

    public boolean hasCuriosSlot(String slot) {
        return curiosEnabled && slot != null && (curiosSlots.isEmpty() || curiosSlots.contains(slot));
    }

    public boolean hasAccessoriesSlot(String slot) {
        return accessoriesEnabled && slot != null && (accessoriesSlots.isEmpty() || accessoriesSlots.contains(slot));
    }

    public boolean hasMiningTier(ResourceLocation tier) {
        return tier != null && (miningTiers.isEmpty() || miningTiers.contains(tier));
    }
}
