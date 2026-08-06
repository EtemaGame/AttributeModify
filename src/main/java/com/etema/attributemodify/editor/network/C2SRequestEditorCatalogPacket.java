package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorCatalogService;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record C2SRequestEditorCatalogPacket(boolean supportsMetadataCatalog) {
    public C2SRequestEditorCatalogPacket() {
        this(true);
    }

    public static void encode(C2SRequestEditorCatalogPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.supportsMetadataCatalog());
    }

    public static C2SRequestEditorCatalogPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestEditorCatalogPacket(buf.isReadable() && buf.readBoolean());
    }

    public static void handle(C2SRequestEditorCatalogPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String payload = packet.supportsMetadataCatalog()
                    ? EditorJsonPayloads.metadataCatalogToJson(EditorCatalogService.buildMetadataCatalog()).toString()
                    : EditorJsonPayloads.catalogToJson(EditorCatalogService.buildCatalog()).toString();
            if (!S2CEditorCatalogPacket.isPayloadWithinLimit(payload)) {
                payload = EditorJsonPayloads.catalogError("Catalog metadata exceeds the network safety limit").toString();
            }
            EditorNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new S2CEditorCatalogPacket(payload));
        });
        context.setPacketHandled(true);
    }
}
