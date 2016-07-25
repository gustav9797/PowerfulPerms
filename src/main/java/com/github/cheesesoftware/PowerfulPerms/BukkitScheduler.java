package com.github.cheesesoftware.PowerfulPerms;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import com.github.cheesesoftware.PowerfulPerms.common.SchedulerBase;

public class BukkitScheduler extends SchedulerBase {

    private PowerfulPerms plugin;

    public BukkitScheduler(PowerfulPerms plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable, boolean sameThread) {
        if (sameThread) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
        super.runAsync(runnable, sameThread);
    }

    @Override
    public void runSync(Runnable runnable, boolean sameThread) {
        if (sameThread) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
        super.runSync(runnable, sameThread);
    }

    @Override
    public void runSync(Runnable runnable) {
        runSync(runnable, false);
    }

    @Override
    public int runRepeating(Runnable runnable, int seconds) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, 0, seconds * 20);
        return task.getTaskId();
    }

    @Override
    public void stopRepeating(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    public int runDelayed(Runnable runnable, Date when) {
        return runDelayed(runnable, TimeUnit.MILLISECONDS.toSeconds(when.getTime() - (new Date()).getTime()));
    }

    @Override
    public int runDelayed(Runnable runnable, long seconds) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, seconds * 20).getTaskId();
    }

}
