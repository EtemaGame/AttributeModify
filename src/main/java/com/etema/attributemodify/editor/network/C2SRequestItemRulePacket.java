package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorBaseAttributeService;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import com.etema.attributemodify.editor.EditorConflictDetector;
import com.etema.attributemodify.editor.EditorRuleRepository;
import com.etema.attributemodify.editor.model.EditableItemRule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public record C2SRequestItemRulePacket(ResourceLocation targetId, boolean tagTarget) {
    public static void encode(C2SRequestItemRulePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.targetId());
        buf.writeBoolean(packet.tagTarget());
    }

    public static C2SRequestItemRulePacket decode(FriendlyByteBuf buf) {
        return new C2SRequestItemRulePacket(buf.readResourceLocation(), buf.readBoolean());
    }

    public static void handle(C2SRequestItemRulePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            List<EditableItemRule> rules = EditorRuleRepository.loadEditorRules(player.getServer());
            var editorRule = EditorRuleRepository.findRule(rules, packet.targetId(), packet.tagTarget());
            EditableItemRule rule = editorRule.orElseGet(() -> new EditableItemRule(packet.targetId(), packet.tagTarget()));
            boolean externalConflict = !packet.tagTarget() && editorRule.isEmpty()
                    && EditorConflictDetector.hasRuntimeRuleForItem(packet.targetId());
            EditorNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2CItemRulePacket(EditorJsonPayloads.ruleToPayload(
                            rule,
                            externalConflict,
                            EditorBaseAttributeService.collectBaseAttributes(packet.targetId()))));
        });
        context.setPacketHandled(true);
    }
}
