package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import com.github.cheesesoftware.PowerfulPerms.common.IScheduler;

public class BungeeScheduler implements IScheduler {

    private Plugin plugin;

    public BungeeScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            runnable.run();
        else
            ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
    }

    @Override
    public void runSync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            runnable.run();
        else
            ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runSync(Runnable runnable) {
        ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0, TimeUnit.MILLISECONDS);
    }

}
