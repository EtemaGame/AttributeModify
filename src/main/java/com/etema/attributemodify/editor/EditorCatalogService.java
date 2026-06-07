package com.etema.attributemodify.editor;

import com.etema.attributemodify.integration.CuriosIntegration;
import com.etema.attributemodify.integration.AccessoriesIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EditorCatalogService {
    private EditorCatalogService() {
    }

    public static EditorCatalog buildCatalog() {
        List<EditorItemInfo> items = ForgeRegistries.ITEMS.getEntries().stream()
                .map(entry -> toItemInfo(entry.getKey().location(), entry.getValue()))
                .sorted(Comparator.comparing(info -> info.id().toString()))
                .toList();

        List<EditorAttributeInfo> attributes = ForgeRegistries.ATTRIBUTES.getEntries().stream()
                .map(entry -> toAttributeInfo(entry.getKey().location(), entry.getValue()))
                .sorted(Comparator.comparing(info -> info.id().toString()))
                .toList();

        Set<String> equipmentSlots = new LinkedHashSet<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipmentSlots.add(slot.getName());
        }

        Set<String> namespaces = new LinkedHashSet<>();
        for (EditorItemInfo item : items) {
            namespaces.add(item.namespace());
        }

        Set<ResourceLocation> tags = new LinkedHashSet<>();
        if (ForgeRegistries.ITEMS.tags() != null) {
            ForgeRegistries.ITEMS.tags().getTagNames().map(TagKey::location).forEach(tags::add);
        }

        Set<ResourceLocation> miningTiers = new LinkedHashSet<>();
        for (var tier : TierSortingRegistry.getSortedTiers()) {
            ResourceLocation id = TierSortingRegistry.getName(tier);
            if (id != null) {
                miningTiers.add(id);
            }
        }

        return new EditorCatalog(items, attributes, Set.copyOf(equipmentSlots), Set.of(), Set.of(),
                Set.copyOf(namespaces), Set.copyOf(tags), Set.copyOf(miningTiers), CuriosIntegration.isCuriosLoaded(),
                AccessoriesIntegration.isAccessoriesLoaded());
    }

    private static EditorItemInfo toItemInfo(ResourceLocation id, Item item) {
        ItemStack stack = item.getDefaultInstance();
        int maxDamage = Math.max(0, item.getMaxDamage(stack));
        boolean damageable = stack.isDamageableItem() || maxDamage > 0;
        return new EditorItemInfo(id, item.getDescriptionId(), maxDamage, damageable, id.getNamespace());
    }

    private static EditorAttributeInfo toAttributeInfo(ResourceLocation id, Attribute attribute) {
        return new EditorAttributeInfo(id, attribute.getDescriptionId());
    }

    public record EditorCatalog(
            List<EditorItemInfo> items,
            List<EditorAttributeInfo> attributes,
            Set<String> equipmentSlots,
            Set<String> curiosSlots,
            Set<String> accessoriesSlots,
            Set<String> namespaces,
            Set<ResourceLocation> itemTags,
            Set<ResourceLocation> miningTiers,
            boolean curiosLoaded,
            boolean accessoriesLoaded) {
    }

    public record EditorItemInfo(
            ResourceLocation id,
            String descriptionId,
            int maxDamage,
            boolean damageable,
            String namespace) {
    }

    public record EditorAttributeInfo(ResourceLocation id, String descriptionId) {
    }
}
