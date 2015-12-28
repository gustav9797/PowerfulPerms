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
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

}
