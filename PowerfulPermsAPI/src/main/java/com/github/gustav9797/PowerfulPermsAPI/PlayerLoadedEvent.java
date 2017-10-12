package com.github.gustav9797.PowerfulPermsAPI;

import java.util.UUID;

public class PlayerLoadedEvent extends Event {
    private final UUID playerUUID;
    private final String playerName;

    public PlayerLoadedEvent(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    public String getPlayerName() {
        return this.playerName;
    }
}
