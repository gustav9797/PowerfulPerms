package com.github.cheesesoftware.PowerfulPerms;

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

}
