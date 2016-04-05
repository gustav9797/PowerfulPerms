package com.github.cheesesoftware.PowerfulPerms;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.cheesesoftware.PowerfulPerms.Vault.VaultHook;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.Versioner;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPerms.database.MySQLDatabase;
import com.github.cheesesoftware.PowerfulPerms.database.SQL;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ServerMode;
import com.sk89q.wepif.PermissionsProvider;

public class PowerfulPerms extends JavaPlugin implements Listener, PowerfulPermsPlugin, PermissionsProvider {

    private SQL sql;
    private PowerfulPermissionManager permissionManager;

    private File customConfigFile = null;
    private FileConfiguration customConfig = null;

    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static boolean debug = false;
    public static ServerMode serverMode = ServerMode.ONLINE;
    public static int oldVersion = 0;
    public static boolean useChatFormat;
    public static boolean placeholderAPIEnabled = false;
    public static String chatFormat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultCustomConfig();
        getServer().getPluginManager().registerEvents(this, this);

        int currentVersion = Versioner.getVersionNumber(this.getDescription().getVersion());
        oldVersion = getCustomConfig().getInt("oldversion", 0);
        if (oldVersion <= 0)
            oldVersion = currentVersion;

        this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));

        PermissionManagerBase.redis = getConfig().getBoolean("redis", true);
        PermissionManagerBase.redis_ip = getConfig().getString("redis_ip");
        PermissionManagerBase.redis_port = getConfig().getInt("redis_port");
        PermissionManagerBase.redis_password = getConfig().getString("redis_password");

        debug = getConfig().getBoolean("debug");
        if (getConfig().getBoolean("onlinemode", false) == true)
            serverMode = ServerMode.ONLINE;
        else if (getConfig().getBoolean("onlinemode", true) == false)
            serverMode = ServerMode.OFFLINE;
        else if (getConfig().getString("onlinemode", "default").equalsIgnoreCase("mixed"))
            serverMode = ServerMode.MIXED;
        getLogger().info("PowerfulPerms is now running on server mode " + serverMode);

        useChatFormat = getConfig().getBoolean("use_chatformat", false);
        chatFormat = getConfig().getString("chatformat", "");

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
        permissionManager = new PowerfulPermissionManager(db, this, serverName);
        Bukkit.getPluginManager().registerEvents(permissionManager, this);

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Bukkit.getLogger().info(consolePrefix + "Found Vault. Enabling Vault integration.");
            VaultHook vaultHook = new VaultHook();
            vaultHook.hook(this);
        }

        this.getCommand("powerfulperms").setExecutor(new PermissionCommandExecutor(permissionManager));

        if (Bukkit.getOnlinePlayers().size() > 0) { // Admin used /reload command
            debug("Reload used. Reloaded all online players. " + Bukkit.getOnlinePlayers().size() + " players.");
            permissionManager.reloadPlayers();
        }

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent e) {
        if (e.getPlugin().getName().equals("PlaceholderAPI")) {
            Bukkit.getLogger().info(consolePrefix + "Found PlaceholderAPI. Using custom chat format.");
            placeholderAPIEnabled = true;
        } else if (e.getPlugin().getName().equals("WorldEdit")) {
            com.sk89q.wepif.PermissionsResolverManager.initialize(this);
            Bukkit.getLogger().info(consolePrefix + "Found PlaceholderAPI. Enabling WEPIF hook.");
        }
    }

    public void reloadCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "data.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    public FileConfiguration getCustomConfig() {
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
            getCustomConfig().save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(getDataFolder(), "data.yml");
        }
        if (!customConfigFile.exists()) {
            saveResource("data.yml", false);
        }
    }

    public static PowerfulPerms getPlugin() {
        return (PowerfulPerms) Bukkit.getPluginManager().getPlugin("PowerfulPerms");
    }

    @Override
    public PermissionManager getPermissionManager() {
        return this.permissionManager;
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
    public ServerMode getServerMode() {
        return serverMode;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            return player.isOnline();
        return false;
    }

    @Override
    public boolean isPlayerOnline(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null)
            return player.isOnline();
        return false;
    }

    @Override
    public UUID getPlayerUUID(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null)
            return player.getUniqueId();
        return null;
    }

    @Override
    public String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            return player.getName();
        return null;
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
    public String[] getGroups(String player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            List<Group> groups = p.getGroups(PermissionManagerBase.serverName);
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames.toArray(new String[groupNames.size()]);
        }
        return null;
    }

    @Override
    public String[] getGroups(OfflinePlayer player) {
        return null;
    }

    @Override
    public boolean hasPermission(String player, String permission) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            Boolean has = p.hasPermission(permission);
            if (has != null)
                return has;
        }
        return false;
    }

    @Override
    public boolean hasPermission(OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean hasPermission(String world, String player, String permission) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            Boolean has = p.hasPermission(permission);
            if (has != null)
                return has;
        }
        return false;
    }

    @Override
    public boolean hasPermission(String world, OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean inGroup(String player, String groupName) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            List<Group> groups = p.getGroups();
            for (Group group : groups) {
                if (group.getName().equalsIgnoreCase(groupName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean inGroup(OfflinePlayer player, String groupName) {
        return false;
    }
}
