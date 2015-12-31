package com.github.cheesesoftware.PowerfulPerms;

import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.cheesesoftware.PowerfulPerms.common.IPlugin;
import com.github.cheesesoftware.PowerfulPerms.common.IScheduler;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPerms.database.MySQLDatabase;
import com.github.cheesesoftware.PowerfulPerms.database.SQL;

public class PowerfulPerms extends JavaPlugin implements Listener, IPlugin {

    private SQL sql;
    private PermissionManager permissionManager;
    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
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

        IScheduler scheduler = new BukkitScheduler(this);
        Database db = new MySQLDatabase(scheduler, sql);
        String tablePrefix = getConfig().getString("prefix");
        if (tablePrefix != null && !tablePrefix.isEmpty())
            db.setTablePrefix(tablePrefix);
        String serverName = getConfig().getString("servername");
        permissionManager = new PermissionManager(db, this, serverName);
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

        this.getCommand("powerfulperms").setExecutor(new PermissionCommandExecutor(permissionManager));

        if (Bukkit.getOnlinePlayers().size() > 0) { // Admin used /reload command
            permissionManager.debug("Reload used. Reloading all online players. " + Bukkit.getOnlinePlayers().size() + " players.");
            permissionManager.reloadPlayers();
        }
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
        Player player = Bukkit.getPlayer(name);
        if (player != null)
            return player.getUniqueId();
        return null;
    }

    @Override
    public void debug(String message) {
        if (debug)
            getLogger().info("[DEBUG] " + message);
    }
}
