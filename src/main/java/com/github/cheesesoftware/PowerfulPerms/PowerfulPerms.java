package com.github.cheesesoftware.PowerfulPerms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerfulPerms extends JavaPlugin implements Listener, IPlugin {

    private SQL sql;
    private PermissionManager permissionManager;
    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String pluginPrefixShort = ChatColor.WHITE + "[" + ChatColor.BLUE + "PP" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static String serverName;
    public static boolean debug = false;

    public void onEnable() {
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));
        PermissionManagerBase.redis_ip = getConfig().getString("redis_ip");
        PermissionManagerBase.redis_port = getConfig().getInt("redis_port");
        PermissionManagerBase.redis_password = getConfig().getString("redis_password");
        debug = getConfig().getBoolean("debug");

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
                    + PermissionManagerBase.tblGroups
                    + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`name` varchar(255) NOT NULL,`parents` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`id`),UNIQUE KEY `id_UNIQUE` (`id`),UNIQUE KEY `name_UNIQUE` (`name`)) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8";
            try {
                sql.getConnection().prepareStatement(groupsTable).execute();

                // Insert one group "Guest"
                sql.getConnection().prepareStatement("INSERT INTO `" + PermissionManagerBase.tblGroups + "` (`id`, `name`, `parents`, `prefix`, `suffix`) VALUES ('1', 'Guest', '', '[Guest]', ': ');").execute();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        // Create table Players
        try {
            sql.getConnection().prepareStatement("SELECT 1 FROM players LIMIT 1;").execute();
        } catch (SQLException e) {
            String playersTable = "CREATE TABLE `"
                    + PermissionManagerBase.tblPlayers
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
                sql.getConnection().prepareStatement("INSERT INTO `" + PermissionManagerBase.tblPlayers + "` (`name`, `groups`, `prefix`, `suffix`) VALUES ('[default]', '1', '', '');").execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Create table Permissions
        try {
            sql.getConnection().prepareStatement("SELECT 1 FROM permissions LIMIT 1;").execute();
        } catch (SQLException e) {
            String permissionsTable = "CREATE TABLE `"
                    + PermissionManagerBase.tblPermissions
                    + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`playeruuid` varchar(36) NOT NULL,`playername` varchar(45) NOT NULL,`groupname` varchar(255) NOT NULL,`permission` varchar(128) NOT NULL,`world` varchar(128) NOT NULL,`server` varchar(128) NOT NULL,PRIMARY KEY (`id`,`playeruuid`,`playername`,`groupname`),UNIQUE KEY `id_UNIQUE` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
            try {
                sql.getConnection().prepareStatement(permissionsTable).execute();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        permissionManager = new PermissionManager(sql, this);
        Bukkit.getPluginManager().registerEvents(permissionManager, this);

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Bukkit.getLogger().info(consolePrefix + "Found Vault. Enabling Vault integration.");

            com.github.cheesesoftware.PowerfulPerms.Vault.PowerfulPerms_Vault_Permissions vaultPermsHook = new com.github.cheesesoftware.PowerfulPerms.Vault.PowerfulPerms_Vault_Permissions(
                    permissionManager);
            com.github.cheesesoftware.PowerfulPerms.Vault.PowerfulPerms_Vault_Chat vaultChatHook = new com.github.cheesesoftware.PowerfulPerms.Vault.PowerfulPerms_Vault_Chat(vaultPermsHook,
                    permissionManager);
            Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPermsHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);
            Bukkit.getServicesManager().register(net.milkbowl.vault.chat.Chat.class, vaultChatHook, Bukkit.getPluginManager().getPlugin("Vault"), ServicePriority.Normal);
        }

        this.getCommand("powerfulperms").setExecutor(new PermissionCommand(permissionManager));

        if (Bukkit.getOnlinePlayers().size() > 0) // Admin used /reload command
            permissionManager.reloadPlayers();
        // permissionManager.notifyLoadServerName();
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

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public void runTaskLater(Runnable runnable, int delay) {
        Bukkit.getScheduler().runTaskLater(this, runnable, delay);
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        Player toReload = Bukkit.getPlayer(uuid);
        if (toReload != null)
            return toReload.isOnline();
        return false;
    }

    @Override
    public UUID getPlayerUUID(String name) {
        Player toReload = Bukkit.getPlayer(name);
        if (toReload != null)
            return toReload.getUniqueId();
        return null;
    }
}
