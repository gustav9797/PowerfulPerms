package com.github.gustav9797.PowerfulPerms.Bungee;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import com.github.gustav9797.PowerfulPerms.database.Database;
import com.github.gustav9797.PowerfulPerms.database.DatabaseCredentials;
import com.github.gustav9797.PowerfulPerms.database.MySQLDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPerms.common.Versioner;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.ServerMode;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PowerfulPerms extends Plugin implements Listener, PowerfulPermsPlugin {

    private PowerfulPermissionManager permissionManager;
    private Configuration config;
    private PermissionCommandExecutor commandExecutor;

    private File customConfigFile = null;
    private Configuration customConfig = null;

    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static boolean debug = false;
    public static ServerMode serverMode = ServerMode.ONLINE;
    public static int oldVersion = 0;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        saveDefaultCustomConfig();
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        int currentVersion = Versioner.getVersionNumber(this.getDescription().getVersion());
        oldVersion = getCustomConfig().getInt("oldversion", 0);
        if (oldVersion <= 0)
            oldVersion = currentVersion;

        PermissionManagerBase.redisEnabled = config.getBoolean("redis", true);
        PermissionManagerBase.redis_ip = config.getString("redis_ip");
        PermissionManagerBase.redis_port = config.getInt("redis_port");
        PermissionManagerBase.redis_password = config.getString("redis_password");

        if (config.getBoolean("onlinemode", false) == true)
            serverMode = ServerMode.ONLINE;
        else if (config.getBoolean("onlinemode", true) == false)
            serverMode = ServerMode.OFFLINE;
        else if (config.getString("onlinemode", "default").equalsIgnoreCase("mixed"))
            serverMode = ServerMode.MIXED;
        getLogger().info("PowerfulPerms is now running on server mode " + serverMode);

        loadConfig();

        DatabaseCredentials cred = new DatabaseCredentials(config.getString("host"), config.getString("database"), config.getInt("port"), config.getString("username"), config.getString("password"));
        Database db = new MySQLDatabase(new BungeeScheduler(this), cred, this, config.getString("prefix"));
        permissionManager = new PowerfulPermissionManager(db, this, "BungeeCord");
        permissionManager.loadGroups(true);
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerListener(this, permissionManager);

        this.commandExecutor = new PermissionCommandExecutor(permissionManager);
        getProxy().getPluginManager().registerCommand(this, commandExecutor);

        if (getCustomConfig().getInt("oldversion", -1) == -1 || oldVersion != currentVersion) {
            getCustomConfig().set("oldversion", currentVersion);
            saveCustomConfig();
        }
    }

    @Override
    public void onDisable() {
        if (permissionManager != null)
            permissionManager.onDisable();
    }

    @EventHandler
    public void onTab(TabCompleteEvent e) {
        if (e.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            String[] args = e.getCursor().split(" ");
            if (args != null && args.length > 0 && args[0].startsWith("/"))
                args[0] = args[0].substring(1);
            boolean isPowerfulPerms = false;
            for (String alias : this.commandExecutor.getAliases()) {
                if (alias.equalsIgnoreCase(args[0])) {
                    isPowerfulPerms = true;
                    break;
                }
            }
            if (isPowerfulPerms || this.commandExecutor.getName().equalsIgnoreCase(args[0])) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                if (newArgs.length <= 0 || e.getCursor().endsWith(" ")) {
                    String[] newArgsTemp = new String[newArgs.length + 1];
                    System.arraycopy(args, 0, newArgsTemp, 0, newArgs.length);
                    newArgsTemp[newArgs.length] = " ";
                    newArgs = newArgsTemp;
                }
                List<String> out = this.commandExecutor.onTabComplete(player, newArgs);
                if (out != null)
                    e.getSuggestions().addAll(out);
            }
        }

    }

    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "data.yml");
        }
        try {
            customConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getCustomConfig() {
        if (customConfig == null) {
            reloadCustomConfig();
        }
        return customConfig;
    }

    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(getCustomConfig(), customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "data.yml");
        }
        if (!customConfigFile.exists()) {
            try (InputStream in = getResourceAsStream("data.yml")) {
                Files.copy(in, customConfigFile.toPath());
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
                is.close();
                os.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to create configuration file", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPermissionCheck(PermissionCheckEvent e) {
        if (e != null && e.getSender() != null && e.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            PermissionPlayer gp = permissionManager.getPermissionPlayer(player.getUniqueId());
            if (gp != null) {
                Boolean hasPermission = gp.hasPermission(e.getPermission());
                if (hasPermission == null)
                    hasPermission = false;
                e.setHasPermission(hasPermission);
            } else
                debug("PermissionsPlayer is null " + player.getUniqueId() + " " + e.getSender().toString());
        }
    }

    public static PowerfulPerms getPlugin() {
        return (PowerfulPerms) ProxyServer.getInstance().getPluginManager().getPlugin("PowerfulPerms");
    }

    @Override
    public PermissionManager getPermissionManager() {
        return this.permissionManager;
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
    public ServerMode getServerMode() {
        return serverMode;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        ProxiedPlayer player = this.getProxy().getPlayer(uuid);
        if (player != null)
            return true;
        return false;
    }

    @Override
    public boolean isPlayerOnline(String name) {
        ProxiedPlayer player = this.getProxy().getPlayer(name);
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
    public String getPlayerName(UUID uuid) {
        ProxiedPlayer player = this.getProxy().getPlayer(uuid);
        if (player != null)
            return player.getName();
        return null;
    }

    @Override
    public Map<UUID, String> getOnlinePlayers() {
        HashMap<UUID, String> players = new HashMap<>();
        for (ProxiedPlayer player : getProxy().getPlayers())
            players.put(player.getUniqueId(), player.getName());
        return players;
    }

    @Override
    public void sendPlayerMessage(String name, String message) {
        CommandSender commandSender = null;
        if (name.equalsIgnoreCase("console"))
            commandSender = ProxyServer.getInstance().getConsole();
        else
            commandSender = ProxyServer.getInstance().getPlayer(name);

        if (commandSender != null)
            commandSender.sendMessage(TextComponent.fromLegacyText(PermissionManagerBase.pluginPrefixShort + message));
    }

    @Override
    public void debug(String message) {
        if (debug)
            getLogger().info("[DEBUG] " + message);
    }

    @Override
    public int getOldVersion() {
        return oldVersion;
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }

    @Override
    public void loadConfig() {
        this.saveDefaultConfig();
        saveDefaultCustomConfig();
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        debug = config.getBoolean("debug");
    }
    
    @Override
    public boolean isBungeeCord() {
        return true;
    }
}
