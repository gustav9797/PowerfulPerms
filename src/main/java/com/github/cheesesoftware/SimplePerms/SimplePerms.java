package com.github.cheesesoftware.SimplePerms;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimplePerms extends JavaPlugin implements Listener {

    private SQL sql;
    public static String pluginPrefix = "[SimplePerms] ";

    public void onEnable() {
	this.saveDefaultConfig();
	getServer().getPluginManager().registerEvents(this, this);
	this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));
    }

    public static SimplePerms getPlugin() {
	return (SimplePerms) Bukkit.getPluginManager().getPlugin("SimplePerms");
    }
    
    public SQL getSQL() {
	return this.sql;
    }
}
