package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CSaveResultPacket(String payload) {
    private static final int MAX_PAYLOAD_SIZE = 1_048_576;

    public static void encode(S2CSaveResultPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload(), MAX_PAYLOAD_SIZE);
    }

    public static S2CSaveResultPacket decode(FriendlyByteBuf buf) {
        return new S2CSaveResultPacket(buf.readUtf(MAX_PAYLOAD_SIZE));
    }

    public static void handle(S2CSaveResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> EditorClientState.setLatestSaveResultJson(packet.payload()));
        context.setPacketHandled(true);
    }
}
