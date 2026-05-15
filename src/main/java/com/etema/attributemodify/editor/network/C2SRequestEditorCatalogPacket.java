package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorCatalogService;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class C2SRequestEditorCatalogPacket {
    public static void encode(C2SRequestEditorCatalogPacket packet, FriendlyByteBuf buf) {
    }

    public static C2SRequestEditorCatalogPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestEditorCatalogPacket();
    }

    public static void handle(C2SRequestEditorCatalogPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String payload = EditorJsonPayloads.catalogToJson(EditorCatalogService.buildCatalog()).toString();
            EditorNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new S2CEditorCatalogPacket(payload));
        });
        context.setPacketHandled(true);
    }
}
