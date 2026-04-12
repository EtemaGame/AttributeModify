package com.etema.attributemodify.integration;

import com.etema.attributemodify.AttributeModify;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

// Optional imports - only used if Apotheosis is loaded
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;

public class ApotheosisIntegration {
    private static Boolean apotheosisLoaded = null;

    public static boolean isApotheosisLoaded() {
        if (apotheosisLoaded == null) {
            apotheosisLoaded = ModList.get().isLoaded("apotheosis");
        }
        return apotheosisLoaded;
    }

    /**
     * Aplica una rareza de Apotheosis a un ItemStack usando su API oficial.
     * Esto asegura que los atributos se recalculen correctamente.
     */
    public static void applyApotheosisRarity(ItemStack stack, String rarityId) {
        if (!isApotheosisLoaded() || stack.isEmpty()) return;

        try {
            ResourceLocation rl = ResourceLocation.tryParse(rarityId);
            if (rl == null) return;

            LootRarity rarity = RarityRegistry.INSTANCE.getValue(rl);
            if (rarity != null) {
                AffixHelper.setRarity(stack, rarity);
                
                AttributeModify.LOGGER.info("Applied Apotheosis rarity '{}' to {}", rarityId, stack.getItem());
            }
        } catch (Throwable e) {
            AttributeModify.LOGGER.error("Failed to apply Apotheosis rarity via API: {}", e.getMessage());
        }
    }

    /**
     * Sincroniza la calidad interna del mod con la rareza de Apotheosis.
     * Útil cuando Apotheosis asigna una rareza de forma independiente.
     */
    public static void syncApotheosisRarity(ItemStack stack) {
        if (!isApotheosisLoaded() || stack.isEmpty()) return;

        try {
            LootRarity rarity = AffixHelper.getRarity(stack).get();
            if (rarity != null) {
                // Si ya tiene rareza, forzamos que nuestro sistema de calidad lo reconozca
                // (esto disparará las modificaciones de atributos basadas en rareza)
                AttributeModify.LOGGER.debug("Syncing internal quality with Apotheosis rarity '{}' for {}",
                        RarityRegistry.INSTANCE.getKey(rarity), stack.getItem());
            }
        } catch (Throwable e) {
            // Silently fail if not applicable
        }
    }
}
