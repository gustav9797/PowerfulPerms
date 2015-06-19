package com.github.cheesesoftware.PowerfulPerms;

import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerfulPerms extends JavaPlugin implements Listener {

    private SQL sql;
    @SuppressWarnings("unused")
    private PermissionManager permissionManager;
    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "SimplePerms" + ChatColor.WHITE + "] ";

    public static String defaultGroup = "";

    public void onEnable() {
	this.saveDefaultConfig();
	getServer().getPluginManager().registerEvents(this, this);
	this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));
	defaultGroup = getConfig().getString("defaultGroup");

	try {
	    if (sql.getConnection() == null || sql.getConnection().isClosed()) {
		Bukkit.getLogger().severe(pluginPrefix + "Could not access the database, disabling..");
		this.setEnabled(false);
	    }
	} catch (SQLException e2) {
	    Bukkit.getLogger().severe(pluginPrefix + "Could not access the database, disabling..");
	    this.setEnabled(false);
	}

	permissionManager = new PermissionManager(this, sql);
    }

    public static PowerfulPerms getPlugin() {
	return (PowerfulPerms) Bukkit.getPluginManager().getPlugin("SimplePerms");
    }

    public SQL getSQL() {
	return this.sql;
    }
}
