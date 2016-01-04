package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.cheesesoftware.PowerfulPerms.common.IPermissionsPlayer;
import com.github.cheesesoftware.PowerfulPerms.common.IPlugin;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPerms.database.MySQLDatabase;
import com.github.cheesesoftware.PowerfulPerms.database.SQL;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PowerfulPerms extends Plugin implements Listener, IPlugin {

    private SQL sql;
    private PermissionManager permissionManager;
    private Configuration config;

    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static boolean bungee_command = false;
    public static boolean debug = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        this.sql = new SQL(config.getString("host"), config.getString("database"), config.getInt("port"), config.getString("username"), config.getString("password"));

        PermissionManagerBase.redis = config.getBoolean("redis", true);
        PermissionManagerBase.redis_ip = config.getString("redis_ip");
        PermissionManagerBase.redis_port = config.getInt("redis_port");
        PermissionManagerBase.redis_password = config.getString("redis_password");

        bungee_command = config.getBoolean("bungee_command");
        debug = config.getBoolean("debug");

        try {
            if (sql.getConnection() == null || sql.getConnection().isClosed()) {
                getLogger().severe("Could not access the database!");
            }
        } catch (SQLException e2) {
            getLogger().severe("Could not access the database!");
            e2.printStackTrace();
        }

        Database db = new MySQLDatabase(new BungeeScheduler(this), sql);
        String tablePrefix = config.getString("prefix");
        if (tablePrefix != null && !tablePrefix.isEmpty())
            db.setTablePrefix(tablePrefix);
        String serverName = "bungeeproxy" + (new Random()).nextInt(5000) + (new Date()).getTime();
        permissionManager = new PermissionManager(db, this, serverName);
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerListener(this, permissionManager);

        if (bungee_command) {
            getLogger().info("Using Bungee sided command.");
            getProxy().getPluginManager().registerCommand(this, new PermissionCommandExecutor(permissionManager));
        }
    }

    @Override
    public void onDisable() {
        permissionManager.onDisable();
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                InputStream is = getResourceAsStream("config.yml");
                OutputStream os = new FileOutputStream(configFile);
                ByteStreams.copy(is, os);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create configuration file", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPermissionCheck(PermissionCheckEvent e) {
        if (e != null && e.getSender() != null && e.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            IPermissionsPlayer gp = permissionManager.getPermissionsPlayer(player.getUniqueId());
            if (gp != null) {
                Boolean hasPermission = gp.hasPermission(e.getPermission());
                if (hasPermission == null)
                    hasPermission = false;
                e.setHasPermission(hasPermission);
            } else
                debug("PermissionsPlayer is null");
        }
    }

    public static PowerfulPerms getPlugin() {
        return (PowerfulPerms) ProxyServer.getInstance().getPluginManager().getPlugin("PowerfulPerms");
    }

    public SQL getSQL() {
        return this.sql;
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        this.getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public void runTaskLater(Runnable runnable, int delay) {
        this.getProxy().getScheduler().schedule(this, runnable, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        ProxiedPlayer player = this.getProxy().getPlayer(uuid);
        if (player != null)
            return true;
        return false;
    }

    @Override
    public UUID getPlayerUUID(String name) {
        ProxiedPlayer player = this.getProxy().getPlayer(name);
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
