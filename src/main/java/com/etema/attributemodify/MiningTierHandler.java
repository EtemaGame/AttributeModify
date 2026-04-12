package com.etema.attributemodify;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class MiningTierHandler {

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack heldItem = event.getEntity().getMainHandItem();
        if (heldItem.isEmpty()) return;

        List<ItemAttributeDataManager.MiningOverride> overrides =
                ItemAttributeDataManager.getInstance().getMiningOverrides(heldItem.getItem());
        if (overrides.isEmpty()) return;

        for (ItemAttributeDataManager.MiningOverride override : overrides) {
            if (!override.matches(heldItem)) continue;

            if (override.speed() != null) {
                float originalSpeed = event.getOriginalSpeed();
                float baseToolSpeed = getBaseToolSpeed(heldItem);

                if (baseToolSpeed > 1.0f && originalSpeed > 1.0f) {
                    // Preserve enchantment/effect multipliers proportionally
                    float ratio = originalSpeed / baseToolSpeed;
                    event.setNewSpeed(override.speed() * ratio);
                } else {
                    event.setNewSpeed(override.speed());
                }
            }
            return; // First matching override wins
        }
    }

    @SubscribeEvent
    public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        ItemStack heldItem = event.getEntity().getMainHandItem();
        if (heldItem.isEmpty()) return;

        List<ItemAttributeDataManager.MiningOverride> overrides =
                ItemAttributeDataManager.getInstance().getMiningOverrides(heldItem.getItem());
        if (overrides.isEmpty()) return;

        BlockState blockState = event.getTargetBlock();

        for (ItemAttributeDataManager.MiningOverride override : overrides) {
            if (!override.matches(heldItem)) continue;

            if (override.tier() != null) {
                if (TierSortingRegistry.isCorrectTierForDrops(override.tier(), blockState)) {
                    event.setCanHarvest(true);
                }
            }
            return; // First matching override wins
        }
    }

    private float getBaseToolSpeed(ItemStack stack) {
        if (stack.getItem() instanceof TieredItem tiered) {
            return tiered.getTier().getSpeed();
        }
        return 1.0f;
    }

    public static Tier parseTier(String tierName) {
        return switch (tierName.toLowerCase()) {
            case "wood", "wooden" -> Tiers.WOOD;
            case "stone" -> Tiers.STONE;
            case "iron" -> Tiers.IRON;
            case "diamond" -> Tiers.DIAMOND;
            case "netherite" -> Tiers.NETHERITE;
            case "gold", "golden" -> Tiers.GOLD;
            default -> {
                if (AttributeModify.DEBUG_MODE) {
                    AttributeModify.LOGGER.warn("Unknown mining tier '{}', ignoring", tierName);
                }
                yield null;
            }
        };
    }
}
