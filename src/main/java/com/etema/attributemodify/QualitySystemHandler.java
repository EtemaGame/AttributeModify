package com.etema.attributemodify;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class QualitySystemHandler {

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // Apply directly to the event stack (works for single-click craft)
        applyQuality(event.getCrafting(), "craft");

        // Also schedule a deferred pass for shift-click crafting, where the event
        // stack may be a copy that gets discarded before reaching the inventory.
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Item craftedItem = event.getCrafting().getItem();
            scheduleDeferredScan(serverPlayer, craftedItem, "craft");
        }
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        applyQuality(event.getItem().getItem(), "loot");
    }

    @SubscribeEvent
    public void onVillagerTrade(TradeWithVillagerEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // Do NOT apply quality to event.getMerchantOffer().getResult() directly,
        // as that is the offer template shared across all trades of the same offer.
        // Modifying it would permanently tag the template, preventing re-rolls on future trades.
        // Instead, use the deferred scan to apply quality to the actual inventory copy.
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Item tradedItem = event.getMerchantOffer().getResult().getItem();
            scheduleDeferredScan(serverPlayer, tradedItem, "villager_trade");
        }
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        // Covers loot from chests, barrels, etc. which don't fire EntityItemPickupEvent
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        // Skip if this is the player's own inventory (no container interaction)
        if (event.getContainer() == serverPlayer.inventoryMenu) return;

        final net.minecraft.server.MinecraftServer server = serverPlayer.getServer();
        if (server != null) {
            server.execute(() -> {
                for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack stack = serverPlayer.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        applyQuality(stack, "loot");
                    }
                }
            });
        }
    }

    private void scheduleDeferredScan(ServerPlayer player, Item targetItem, String trigger) {
        final net.minecraft.server.MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == targetItem) {
                        applyQuality(stack, trigger);
                    }
                }
            });
        }
    }

    private void applyQuality(ItemStack itemStack, String trigger) {
        if (itemStack.isEmpty()) return;

        Item item = itemStack.getItem();
        ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
        ItemAttributeDataManager.QualityConfig config = dataManager.getQualityConfig(item);

        if (config == null) return;
        
        // Special trigger: apotheosis_sync
        if ("apotheosis_sync".equals(trigger)) {
            ApotheosisIntegration.syncApotheosisRarity(itemStack);
            return;
        }

        if (!config.triggers().contains(trigger)) return;

        // Skip if the item already has a quality tag (don't re-roll)
        if (hasQualityTag(itemStack, config.pathParts())) return;

        // Roll a random level based on weights
        JsonElement rolledValue = rollQualityLevel(config.levels(), config.totalWeight());
        if (rolledValue == null) return;

        // Write the NBT value
        if (ApotheosisIntegration.isApotheosisLoaded() && isApotheosisRarityPath(config.pathParts())) {
            ApotheosisIntegration.applyApotheosisRarity(itemStack, rolledValue.getAsString());
        } else {
            writeNbtValue(itemStack, config.pathParts(), rolledValue);
        }
    }

    private boolean isApotheosisRarityPath(String[] parts) {
        if (parts.length != 2) return false;
        return "affix_data".equals(parts[0]) && "rarity".equals(parts[1]);
    }

    private boolean hasQualityTag(ItemStack stack, String[] parts) {
        if (!stack.hasTag()) return false;

        CompoundTag tag = stack.getTag();

        for (int i = 0; i < parts.length - 1; i++) {
            if (tag == null || !tag.contains(parts[i])) return false;
            if (tag.getTagType(parts[i]) != 10) return false; // 10 = CompoundTag
            tag = tag.getCompound(parts[i]);
        }

        return tag != null && tag.contains(parts[parts.length - 1]);
    }

    private JsonElement rollQualityLevel(List<ItemAttributeDataManager.QualityLevel> levels, int totalWeight) {
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (ItemAttributeDataManager.QualityLevel level : levels) {
            cumulative += level.weight();
            if (roll < cumulative) {
                return level.value();
            }
        }

        // Fallback (should not happen)
        return levels.get(levels.size() - 1).value();
    }

    private void writeNbtValue(ItemStack stack, String[] parts, JsonElement value) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag current = root;

        // Navigate/create intermediate compound tags
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.contains(parts[i]) || current.getTagType(parts[i]) != 10) {
                current.put(parts[i], new CompoundTag());
            }
            current = current.getCompound(parts[i]);
        }

        String finalKey = parts[parts.length - 1];

        // Write the value with the appropriate NBT type
        if (value.isJsonPrimitive()) {
            JsonPrimitive prim = value.getAsJsonPrimitive();
            if (prim.isNumber()) {
                Number num = prim.getAsNumber();
                double d = num.doubleValue();
                long l = num.longValue();
                if (d == l) {
                    // Fits in int range -> putInt, otherwise putLong
                    if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                        current.putInt(finalKey, (int) l);
                    } else {
                        current.putLong(finalKey, l);
                    }
                } else {
                    current.putDouble(finalKey, d);
                }
            } else if (prim.isBoolean()) {
                current.putBoolean(finalKey, prim.getAsBoolean());
            } else {
                current.putString(finalKey, prim.getAsString());
            }
        } else {
            current.putString(finalKey, value.toString());
        }
    }
}
