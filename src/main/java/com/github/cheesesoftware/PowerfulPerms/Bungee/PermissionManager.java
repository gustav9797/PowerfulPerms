package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import org.apache.commons.pool2.impl.*;

import com.github.cheesesoftware.PowerfulPerms.CachedPlayer;
import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;
import com.github.cheesesoftware.PowerfulPerms.SQL;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PermissionManager implements Listener {
    private HashMap<UUID, PermissionsPlayer> players = new HashMap<UUID, PermissionsPlayer>();
    private ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();

    private HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    private SQL sql;
    private Plugin plugin;

    private JedisPool pool;
    private JedisPubSub subscriber;

    private String thisName = "";

    public PermissionManager(SQL sql, Plugin plugin) {
        this.sql = sql;
        this.plugin = plugin;
        this.thisName = "bungeeproxy" + (new Random()).nextInt(5000) + (new Date()).getTime();

        // Initialize Redis
        if (PowerfulPerms.redis_password == null || PowerfulPerms.redis_password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0, PowerfulPerms.redis_password);
        final Plugin tempPlugin = plugin;
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    subscriber = (new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, final String msg) {
                            tempPlugin.getProxy().getScheduler().runAsync(tempPlugin, new Runnable() {
                                public void run() {
                                    // Reload player or groups depending on msg
                                    String[] split = msg.split(" ");
                                    if (split.length == 2) {
                                        String first = split[0];

                                        String server = split[1];

                                        if (server.equals(thisName))
                                            return;

                                        if (first.equals("[groups]")) {
                                            loadGroups();
                                            tempPlugin.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded all groups.");
                                        } else if (first.equals("[players]")) {
                                            loadGroups();
                                            tempPlugin.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded all players. ");
                                        } else {
                                            ProxiedPlayer player = tempPlugin.getProxy().getPlayer(first);
                                            if (player != null) {
                                                loadPlayer(player);
                                                tempPlugin.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded player \"" + first + "\".");
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
                    jedis.subscribe(subscriber, "PowerfulPerms");
                } catch (Exception e) {
                    pool.returnBrokenResource(jedis);
                    tempPlugin.getLogger().warning(
                            PowerfulPerms.pluginPrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                    return;
                }
                pool.returnResource(jedis);
            }
        });

        try {
            loadGroups();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        debug("PlayerQuitEvent " + e.getPlayer().getName());
        if (players.containsKey(e.getPlayer().getUniqueId()))
            players.remove(e.getPlayer().getUniqueId());
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId()))
            cachedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(final LoginEvent e) {
        debug("LoginEvent " + e.getConnection().getName());

        if (!e.isCancelled()) {
            e.registerIntent(plugin);
            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {

                @Override
                public void run() {
                    loadPlayer(e.getConnection().getUniqueId(), e.getConnection().getName(), true);
                    debug("LoginEvent uuid " + e.getConnection().getUniqueId().toString());
                    e.completeIntent(plugin);
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPostLogin(final PostLoginEvent e) {
        debug("PostLoginEvent " + e.getPlayer().getName() + " uuid " + e.getPlayer().getUniqueId().toString());
        // Check again if
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId())) {
            // Player is cached. Continue load it.
            continueLoadPlayer(e.getPlayer());
        } else
            debug("onPlayerJoin player isn't cached");
    }

    private void debug(String msg) {
        if (PowerfulPerms.debug) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

    public void onDisable() {
        if (subscriber != null)
            subscriber.unsubscribe();
        if (pool != null)
            pool.destroy();
        groups.clear();
    }

    public void reloadGroups() {
        groups.clear();
        loadGroups();
    }

    private void loadPlayer(ProxiedPlayer player) {
        loadPlayer(player.getUniqueId(), player.getName(), false);
    }

    @SuppressWarnings("resource")
    private void loadPlayer(UUID uuid, String name, boolean login) {
        /**
         * Loads player data from MySQL, removes old data
         */
        try {
            String groups_loaded = "";
            String prefix_loaded = "";
            String suffix_loaded = ": ";

            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `uuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next()) {
                // The player exists in database.
                groups_loaded = result.getString("groups");
                prefix_loaded = result.getString("prefix");
                suffix_loaded = result.getString("suffix");

                // Check if name mismatch, update player name
                String playerName_loaded = result.getString("name");
                String playerName = name;
                debug("playername_loaded " + playerName_loaded);
                debug("playerName " + playerName);
                if (!playerName_loaded.equals(playerName)) {
                    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `name`=? WHERE `uuid`=?;");
                    s.setString(1, name);
                    s.setString(2, uuid.toString());
                    s.execute();
                    debug("PLAYER NAME MISMATCH");
                }
            } else {
                // The player might exist in database but has no UUID yet.
                s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
                s.setString(1, name);
                s.execute();
                result = s.getResultSet();
                if (result.next()) {
                    // Player exists in database but has no UUID. Lets enter it.
                    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `uuid`=? WHERE `name`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.execute();
                    // UUID has been entered into player. Lets continue.
                    groups_loaded = result.getString("groups");
                    prefix_loaded = result.getString("prefix");
                    suffix_loaded = result.getString("suffix");
                    debug("ENTERED NEW UUID. NAMECHANGE");

                    s.close();
                } else {
                    // Player does not exist in database. Create a new player.
                    s.close();

                    result = getPlayerData("[default]");
                    groups_loaded = result.getString("groups");
                    prefix_loaded = result.getString("prefix");
                    suffix_loaded = result.getString("suffix");

                    s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.setString(3, groups_loaded);
                    s.setString(4, prefix_loaded);
                    s.setString(5, suffix_loaded);
                    s.execute();
                    s.close();
                    debug("NEW PLAYER CREATED");
                }
            }
            s.close();

            /*
             * debug("begin sleep");
             * 
             * try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
             * 
             * debug("end sleep");
             */

            ArrayList<PowerfulPermission> perms = loadPlayerPermissions(uuid);

            if (login) {
                /*
                 * if (cachedPlayers.containsKey(uuid)) { final Player player = Bukkit.getServer().getPlayer(uuid); if (player != null) { cachedPlayers.put(uuid, new CachedPlayer(groups_loaded,
                 * prefix_loaded, suffix_loaded, perms)); Bukkit.getLogger().warning(PowerfulPerms.consolePrefix +
                 * "Your MySQL connection is running slow. Permission checks in player join event may not work as expected."); Bukkit.getScheduler().runTask(plugin, new Runnable() { public void run()
                 * { continueLoadPlayer(player); } }); } return; }
                 */

                debug("Inserted into cachedPlayers allowing playerjoin to finish");
                debug("perms size " + perms.size());
                cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));

            } else {
                ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
                if (player != null) {
                    cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));
                    continueLoadPlayer(player);
                }

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void continueLoadPlayer(ProxiedPlayer p) {
        debug("continueLoadPlayer " + p.getName());
        CachedPlayer cachedPlayer = cachedPlayers.get(p.getUniqueId());
        if (cachedPlayer == null) {
            plugin.getLogger().severe(PowerfulPerms.consolePrefix + "Could not continue load player. Cached player is null.");
            return;
        }

        if (players.containsKey(p.getUniqueId())) {
            players.remove(p.getUniqueId());
        }

        // Load player groups.
        HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(cachedPlayer.getGroups());
        HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
        for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
            ArrayList<Group> groupList = new ArrayList<Group>();
            for (Integer groupId : entry.getValue())
                groupList.add(groups.get(groupId));
            playerGroups.put(entry.getKey(), groupList);
        }

        PermissionsPlayer permissionsPlayer = new PermissionsPlayer(p, playerGroups, cachedPlayer.getPermissions(), cachedPlayer.getPrefix(), cachedPlayer.getSuffix());
        players.put(p.getUniqueId(), permissionsPlayer);
        cachedPlayers.remove(p.getUniqueId());
    }

    private void loadGroups() {
        /**
         * Loads groups from MySQL, removes old group data. Will reload all players too.
         */
        HashMap<Integer, String> tempParents = new HashMap<Integer, String>();
        try {
            groups.clear();
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblGroups);
            s.execute();
            ResultSet result = s.getResultSet();
            while (result.next()) {
                int groupId = result.getInt("id");
                String name = result.getString("name");
                String parents = result.getString("parents");
                String prefix = result.getString("prefix");
                String suffix = result.getString("suffix");

                tempParents.put(groupId, parents);
                Group group = new Group(groupId, name, loadGroupPermissions(name), prefix, suffix);
                groups.put(groupId, group);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Iterator<Entry<Integer, String>> it = tempParents.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, String> e = it.next();
            // Bukkit.getLogger().info("Adding parents to " +
            // groups.get(e.getKey()).getName());
            ArrayList<Group> finalGroups = new ArrayList<Group>();
            ArrayList<String> rawParents = getGroupParents(e.getValue());
            for (String s : rawParents) {
                for (Group testGroup : groups.values()) {
                    // Bukkit.getLogger().info("Comparing " + s + " with " +
                    // testGroup.getId());
                    if (!s.isEmpty() && Integer.parseInt(s) == testGroup.getId()) {
                        finalGroups.add(testGroup);
                        // Bukkit.getLogger().info("Added parent " +
                        // testGroup.getName() + " to " +
                        // groups.get(e.getKey()).getName());
                        break;
                    }
                }
            }
            groups.get(e.getKey()).setParents(finalGroups);
        }
    }

    private ArrayList<PowerfulPermission> loadPlayerPermissions(UUID uuid) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playeruuid`=?");
            s.setString(1, uuid.toString());
            debug("PlayerUUID is \"" + uuid.toString() + "\"");
            s.execute();
            ResultSet result = s.getResultSet();
            ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
            while (result.next()) {
                PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
                perms.add(tempPerm);
                debug("permission added loadplayerpermissions " + tempPerm.getPermissionString());
            }

            return perms;
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load player permissions.");
        }
        return null;
    }

    private ArrayList<PowerfulPermission> loadPlayerPermissions(String name) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playername`=?");
            s.setString(1, name);
            s.execute();
            ResultSet result = s.getResultSet();
            ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
            while (result.next()) {
                PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
                perms.add(tempPerm);
            }
            return perms;
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load player permissions.");
        }
        return null;
    }

    private ArrayList<PowerfulPermission> loadGroupPermissions(String groupName) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `groupname`=?");
            s.setString(1, groupName);
            s.execute();
            ResultSet result = s.getResultSet();
            ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
            while (result.next()) {
                PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
                perms.add(tempPerm);
            }
            return perms;
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load group permissions.");
        }
        return null;
    }

    private ArrayList<String> getGroupParents(String parentsString) {
        ArrayList<String> parents = new ArrayList<String>();
        if (parentsString.contains(";")) {
            for (String s : parentsString.split(";")) {
                parents.add(s);
            }
        } else
            parents.add(parentsString);
        return parents;
    }

    private HashMap<String, List<Integer>> getPlayerGroupsRaw(String groupsString) {
        HashMap<String, List<Integer>> groups = new HashMap<String, List<Integer>>();
        if (groupsString.contains(";")) {
            for (String s : groupsString.split(";")) {
                String[] split = s.split(":");
                if (split.length >= 2) {
                    List<Integer> input = groups.get(split[0]);
                    if (input == null)
                        input = new ArrayList<Integer>();
                    input.add(Integer.parseInt(split[1]));
                    groups.put(split[0], input);
                } else {
                    List<Integer> input = groups.get("");
                    if (input == null)
                        input = new ArrayList<Integer>();
                    input.add(Integer.parseInt(s));
                    groups.put("", input);
                }
            }
        } else if (!groupsString.isEmpty()) {
            ArrayList<Integer> tempList = new ArrayList<Integer>();
            tempList.add(Integer.parseInt(groupsString));
            groups.put("", tempList);
        }
        return groups;
    }

    public ResultSet getPlayerData(String playerName) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet rs = s.getResultSet();
            if (rs.next())
                return rs;
            else {
                s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?");
                s.setString(1, "");
                s.setString(2, playerName);
                s.setString(3, "1");
                s.setString(4, "");
                s.setString(5, "");
                s.execute();

                s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
                s.setString(1, playerName);
                s.execute();
                rs = s.getResultSet();
                if (rs.next())
                    return rs;
                plugin.getLogger().severe(PowerfulPerms.consolePrefix + "Player didn't insert into database properly!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the primary group of an online player.
     */
    public Group getPlayerPrimaryGroup(ProxiedPlayer p) {
        PermissionsPlayer gp = players.get(p.getUniqueId());
        if (gp != null)
            return gp.getPrimaryGroup();
        return null;
    }

    /**
     * Returns the primary group of a player.
     */
    public Group getPlayerPrimaryGroup(String playerName) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null)
            return getPlayerPrimaryGroup(p);
        Iterator<Group> it = getPlayerGroups(playerName).get("").iterator();
        return it.next(); // First group is primary group.
    }

    /**
     * Get the full list of groups a player has, if player isn't online it will be loaded from the database.
     */
    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null) {
            PermissionsPlayer gp = players.get(p.getUniqueId());
            if (gp != null)
                return gp.getServerGroups();
        }
        // Player is not online, load from MySQL
        try {
            ResultSet result = getPlayerData(playerName);
            HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(result.getString("groups"));
            HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
            for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
                ArrayList<Group> groupList = new ArrayList<Group>();
                for (Integer groupId : entry.getValue())
                    groupList.add(groups.get(groupId));
                playerGroups.put(entry.getKey(), groupList);
            }
            return playerGroups;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a map containing all the permissions a player has, including derived permissions. If player is not online data will be loaded from DB and will not return world-specific or server-specific
     * permissions.
     * 
     * @param p
     *            The player to get permissions from.
     */
    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {

        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null) {
            PermissionsPlayer gp = players.get(p.getUniqueId());
            if (gp != null)
                return gp.getAllPermissions();
        } else {
            // Load from DB
            try {
                ArrayList<PowerfulPermission> permissions = loadPlayerPermissions(playerName);

                ResultSet result = getPlayerData(playerName);
                HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(result.getString("groups"));
                HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
                for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
                    ArrayList<Group> groupList = new ArrayList<Group>();
                    for (Integer groupId : entry.getValue())
                        groupList.add(groups.get(groupId));
                    playerGroups.put(entry.getKey(), groupList);
                }

                Group group = playerGroups.get("").iterator().next();
                if (group != null) {
                    permissions.addAll(group.getPermissions());
                    return permissions;
                } else {
                    return permissions;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<PowerfulPermission>();
    }

    public boolean getPlayerHasPermission(ProxiedPlayer player, String permission) {
        PermissionsPlayer permissionsPlayer = players.get(player.getUniqueId());
        if (permissionsPlayer != null) {
            boolean hasPermission = permissionsPlayer.hasPermission(permission);
            debug("Permission check of " + permission + " on player " + player.getName() + " is " + hasPermission);
            return hasPermission;
        }
        return false;
        /*
         * ArrayList<PowerfulPermission> perms = getPlayerPermissions(player.getName()); for (PowerfulPermission toCheck : perms) { if (permission.equalsIgnoreCase(toCheck.getPermissionString())) { if
         * (toCheck.getServer().equalsIgnoreCase("all") || toCheck.getServer().isEmpty() || player.getServer().getInfo().getName().equalsIgnoreCase(toCheck.getServer())) { if
         * (toCheck.getWorld().equalsIgnoreCase(("all")) || toCheck.getWorld().isEmpty()) return true; } } } return false;
         */
    }

}