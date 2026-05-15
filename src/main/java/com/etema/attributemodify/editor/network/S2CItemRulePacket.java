package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CItemRulePacket(String payload) {
    private static final int MAX_PAYLOAD_SIZE = 1_048_576;

    public static void encode(S2CItemRulePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload(), MAX_PAYLOAD_SIZE);
    }

    public static S2CItemRulePacket decode(FriendlyByteBuf buf) {
        return new S2CItemRulePacket(buf.readUtf(MAX_PAYLOAD_SIZE));
    }

    public static void handle(S2CItemRulePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> EditorClientState.setLatestRuleJson(packet.payload()));
        context.setPacketHandled(true);
    }
}
