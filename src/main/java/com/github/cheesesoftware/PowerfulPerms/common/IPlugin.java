package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.UUID;
import java.util.logging.Logger;

public interface IPlugin {
    
    public Logger getLogger();
    
    public void runTaskAsynchronously(Runnable runnable);
    
    public void runTaskLater(Runnable runnable, int delay);
    
    public boolean isDebug();
    
    public boolean isPlayerOnline(UUID uuid);
    
    public UUID getPlayerUUID(String name);
    
    public void debug(String message);
}
