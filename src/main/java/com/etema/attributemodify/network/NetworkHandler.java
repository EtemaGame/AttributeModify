package com.etema.attributemodify.network;

import com.etema.attributemodify.AttributeModify;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryParse(AttributeModify.MODID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(SyncAttributeDataPacket.class, id++)
                .encoder(SyncAttributeDataPacket::encode)
                .decoder(SyncAttributeDataPacket::decode)
                .consumerMainThread(SyncAttributeDataPacket::handle)
                .add();

        // ServerSetAttributesPacket removed - using datapack-only mode

        // AttributeModify.LOGGER.info("Network packets registered successfully");
    }
}
