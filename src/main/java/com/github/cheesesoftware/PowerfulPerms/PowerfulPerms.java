package com.github.cheesesoftware.PowerfulPerms;

import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerfulPerms extends JavaPlugin implements Listener {

    private SQL sql;
    private PermissionManager permissionManager;
    private PowerfulPerms_Vault vaultPermsHook;
    private PowerfulPerms_Chat vaultChatHook;
    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";

    public static String tblPlayers = "players";
    public static String tblGroups = "groups";
    public static String tblPermissions = "permissions";

    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public void onEnable() {
	this.saveDefaultConfig();
	getServer().getPluginManager().registerEvents(this, this);
	this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));
	redis_ip = getConfig().getString("redis_ip");
	redis_port = getConfig().getInt("redis_port");
	redis_password = getConfig().getString("redis_password");

	if (redis_ip == null || redis_password == null || getConfig().getString("defaultGroup") != null)
	    Bukkit.getLogger().severe(consolePrefix + "You've forgot to delete the old config! Do it now and restart the server!");

	try {
	    if (sql.getConnection() == null || sql.getConnection().isClosed()) {
		Bukkit.getLogger().severe(consolePrefix + "Could not access the database, disabling..");
		this.setEnabled(false);
	    }
	} catch (SQLException e2) {
	    Bukkit.getLogger().severe(consolePrefix + "Could not access the database, disabling..");
	    this.setEnabled(false);
	}

	permissionManager = new PermissionManager(this, sql);
	if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
	    Bukkit.getLogger().info(consolePrefix + "Found Vault. Enabling Vault integration.");

	    vaultPermsHook = new PowerfulPerms_Vault(permissionManager);
	    vaultChatHook = new PowerfulPerms_Chat(vaultPermsHook, permissionManager);
	    Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);
	    Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);

	}
    }

    public void onDisable() {
	permissionManager.onDisable();
    }

    public static PowerfulPerms getPlugin() {
	return (PowerfulPerms) Bukkit.getPluginManager().getPlugin("PowerfulPerms");
    }

    public PermissionManager getPermissionManager() {
	return this.permissionManager;
    }

    public SQL getSQL() {
	return this.sql;
    }
}
