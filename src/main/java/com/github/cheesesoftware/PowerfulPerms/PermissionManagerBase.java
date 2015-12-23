package com.github.cheesesoftware.PowerfulPerms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

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

    public static String tblPlayers = "players";
    public static String tblGroups = "groups";
    public static String tblPermissions = "permissions";

    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public static String consolePrefix = "[PowerfulPerms] ";

    public PermissionManagerBase(SQL sql, IPlugin plugin) {
        this.sql = sql;
        this.plugin = plugin;

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
                sql.getConnection().prepareStatement("INSERT INTO `" + PermissionManagerBase.tblGroups + "` (`id`, `name`, `parents`, `prefix`, `suffix`) VALUES ('1', 'Guest', '', '[Guest]', ': ');")
                        .execute();
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

        // Initialize Redis
        if (redis_password == null || redis_password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0, redis_password);
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
                    plugin.getLogger().warning(consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
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
                    plugin.getLogger().warning(consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
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
                    plugin.getLogger().warning(consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                }
            }
        });
    }

    public void reloadPlayers() {
        for (UUID uuid : players.keySet()) {
            if (plugin.isPlayerOnline(uuid)) {
                IPermissionsPlayer permissionsPlayer = players.get(uuid);
                permissionsPlayer.clearPermissions();
                players.remove(uuid);
            }
            loadPlayer(uuid, null, false);
        }
    }

    public void reloadPlayer(String name) {
        UUID uuid = plugin.getPlayerUUID(name);
        if (uuid != null) {
            this.loadPlayer(uuid, name, false);
        }
    }

    public void reloadPlayer(UUID uuid) {
        this.loadPlayer(uuid, null, false);
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(String name) {
        UUID uuid = plugin.getPlayerUUID(name);
        return players.get(uuid);
    }

    protected void loadPlayer(UUID uuid, String name, boolean login) {
        ResultSet result = null;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `uuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            result = s.getResultSet();

            if (result.next()) {
                // The player exists in database.

                String playerName_loaded = result.getString("name");
                debug("playername_loaded " + playerName_loaded);

                if (name != null) {
                    debug("playerName " + name);

                    // Check if name mismatch, update player name
                    if (!playerName_loaded.equals(name)) {
                        s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `name`=? WHERE `uuid`=?;");
                        s.setString(1, name);
                        s.setString(2, uuid.toString());
                        s.execute();
                        debug("PLAYER NAME MISMATCH. PLAYER NAME UPDATED.");
                    }
                }
            } else if (name != null) {
                s.close();
                // The player might exist in database but has no UUID yet.
                s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
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
                    s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `uuid`=? WHERE `name`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.execute();

                    debug("ENTERED NEW UUID. NAMECHANGE");

                    s.close();
                } else {
                    // Player does not exist in database. Create a new player.
                    s.close();

                    result = getPlayerData("[default]");
                    if (result != null) {

                        s = sql.getConnection().prepareStatement("INSERT INTO " + tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
                        s.setString(1, uuid.toString());
                        s.setString(2, name);
                        s.setString(3, result.getString("groups"));
                        s.setString(4, result.getString("prefix"));
                        s.setString(5, result.getString("suffix"));
                        s.execute();
                        s.close();

                        debug("NEW PLAYER CREATED");
                    } else
                        plugin.getLogger().severe(consolePrefix + "Cannot get data from user [default]. Please create the default user.");

                }
            } else
                debug("Could not reload player, 'name' is null");

            String groups_loaded = (result != null ? result.getString("groups") : "");
            String prefix_loaded = (result != null ? result.getString("prefix") : "");
            String suffix_loaded = (result != null ? result.getString("suffix") : ": ");
            s.close();

            ArrayList<PowerfulPermission> perms = loadPlayerPermissions(uuid);

            if (login) {
                debug("Inserted into cachedPlayers allowing playerjoin to finish");
                cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));

            } else {
                // Player should be reloaded if "login" is false. Reload already loaded player.
                if (plugin.isPlayerOnline(uuid) && players.containsKey(uuid)) {
                    IPermissionsPlayer toUpdate = players.get(uuid);
                    PermissionsPlayerBase base = new PermissionsPlayerBase(this.getPlayerGroups(getPlayerGroupsRaw(groups_loaded)), perms, prefix_loaded, suffix_loaded);
                    toUpdate.update(base);

                    if (cachedPlayers.get(uuid) != null)
                        cachedPlayers.remove(uuid);
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected PermissionsPlayerBase loadCachedPlayer(UUID uuid) {
        debug("continueLoadPlayer " + uuid);
        CachedPlayer cachedPlayer = cachedPlayers.get(uuid);
        if (cachedPlayer == null) {
            plugin.getLogger().severe(consolePrefix + "Could not continue load player. Cached player is null.");
            return null;
        }

        if (players.containsKey(uuid)) {
            players.remove(uuid);
        }

        PermissionsPlayerBase base = new PermissionsPlayerBase(this.getPlayerGroups(getPlayerGroupsRaw(cachedPlayer.getGroups())), cachedPlayer.getPermissions(), cachedPlayer.getPrefix(),
                cachedPlayer.getSuffix());

        cachedPlayers.remove(uuid);
        return base;
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
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroups);
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

        // Reload players too.
        Set<UUID> keysCopy = new HashSet<UUID>(players.keySet());
        for (UUID uuid : keysCopy) {
            if (plugin.isPlayerOnline(uuid))
                reloadPlayer(uuid);
        }
    }

    protected ArrayList<PowerfulPermission> loadGroupPermissions(String groupName) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `groupname`=?");
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
            plugin.getLogger().severe(consolePrefix + "Could not load group permissions.");
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
            s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet rs = s.getResultSet();
            if (rs.next())
                return rs;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static HashMap<String, List<Integer>> getPlayerGroupsRaw(String groupsString) {
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

    protected HashMap<String, List<Group>> getPlayerGroups(HashMap<String, List<Integer>> playerGroupsRaw) {
        HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
        for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
            ArrayList<Group> groupList = new ArrayList<Group>();
            for (Integer groupId : entry.getValue())
                groupList.add(groups.get(groupId));
            playerGroups.put(entry.getKey(), groupList);
        }
        return playerGroups;
    }

    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
        try {
            ResultSet result = getPlayerData(playerName);
            if (result != null)
                return getPlayerGroups(getPlayerGroupsRaw(result.getString("groups")));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new HashMap<String, List<Group>>();
    }

    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {
        ArrayList<PowerfulPermission> permissions = loadPlayerPermissions(playerName);
        HashMap<String, List<Group>> playerGroups = getPlayerGroups(playerName);

        if (!playerGroups.isEmpty()) {
            Group group = playerGroups.get("").iterator().next();
            if (group != null) {
                permissions.addAll(group.getPermissions());
            }
        }
        return permissions;
    }

    protected ArrayList<PowerfulPermission> loadPlayerPermissions(UUID uuid) {
        PreparedStatement s;
        // boolean needsNameUpdate = false;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playeruuid`=?");
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
            plugin.getLogger().severe(consolePrefix + "Could not load player permissions.");
        }
        return null;
    }

    protected ArrayList<PowerfulPermission> loadPlayerPermissions(String name) {
        PreparedStatement s;
        try {
            s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playername`=?");
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
            plugin.getLogger().severe(consolePrefix + "Could not load player permissions.");
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

    /**
     * Gets the prefix of a player as stored in database.
     */
    public String getPlayerPrefix(String playerName) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next()) {
                return result.getString("prefix");
            } else
                plugin.getLogger().severe(consolePrefix + "Attempted to get prefix of a player that doesn't exist.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the suffix of a player as stored in database.
     */
    public String getPlayerSuffix(String playerName) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next()) {
                return result.getString("suffix");
            } else
                plugin.getLogger().severe(consolePrefix + "Attempted to get suffix of a player that doesn't exist.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the prefix of a group.
     */
    public String getGroupPrefix(String groupName) {
        Group g = getGroup(groupName);
        if (g != null)
            return g.getPrefix();
        return null;
    }

    /**
     * Gets the suffix of a group.
     */
    public String getGroupSuffix(String groupName) {
        Group g = getGroup(groupName);
        if (g != null)
            return g.getSuffix();
        return null;
    }

    // -------------------------------------------------------------------//
    // //
    // ------------PLAYER PERMISSION MODIFYING FUNCTIONS BELOW------------//
    // //
    // -------------------------------------------------------------------//

    public PMR addPlayerPermission(String playerName, String permission) {
        return addPlayerPermission(playerName, permission, "", "");
    }

    public PMR addPlayerPermission(String playerName, String permission, String world, String server) {
        try {
            if (playerName.equalsIgnoreCase("[default]"))
                return new PMR(false, "You can't add permissions to the default player. Add them to a group instead and add the group to the default player.");

            // Check if the same permission already exists.
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playername`=? AND `permission`=? AND `world`=? AND `server`=?");
            s.setString(1, playerName);
            s.setString(2, permission);
            s.setString(3, world);
            s.setString(4, server);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next()) {
                return new PMR(false, "Player already has the specified permission.");
            }

            UUID uuid = null;
            // Get UUID from table players. Player has to exist.
            s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            result = s.getResultSet();
            if (result.next()) {
                uuid = UUID.fromString(result.getString("uuid"));
            }

            s = sql.getConnection().prepareStatement("INSERT INTO " + tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
            if (uuid != null)
                s.setString(1, uuid.toString());
            else
                return new PMR(false, "Could not add permission. Player doesn't exist.");
            s.setString(2, playerName);
            s.setString(3, "");
            s.setString(4, permission);
            s.setString(5, world);
            s.setString(6, server);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Permission added to player.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR removePlayerPermission(String playerName, String permission) {
        return removePlayerPermission(playerName, permission, "", "");
    }

    public PMR removePlayerPermission(String playerName, String permission, String world, String server) {
        try {
            boolean useWorld = false;
            boolean useServer = false;

            String statement = "DELETE FROM `" + tblPermissions + "` WHERE `playername`=? AND `permission`=?";
            if (!world.isEmpty() && !world.equalsIgnoreCase("ALL")) {
                statement += ", `world`=?";
                useWorld = true;
            }
            if (!server.isEmpty() && !server.equalsIgnoreCase("ALL")) {
                statement += ", `server`=?";
                useServer = true;
            }
            PreparedStatement s = sql.getConnection().prepareStatement(statement);

            s.setString(1, playerName);
            s.setString(2, permission);
            if (useWorld)
                s.setString(3, world);
            if (useServer)
                s.setString(4, server);
            int amount = s.executeUpdate();
            if (amount <= 0)
                return new PMR(false, "Player does not have the specified permission.");

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Removed " + amount + " permissions from the player.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR removePlayerPermissions(String playerName) {
        try {
            String statement = "DELETE FROM `" + tblPermissions + "` WHERE `playername`=?";
            PreparedStatement s = sql.getConnection().prepareStatement(statement);

            s.setString(1, playerName);
            int amount = s.executeUpdate();
            if (amount <= 0)
                return new PMR(false, "Player does not have any permissions.");

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Removed " + amount + " permissions from the player.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR setPlayerPrefix(String playerName, String prefix) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `prefix`=? WHERE `name`=?");
            s.setString(1, prefix);
            s.setString(2, playerName);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Player prefix set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR setPlayerSuffix(String playerName, String suffix) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `suffix`=? WHERE `name`=?");
            s.setString(1, suffix);
            s.setString(2, playerName);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Player suffix set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR setPlayerPrimaryGroup(String playerName, String groupName) {
        Group group = getGroup(groupName);
        if (group == null)
            return new PMR(false, "Group does not exist.");

        try {
            String playerGroupString = "";

            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next())
                playerGroupString = result.getString("groups");
            else
                return new PMR(false, "Player does not exist.");

            // Add group. Put it first.
            HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);
            List<Integer> groupList = playerGroups.get("");
            if (groupList == null)
                groupList = new ArrayList<Integer>();

            // Remove existing primary
            Iterator<Integer> it = groupList.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }

            ArrayList<Integer> newList = new ArrayList<Integer>();
            newList.add(group.getId());
            newList.addAll(groupList);
            playerGroups.put("", newList);

            String playerGroupStringOutput = "";
            for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
                for (Integer groupId : entry.getValue())
                    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
            }

            s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `groups`=? WHERE `name`=?");
            s.setString(1, playerGroupStringOutput);
            s.setString(2, playerName);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Player primary group set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }

    }

    public PMR removePlayerGroup(String playerName, String groupName) {
        return removePlayerGroup(playerName, groupName, "");
    }

    public PMR removePlayerGroup(String playerName, String groupName, String server) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        int groupId_new;
        Group group = getGroup(groupName);
        if (group != null)
            groupId_new = group.getId();
        else
            return new PMR(false, "Group does not exist.");
        try {
            String playerGroupString = "";

            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next())
                playerGroupString = result.getString("groups");
            else
                return new PMR(false, "Player does not exist.");

            boolean removed = false;
            HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);

            List<Integer> groupList = playerGroups.get(server);
            if (groupList != null) {
                Iterator<Integer> it = groupList.iterator();
                while (it.hasNext()) {
                    int groupId = it.next();
                    if (groupId == groupId_new) {
                        if (getPlayerPrimaryGroup(playerName).getId() == groupId && server.isEmpty())
                            return new PMR(false, "Can't remove player primary group.");
                        it.remove();
                        removed = true;
                    }
                }
            }

            if (removed)
                playerGroups.put(server, groupList);
            else
                return new PMR(false, "Player does not have a specific group for the specified server.");

            String playerGroupStringOutput = "";
            for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
                for (Integer groupId : entry.getValue())
                    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
            }

            s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `groups`=? WHERE `name`=?");
            s.setString(1, playerGroupStringOutput);
            s.setString(2, playerName);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Player group removed.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }

    }

    public PMR addPlayerGroup(String playerName, String groupName) {
        return addPlayerGroup(playerName, groupName, "");
    }

    public PMR addPlayerGroup(String playerName, String groupName, String server) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        Group group = getGroup(groupName);
        if (group == null)
            return new PMR(false, "Group does not exist.");

        try {
            String playerGroupString = "";

            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, playerName);
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next())
                playerGroupString = result.getString("groups");
            else
                return new PMR(false, "Player does not exist.");

            // Add group. Put it first.
            HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);
            List<Integer> groupList = playerGroups.get(server);
            if (groupList == null)
                groupList = new ArrayList<Integer>();
            if (groupList.contains(group.getId()))
                return new PMR(false, "Player already has this group.");
            groupList.add(group.getId());
            playerGroups.put(server, groupList);

            String playerGroupStringOutput = "";
            for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
                for (Integer groupId : entry.getValue())
                    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
            }

            s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `groups`=? WHERE `name`=?");
            s.setString(1, playerGroupStringOutput);
            s.setString(2, playerName);
            s.execute();

            reloadPlayer(playerName);
            notifyReloadPlayer(playerName);
            return new PMR("Player group set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }

    }

    // -------------------------------------------------------------------//
    // //
    // ------------GROUP PERMISSION MODIFYING FUNCTIONS BELOW-------------//
    // //
    // -------------------------------------------------------------------//

    public PMR createGroup(String name) {
        Iterator<Entry<Integer, Group>> it = this.groups.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Group> e = it.next();
            if (e.getValue().getName().equalsIgnoreCase(name)) {
                // Group already exists
                return new PMR(false, "Group already exists.");
            }
        }

        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroups + " SET `name`=?, `parents`=?, `prefix`=?, `suffix`=?");
            s.setString(1, name);
            s.setString(2, "");
            s.setString(3, "");
            s.setString(4, "");
            s.execute();
            // Reload groups
            loadGroups();
            notifyReloadGroups();
            return new PMR("Created group.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "Could not create group: SQL error code: " + e.getErrorCode());
        }
    }

    public PMR deleteGroup(String groupName) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroups + " WHERE `name`=?;");
            s.setString(1, groupName);
            s.execute();
            // Reload groups
            loadGroups();
            notifyReloadGroups();
            return new PMR("Deleted group.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "Could not delete group: SQL error code: " + e.getErrorCode());
        }
    }

    public PMR addGroupPermission(String groupName, String permission) {
        return addGroupPermission(groupName, permission, "", "");
    }

    public PMR addGroupPermission(String groupName, String permission, String world, String server) {
        Group group = getGroup(groupName);
        if (group != null) {
            ArrayList<PowerfulPermission> groupPermissions = group.getOwnPermissions();
            try {
                PowerfulPermission sp = new PowerfulPermission(permission, world, server);

                for (PowerfulPermission temp : groupPermissions) {
                    if (temp.getPermissionString().equals(permission) && temp.getServer().equals(server) && temp.getWorld().equals(world))
                        return new PMR(false, "Group already has the specified permission.");
                }

                groupPermissions.add(sp);

                PreparedStatement s = sql.getConnection().prepareStatement(
                        "INSERT INTO " + tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
                s.setString(1, "");
                s.setString(2, "");
                s.setString(3, groupName);
                s.setString(4, permission);
                s.setString(5, world);
                s.setString(6, server);
                s.execute();

                // Reload groups
                loadGroups();
                notifyReloadGroups();
                return new PMR("Added permission to group.");
            } catch (SQLException e) {
                e.printStackTrace();
                return new PMR(false, "SQL error code: " + e.getErrorCode());
            }
        } else
            return new PMR(false, "Group does not exist.");
    }

    public PMR removeGroupPermission(String groupName, String permission) {
        return removeGroupPermission(groupName, permission, "", "");
    }

    public PMR removeGroupPermission(String groupName, String permission, String world, String server) {
        // boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
        Group group = getGroup(groupName);
        if (group != null) {
            ArrayList<PowerfulPermission> removed = new ArrayList<PowerfulPermission>();
            ArrayList<PowerfulPermission> groupPermissions = group.getOwnPermissions();
            Iterator<PowerfulPermission> it = groupPermissions.iterator();
            while (it.hasNext()) {
                PowerfulPermission current = it.next();
                if (current.getPermissionString().equalsIgnoreCase(permission)) {
                    if (world.equals(current.getWorld()) && server.equals(current.getServer())) {
                        removed.add(current);
                        it.remove();
                    }
                }
            }

            try {
                if (removed.size() <= 0)
                    return new PMR(false, "Group does not have the specified permission.");

                int amount = 0;
                for (PowerfulPermission current : removed) {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=? AND `permission`=? AND `world`=? AND `server`=?");
                    s.setString(1, groupName);
                    s.setString(2, current.getPermissionString());
                    s.setString(3, current.getWorld());
                    s.setString(4, current.getServer());
                    amount += s.executeUpdate();
                }

                // Reload groups
                loadGroups();
                notifyReloadGroups();
                return new PMR("Removed " + amount + " permissions from the group.");
            } catch (SQLException e) {
                e.printStackTrace();
                return new PMR(false, "SQL error code: " + e.getErrorCode());
            }
        } else
            return new PMR(false, "Group does not exist.");
    }

    public PMR removeGroupPermissions(String groupName) {
        // boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
        Group group = getGroup(groupName);
        if (group != null) {
            ArrayList<PowerfulPermission> groupPermissions = group.getOwnPermissions();

            try {
                if (groupPermissions.size() <= 0)
                    return new PMR(false, "Group does not have any permissions.");

                int amount = 0;
                PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=?");
                s.setString(1, groupName);
                amount += s.executeUpdate();

                // Reload groups
                loadGroups();
                notifyReloadGroups();
                return new PMR("Removed " + amount + " permissions from the group.");
            } catch (SQLException e) {
                e.printStackTrace();
                return new PMR(false, "SQL error code: " + e.getErrorCode());
            }
        } else
            return new PMR(false, "Group does not exist.");
    }

    public PMR addGroupParent(String groupName, String parentGroupName) {
        Group group = getGroup(groupName);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupName);
            if (parentGroup != null) {
                String currentParents = group.getRawOwnParents();
                if (currentParents.contains(parentGroupName))
                    return new PMR(false, "Group already has that parent.");
                currentParents += parentGroup.getId() + ";";
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `parents`=? WHERE `name`=?");
                    s.setString(1, currentParents);
                    s.setString(2, groupName);
                    s.execute();
                    // Reload groups
                    loadGroups();
                    notifyReloadGroups();
                    return new PMR("Added parent to group.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    return new PMR(false, "SQL error code " + e.getErrorCode());
                }
            } else
                return new PMR(false, "Parent group does not exist.");
        } else
            return new PMR(false, "Group does not exist.");
    }

    public PMR removeGroupParent(String groupName, String parentGroupName) {
        Group group = getGroup(groupName);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupName);
            if (parentGroup != null) {
                String currentParents = group.getRawOwnParents();
                String toRemove = parentGroup.getId() + ";";
                if (!currentParents.contains(toRemove))
                    return new PMR(false, "Group does not have that parent.");
                currentParents = currentParents.replaceFirst(parentGroup.getId() + ";", "");
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `parents`=? WHERE `name`=?");
                    s.setString(1, currentParents);
                    s.setString(2, groupName);
                    s.execute();
                    // Reload groups
                    loadGroups();
                    notifyReloadGroups();
                    return new PMR("Removed parent from group.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    return new PMR(false, "SQL error code: " + e.getErrorCode());
                }
            } else
                return new PMR(false, "Parent group does not exist.");
        } else
            return new PMR(false, "Group does not exist.");
    }

    public PMR setGroupPrefix(String groupName, String prefix) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `prefix`=? WHERE `name`=?");
            s.setString(1, prefix);
            s.setString(2, groupName);
            s.execute();
            // Reload groups
            loadGroups();
            notifyReloadGroups();
            return new PMR("Group prefix set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

    public PMR setGroupSuffix(String groupName, String suffix) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `suffix`=? WHERE `name`=?");
            s.setString(1, suffix);
            s.setString(2, groupName);
            s.execute();
            // Reload groups
            loadGroups();
            notifyReloadGroups();
            return new PMR("Group suffix set.");
        } catch (SQLException e) {
            e.printStackTrace();
            return new PMR(false, "SQL error code " + e.getErrorCode());
        }
    }

}
