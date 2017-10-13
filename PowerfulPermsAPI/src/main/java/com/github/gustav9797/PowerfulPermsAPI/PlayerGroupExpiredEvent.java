package com.github.gustav9797.PowerfulPermsAPI;

import java.util.UUID;

public class PlayerGroupExpiredEvent extends Event {
    private final UUID playerUUID;
    private final CachedGroup cachedGroup;

    public PlayerGroupExpiredEvent(UUID playerUUID, CachedGroup cachedGroup) {
        this.playerUUID = playerUUID;
        this.cachedGroup = cachedGroup;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    public CachedGroup getCachedGroup() {
        return this.cachedGroup;
    }
}
