package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.AttributeModify;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EditorNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryParse(AttributeModify.MODID + ":editor"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private EditorNetwork() {
    }

    public static void register() {
        int id = 0;

        INSTANCE.messageBuilder(C2SRequestEditorCatalogPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestEditorCatalogPacket::encode)
                .decoder(C2SRequestEditorCatalogPacket::decode)
                .consumerMainThread(C2SRequestEditorCatalogPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CEditorCatalogPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CEditorCatalogPacket::encode)
                .decoder(S2CEditorCatalogPacket::decode)
                .consumerMainThread(S2CEditorCatalogPacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SRequestItemRulePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestItemRulePacket::encode)
                .decoder(C2SRequestItemRulePacket::decode)
                .consumerMainThread(C2SRequestItemRulePacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CItemRulePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CItemRulePacket::encode)
                .decoder(S2CItemRulePacket::decode)
                .consumerMainThread(S2CItemRulePacket::handle)
                .add();

        INSTANCE.messageBuilder(C2SSaveItemRulePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSaveItemRulePacket::encode)
                .decoder(C2SSaveItemRulePacket::decode)
                .consumerMainThread(C2SSaveItemRulePacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CSaveResultPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSaveResultPacket::encode)
                .decoder(S2CSaveResultPacket::decode)
                .consumerMainThread(S2CSaveResultPacket::handle)
                .add();
    }
}
