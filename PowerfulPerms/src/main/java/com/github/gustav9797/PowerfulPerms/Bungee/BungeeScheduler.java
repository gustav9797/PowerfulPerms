package com.github.gustav9797.PowerfulPerms.Bungee;

import com.github.gustav9797.PowerfulPerms.common.SchedulerBase;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

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

    // Bungee can't run sync tasks, only async tasks
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

    @Override
    public int runRepeating(Runnable runnable, int seconds) {
        ScheduledTask task = ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, 0, seconds, TimeUnit.SECONDS);
        super.runRepeating(runnable, seconds);
        return task.getId();
    }

    @Override
    public void stopRepeating(int taskId) {
        ProxyServer.getInstance().getScheduler().cancel(taskId);
        super.stopRepeating(taskId);
    }

    @Override
    public int runDelayed(Runnable runnable, Date when) {
        super.runDelayed(runnable, when);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(when.getTime() - (new Date()).getTime());
        return runDelayed(runnable, (seconds < 0 ? 0 : seconds));
    }

    @Override
    public int runDelayed(Runnable runnable, long seconds) {
        super.runDelayed(runnable, seconds);
        return ProxyServer.getInstance().getScheduler().schedule(plugin, runnable, seconds, TimeUnit.SECONDS).getId();
    }

}
