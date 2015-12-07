package com.github.cheesesoftware.PowerfulPerms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public abstract class PermissionManagerBase implements IPermissionManager {
    protected HashMap<UUID, IPermissionsPlayer> players = new HashMap<UUID, IPermissionsPlayer>();
    protected ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();
    protected HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    protected String serverName;

    protected JedisPool pool;
    protected JedisPubSub subscriber;

    private SQL sql;
    private IPlugin plugin;

    public PermissionManagerBase(SQL sql, IPlugin plugin) {
        this.sql = sql;
        this.plugin = plugin;
    }
    
    protected void debug(String msg) {
        if (plugin.isDebug()) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

    public void notifyReloadGroups() {
        plugin.runTaskAsynchronously(new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                try {
                    Jedis jedis = pool.getResource();
                    try {
                        jedis.publish("PowerfulPerms", "[groups]" + " " + serverName);
                    } catch (Exception e) {
                        pool.returnBrokenResource(jedis);
                    }
                    pool.returnResource(jedis);
                } catch (Exception e) {
                    plugin.getLogger().warning(
                            PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                }
            }
        });
    }

    public void notifyReloadPlayers() {
        plugin.runTaskAsynchronously(new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                try {
                    Jedis jedis = pool.getResource();
                    try {
                        jedis.publish("PowerfulPerms", "[players]" + " " + serverName);
                    } catch (Exception e) {
                        pool.returnBrokenResource(jedis);
                    }
                    pool.returnResource(jedis);

                } catch (Exception e) {
                    plugin.getLogger().warning(
                            PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                }
            }
        });
    }

    public void notifyReloadPlayer(final String playerName) {
        plugin.runTaskAsynchronously(new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                try {
                    Jedis jedis = pool.getResource();
                    try {
                        jedis.publish("PowerfulPerms", playerName + " " + serverName);
                    } catch (Exception e) {
                        pool.returnBrokenResource(jedis);
                    }
                    pool.returnResource(jedis);
                } catch (Exception e) {
                    plugin.getLogger().warning(
                            PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                }
            }
        });
    }
    
    protected Map<String, String> loadPlayerBase(UUID uuid, String name) {
        ResultSet result = null;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `uuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            result = s.getResultSet();

            if (result.next()) {
                // The player exists in database.

                String playerName_loaded = result.getString("name");
                String playerName = name;
                debug("playername_loaded " + playerName_loaded);
                debug("playerName " + playerName);

                // Check if name mismatch, update player name
                if (!playerName_loaded.equals(playerName)) {
                    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `name`=? WHERE `uuid`=?;");
                    s.setString(1, name);
                    s.setString(2, uuid.toString());
                    s.execute();
                    debug("PLAYER NAME MISMATCH");
                }
            } else {
                s.close();
                // The player might exist in database but has no UUID yet.
                s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
                s.setString(1, name);
                s.execute();
                result = s.getResultSet();

                // Make sure player has no UUID in database.
                UUID tempUUID = null;
                if (result.next()) {
                    try {
                        String retrievedUUID = result.getString("uuid");
                        if (retrievedUUID != null && !retrievedUUID.isEmpty())
                            tempUUID = UUID.fromString(retrievedUUID);
                    } catch (IllegalArgumentException e) {
                    }
                }

                if (result.next() && tempUUID == null) {
                    // Player exists in database but has no UUID. Lets enter it.
                    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `uuid`=? WHERE `name`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.execute();

                    debug("ENTERED NEW UUID. NAMECHANGE");

                    s.close();
                } else {
                    // Player does not exist in database. Create a new player.
                    s.close();

                    result = getPlayerData("[default]");

                    s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.setString(3, result.getString("groups"));
                    s.setString(4, result.getString("prefix"));
                    s.setString(5, result.getString("suffix"));
                    s.execute();
                    s.close();

                    debug("NEW PLAYER CREATED");
                }
            }
            s.close();
            
            Map<String, String> output = new HashMap<String, String>();
            output.put("groups", (result != null ? result.getString("groups") : ""));
            output.put("prefix", (result != null ? result.getString("prefix") : ""));
            output.put("suffix", (result != null ? result.getString("suffix") : ": "));
            return output;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
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

    protected void loadGroups() {
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

    protected ArrayList<PowerfulPermission> loadGroupPermissions(String groupName) {
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

    protected ArrayList<String> getGroupParents(String parentsString) {
        ArrayList<String> parents = new ArrayList<String>();
        if (parentsString.contains(";")) {
            for (String s : parentsString.split(";")) {
                parents.add(s);
            }
        } else
            parents.add(parentsString);
        return parents;
    }

    /**
     * Gets a group from its name.
     * 
     * @param groupName
     *            The name of the group to get.
     */
    public Group getGroup(String groupName) {
        for (Map.Entry<Integer, Group> e : groups.entrySet())
            if (e.getValue().getName().equalsIgnoreCase(groupName))
                return e.getValue();
        return null;
    }

    /**
     * Get all groups.
     * 
     * @return All groups.
     */
    public Collection<Group> getGroups() {
        return (Collection<Group>) this.groups.values();
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

    protected HashMap<String, List<Integer>> getPlayerGroupsRaw(String groupsString) {
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

    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
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

    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {
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
        return new ArrayList<PowerfulPermission>();
    }

    protected ArrayList<PowerfulPermission> loadPlayerPermissions(UUID uuid) {
        PreparedStatement s;
        // boolean needsNameUpdate = false;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playeruuid`=?");
            s.setString(1, uuid.toString());
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

    protected ArrayList<PowerfulPermission> loadPlayerPermissions(String name) {
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

    /**
     * Checks if group has permission. Does not work with negated permissions.
     * 
     * @param groupName
     *            Name of the group.
     * @param permission
     *            The permission string. Can check if permission is negated, "-some.permission"
     * @param server
     *            Check server-specific permission. Leave empty if global permission.
     * @param world
     *            Check world-specific permission. Leave empty if global permission.
     * @return
     */
    public boolean groupHasPermission(String groupName, String permission, String server, String world) {
        if (server.equalsIgnoreCase("ALL"))
            server = "";

        if (world.equalsIgnoreCase("ALL"))
            world = "";

        Group group = getGroup(groupName);
        if (group != null) {

            for (PowerfulPermission p : group.getPermissions()) {
                if (p.getPermissionString().equals(permission)) {
                    boolean isSameServer = false;
                    boolean isSameWorld = false;

                    if (p.getServer().isEmpty() || p.getServer().equalsIgnoreCase("ALL") || p.getServer().equals(server))
                        isSameServer = true;

                    if (p.getWorld().isEmpty() || p.getWorld().equalsIgnoreCase("ALL") || p.getWorld().equals(world))
                        isSameWorld = true;
                    if (isSameServer && isSameWorld)
                        return true;
                }
            }
        }
        return false;
    }

}
