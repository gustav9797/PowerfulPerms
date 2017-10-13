package com.github.gustav9797.PowerfulPermsAPI;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public interface PowerfulPermsPlugin {

    public PermissionManager getPermissionManager();

    public Logger getLogger();

    public void runTaskAsynchronously(Runnable runnable);

    public void runTaskLater(Runnable runnable, int delay);

    public boolean isDebug();

    public ServerMode getServerMode();

    public boolean isPlayerOnline(UUID uuid);

    public boolean isPlayerOnline(String name);

    public UUID getPlayerUUID(String name);

    public String getPlayerName(UUID uuid);

    public Map<UUID, String> getOnlinePlayers();

    public void sendPlayerMessage(String name, String message);

    public void debug(String message);

    public int getOldVersion();

    public String getVersion();

    public void loadConfig();
    
    public boolean isBungeeCord();
}
