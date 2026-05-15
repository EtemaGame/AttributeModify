package com.etema.attributemodify.editor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class EditorPermissionService {
    public static final int DEFAULT_REQUIRED_PERMISSION_LEVEL = 2;

    private EditorPermissionService() {
    }

    public static boolean canEdit(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server != null && server.isSingleplayerOwner(player.getGameProfile())) {
            return true;
        }

        return player.hasPermissions(DEFAULT_REQUIRED_PERMISSION_LEVEL);
    }
}
