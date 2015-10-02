package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import com.github.cheesesoftware.PowerfulPerms.SQL;
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

public class PowerfulPerms extends Plugin implements Listener {

    private SQL sql;
    private PermissionManager permissionManager;
    private Configuration config;

    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String pluginPrefixShort = ChatColor.WHITE + "[" + ChatColor.BLUE + "PP" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static boolean debug = false;

    public static String tblPlayers = "players";
    public static String tblGroups = "groups";
    public static String tblPermissions = "permissions";

    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        this.sql = new SQL(config.getString("host"), config.getString("database"), config.getInt("port"), config.getString("username"), config.getString("password"));
        redis_ip = config.getString("redis_ip");
        redis_port = config.getInt("redis_port");
        redis_password = config.getString("redis_password");
        debug = config.getBoolean("debug");

        if (redis_ip == null || redis_password == null)
            getLogger().severe(pluginPrefix + "You haven't deleted the old config! Do it now and restart the server!");

        try {
            if (sql.getConnection() == null || sql.getConnection().isClosed()) {
                getLogger().severe(pluginPrefix + "Could not access the database!");
            }
        } catch (SQLException e2) {
            getLogger().severe(pluginPrefix + "Could not access the database!");
            e2.printStackTrace();
        }

        permissionManager = new PermissionManager(sql, this);
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerListener(this, permissionManager);
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
        if (e.getSender() instanceof ProxiedPlayer) {
            boolean hasPermission = permissionManager.getPlayerHasPermission((ProxiedPlayer) e.getSender(), e.getPermission());
            e.setHasPermission(hasPermission);
        }
    }

    public static PowerfulPerms getPlugin() {
        return (PowerfulPerms) ProxyServer.getInstance().getPluginManager().getPlugin("PowerfulPermsBungee");
    }

    public SQL getSQL() {
        return this.sql;
    }
}
