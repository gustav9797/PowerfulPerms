package com.github.gustav9797.PowerfulPerms.common;

import java.util.Date;

import com.github.gustav9797.PowerfulPermsAPI.IScheduler;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public abstract class SchedulerBase implements IScheduler {

    private PowerfulPermsPlugin plugin;

    public SchedulerBase(PowerfulPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            plugin.debug("Running async task on CURRENT thread (" + Thread.currentThread() + ")");
        else
            plugin.debug("Running async task on NEW thread");
    }

    @Override
    public void runSync(Runnable runnable, boolean sameThread) {
        if (sameThread)
            plugin.debug("Running sync task on CURRENT thread (" + Thread.currentThread() + ")");
        else
            plugin.debug("Running sync task on MAIN thread");
    }

    @Override
    public int runRepeating(Runnable runnable, int seconds) {
        plugin.debug("Running repeating task every " + seconds + " seconds");
        return -1;
    }

    @Override
    public void stopRepeating(int taskId) {
        plugin.debug("Stopping repeating task with ID " + taskId);
    }

    @Override
    public int runDelayed(Runnable runnable, Date when) {
        plugin.debug("Running delayed task at " + when.toString());
        return -1;
    }

    @Override
    public int runDelayed(Runnable runnable, long seconds) {
        plugin.debug("Running delayed task in " + seconds + " seconds");
        return -1;
    }

}
