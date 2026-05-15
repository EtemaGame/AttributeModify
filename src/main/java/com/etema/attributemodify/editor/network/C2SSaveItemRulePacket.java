package com.etema.attributemodify.editor.network;

import com.etema.attributemodify.editor.EditorDatapackWriter;
import com.etema.attributemodify.editor.EditorJsonPayloads;
import com.etema.attributemodify.editor.EditorPermissionService;
import com.etema.attributemodify.editor.EditorRuleRepository;
import com.etema.attributemodify.editor.EditorRuleValidator;
import com.etema.attributemodify.editor.EditorValidationContext;
import com.etema.attributemodify.editor.model.EditableItemRule;
import com.etema.attributemodify.editor.model.EditableValidationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record C2SSaveItemRulePacket(String payload) {
    private static final int MAX_PAYLOAD_SIZE = 1_048_576;

    public static void encode(C2SSaveItemRulePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload(), MAX_PAYLOAD_SIZE);
    }

    public static C2SSaveItemRulePacket decode(FriendlyByteBuf buf) {
        return new C2SSaveItemRulePacket(buf.readUtf(MAX_PAYLOAD_SIZE));
    }

    public static void handle(C2SSaveItemRulePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleSave(packet, context));
        context.setPacketHandled(true);
    }

    private static void handleSave(C2SSaveItemRulePacket packet, NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.getServer() == null) {
            return;
        }

        if (!EditorPermissionService.canEdit(player)) {
            sendResult(player, false, "You do not have permission to use the AttributeModify editor.");
            return;
        }

        Optional<EditableItemRule> parsedRule = EditorJsonPayloads.ruleFromPayload(packet.payload());
        if (parsedRule.isEmpty()) {
            sendResult(player, false, "Invalid editor rule payload.");
            return;
        }

        EditableItemRule rule = parsedRule.get();
        EditableValidationResult validation = new EditorRuleValidator(EditorValidationContext.live()).validate(rule);
        if (!validation.isValid()) {
            sendResult(player, false, String.join("; ", validation.errors()));
            return;
        }

        List<EditableItemRule> existing = EditorRuleRepository.loadEditorRules(player.getServer());
        List<EditableItemRule> updated = EditorRuleRepository.replaceRule(existing, rule);
        EditorDatapackWriter.SaveResult saveResult = EditorDatapackWriter.write(player.getServer(), updated);
        if (!saveResult.success()) {
            sendResult(player, false, saveResult.error());
            return;
        }

        sendResult(player, true, "Saved editor datapack. Run /reload to apply changes.");
    }

    private static void sendResult(ServerPlayer player, boolean success, String message) {
        EditorNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CSaveResultPacket(EditorJsonPayloads.saveResult(success, message).toString()));
    }
}
