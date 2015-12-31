package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;

import com.github.cheesesoftware.PowerfulPerms.common.SchedulerBase;

public class BungeeScheduler extends SchedulerBase {

    private PowerfulPerms plugin;

    public BungeeScheduler(PowerfulPerms plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            runnable.run();
        else
            ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
        super.runAsync(runnable, sameThread);
    }

    @Override
    public void runSync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            runnable.run();
        else
            ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0, TimeUnit.MILLISECONDS);
        super.runSync(runnable, sameThread);
    }

    @Override
    public void runSync(Runnable runnable) {
        runSync(runnable, false);
    }

}
