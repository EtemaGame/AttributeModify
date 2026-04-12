package com.etema.attributemodify.network;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NetworkEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // AttributeModify.LOGGER.info("Player {} logged in, syncing attribute data",
            // serverPlayer.getName().getString());

            final net.minecraft.server.MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                server.execute(() -> {
                    sendDataToPlayer(serverPlayer);
                });
            } else {
                if (AttributeModify.DEBUG_MODE) {
                    AttributeModify.LOGGER.warn("Server was not available for player {} during login sync",
                            serverPlayer.getName().getString());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // AttributeModify.LOGGER.info("Datapack sync event triggered");

        if (event.getPlayer() != null) {
            sendDataToPlayer(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                sendDataToPlayer(player);
            }
        }
    }

    private static void sendDataToPlayer(ServerPlayer player) {
        try {
            ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();

            var standardData = dataManager.getStandardAttributesForSync();
            var curiosData = dataManager.getCuriosAttributesForSync();
            var durabilityData = dataManager.getDurabilityRulesForSync();
            var miningData = dataManager.getMiningOverridesForSync();
            var decorativeData = dataManager.getDecorativeItemsForSync();

            if (standardData == null) {
                standardData = java.util.Map.of();
            }
            if (curiosData == null) {
                curiosData = java.util.Map.of();
            }
            if (durabilityData == null) {
                durabilityData = java.util.Map.of();
            }
            if (miningData == null) {
                miningData = java.util.Map.of();
            }
            if (decorativeData == null) {
                decorativeData = java.util.Set.of();
            }

            if (!standardData.isEmpty() || !curiosData.isEmpty() || !durabilityData.isEmpty() || !miningData.isEmpty()
                    || !decorativeData.isEmpty()) {
                SyncAttributeDataPacket packet = new SyncAttributeDataPacket(standardData, curiosData, durabilityData,
                        miningData, decorativeData);
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);

                AttributeModify.LOGGER.debug("Sent attribute data to player {}: {} standard, {} curios, {} durability",
                        player.getName().getString(), standardData.size(), curiosData.size(), durabilityData.size());
            }

        } catch (Exception e) {
            if (AttributeModify.DEBUG_MODE) {
                AttributeModify.LOGGER.error("Failed to send attribute data to player {}: {}",
                        player.getName().getString(), e.getMessage(), e);
            }
        }
    }
}
