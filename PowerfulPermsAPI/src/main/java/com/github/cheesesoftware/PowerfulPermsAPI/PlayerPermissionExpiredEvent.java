package com.github.cheesesoftware.PowerfulPermsAPI;

import java.util.UUID;

public class PlayerPermissionExpiredEvent extends Event {
    private final UUID playerUUID;
    private final Permission permission;

    public PlayerPermissionExpiredEvent(UUID playerUUID, Permission permission) {
        this.playerUUID = playerUUID;
        this.permission = permission;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    public Permission getPermission() {
        return this.permission;
    }
}
