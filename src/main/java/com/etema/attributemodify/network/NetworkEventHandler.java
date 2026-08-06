package com.etema.attributemodify.network;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = AttributeModify.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NetworkEventHandler {
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            ItemAttributeDataManager dataManager = ItemAttributeDataManager.getInstance();
            var standardData = dataManager.getStandardAttributesForSync();
            var curiosData = dataManager.getCuriosAttributesForSync();
            var durabilityData = dataManager.getDurabilityRulesForSync();
            var miningData = dataManager.getMiningOverridesForSync();
            var decorativeData = dataManager.getDecorativeItemsForSync();
            var preserveDecorativeTooltipData = dataManager.getPreserveDecorativeTooltipItemsForSync();
            var decorativePolicyData = dataManager.getDecorativePoliciesForSync();
            SyncAttributeDataPacket packet = new SyncAttributeDataPacket(standardData, curiosData, durabilityData,
                    miningData, decorativeData, preserveDecorativeTooltipData, decorativePolicyData);

            for (ServerPlayer player : event.getPlayers()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
                AttributeModify.LOGGER.debug("Sent attribute data to player {}: {} standard, {} curios, {} durability",
                        player.getName().getString(), standardData.size(), curiosData.size(), durabilityData.size());
            }
        } catch (Exception e) {
            AttributeModify.LOGGER.error("Failed to synchronize attribute data: {}", e.getMessage(), e);
        }
    }
}
