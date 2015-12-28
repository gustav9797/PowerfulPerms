package com.github.cheesesoftware.PowerfulPerms;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.github.cheesesoftware.PowerfulPerms.common.IScheduler;

public class BukkitScheduler implements IScheduler {

    private Plugin plugin;

    public BukkitScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable, boolean sameThread) {
        if (sameThread) {
            runnable.run();
            Bukkit.getLogger().info("Running async task on CURRENT thread");
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            Bukkit.getLogger().info("Running async task on NEW thread");
        }
    }

    @Override
    public void runSync(Runnable runnable, boolean sameThread) {
        if (sameThread) {
            runnable.run();
            Bukkit.getLogger().info("Running sync task on CURRENT thread");
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
            Bukkit.getLogger().info("Running async task on MAIN thread");
        }
    }

    @Override
    public void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

}
