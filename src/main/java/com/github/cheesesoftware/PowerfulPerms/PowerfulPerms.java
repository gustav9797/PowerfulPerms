package com.github.cheesesoftware.PowerfulPerms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerfulPerms extends JavaPlugin implements Listener {

    private SQL sql;
    private PermissionManager permissionManager;
    private PowerfulPerms_Vault_Permissions vaultPermsHook;
    private PowerfulPerms_Vault_Chat vaultChatHook;
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
	    Bukkit.getLogger().severe(consolePrefix + "You haven't deleted the old config! Do it now and restart the server!");

	try {
	    if (sql.getConnection() == null || sql.getConnection().isClosed()) {
		Bukkit.getLogger().severe(consolePrefix + "Could not access the database!");
		this.setEnabled(false);
	    }
	} catch (SQLException e2) {
	    Bukkit.getLogger().severe(consolePrefix + "Could not access the database!");
	    this.setEnabled(false);
	}

	// Create tables if they do not exist

	// Create table Groups, add group Guest
	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM groups LIMIT 1;").execute();
	} catch (SQLException e) {
	    String groupsTable = "CREATE TABLE `"
		    + tblGroups
		    + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`name` varchar(255) NOT NULL,`parents` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`id`),UNIQUE KEY `id_UNIQUE` (`id`),UNIQUE KEY `name_UNIQUE` (`name`)) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(groupsTable).execute();

		// Insert one group "Guest"
		sql.getConnection().prepareStatement("INSERT INTO `" + tblGroups + "` (`id`, `name`, `parents`, `prefix`, `suffix`) VALUES ('1', 'Guest', '', '[Guest]', ': ');").execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	// Create table Players
	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM players LIMIT 1;").execute();
	} catch (SQLException e) {
	    String playersTable = "CREATE TABLE `"
		    + tblPlayers
		    + "` (`uuid` varchar(36) NOT NULL DEFAULT '',`name` varchar(32) NOT NULL,`groups` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`name`,`uuid`),UNIQUE KEY `uuid_UNIQUE` (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(playersTable).execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	// Insert [default] if not exists
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM players WHERE `name`=?");
	    s.setString(1, "[default]");
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (!result.next()) {
		// Default player doesn't exist. Create it.
		sql.getConnection().prepareStatement("INSERT INTO `" + tblPlayers + "` (`name`, `groups`, `prefix`, `suffix`) VALUES ('[default]', '1', '', '');").execute();
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}

	// Create table Permissions
	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM permissions LIMIT 1;").execute();
	} catch (SQLException e) {
	    String permissionsTable = "CREATE TABLE `"
		    + tblPermissions
		    + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`playeruuid` varchar(36) NOT NULL,`playername` varchar(45) NOT NULL,`groupname` varchar(255) NOT NULL,`permission` varchar(128) NOT NULL,`world` varchar(128) NOT NULL,`server` varchar(128) NOT NULL,PRIMARY KEY (`id`,`playeruuid`,`playername`,`groupname`),UNIQUE KEY `id_UNIQUE` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(permissionsTable).execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	permissionManager = new PermissionManager(this, sql);

	if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
	    Bukkit.getLogger().info(consolePrefix + "Found Vault. Enabling Vault integration.");

	    vaultPermsHook = new PowerfulPerms_Vault_Permissions(permissionManager);
	    vaultChatHook = new PowerfulPerms_Vault_Chat(vaultPermsHook, permissionManager);
	    Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);
	    Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);
	}

	this.getCommand("powerfulperms").setExecutor(new PermissionCommand(permissionManager));
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
