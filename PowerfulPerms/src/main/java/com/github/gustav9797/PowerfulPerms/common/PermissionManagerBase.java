package com.github.gustav9797.PowerfulPerms.common;

import com.github.gustav9797.PowerfulPerms.Redis.RedisConnection;
import com.github.gustav9797.PowerfulPerms.common.event.PowerfulEventHandler;
import com.github.gustav9797.PowerfulPerms.database.DBResult;
import com.github.gustav9797.PowerfulPerms.database.Database;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.github.gustav9797.PowerfulPerms.command.Utils;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.DBDocument;
import com.github.gustav9797.PowerfulPermsAPI.DefaultPermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.EventHandler;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.GroupPermissionExpiredEvent;
import com.github.gustav9797.PowerfulPermsAPI.IScheduler;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.PlayerGroupExpiredEvent;
import com.github.gustav9797.PowerfulPermsAPI.PlayerLoadedEvent;
import com.github.gustav9797.PowerfulPermsAPI.PlayerPermissionExpiredEvent;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;
import com.github.gustav9797.PowerfulPermsAPI.ServerMode;
import com.google.common.base.Charsets;

import redis.clients.jedis.Jedis;

public abstract class PermissionManagerBase {

    protected HashMap<UUID, PermissionPlayer> players = new HashMap<>();
    protected ReentrantLock playersLock = new ReentrantLock();

    protected ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<>();

    protected HashMap<Integer, Group> groups = new HashMap<>();
    protected ReentrantLock groupsLock = new ReentrantLock();

    private final Database db;
    protected PowerfulPermsPlugin plugin;
    protected int checkTimedTaskId = -1;

    protected LinkedHashMap<String, List<CachedGroup>> defaultGroups;

    protected EventHandler eventHandler;

    protected RedisConnection redis;

    public static boolean redisEnabled;
    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public static String serverName;
    public static String serverId;
    public static String consolePrefix = "[PowerfulPerms] ";
    public static String pluginPrefixShort = ChatColor.BLUE + "PP" + ChatColor.WHITE + "> ";

    public PermissionManagerBase(Database database, PowerfulPermsPlugin plugin, String serverName) {
        this.db = database;
        this.plugin = plugin;

        PermissionManagerBase.serverName = serverName;
        serverId = serverName + (new Random()).nextInt(5000) + (new Date()).getTime();

        eventHandler = new PowerfulEventHandler();

        final PowerfulPermsPlugin tempPlugin = plugin;

        db.applyPatches();

        if (!db.tableExists(Database.tblGroupParents))
            db.createTable(Database.tblGroupParents);

        if (!db.tableExists(Database.tblGroupPermissions))
            db.createTable(Database.tblGroupPermissions);

        if (!db.tableExists(Database.tblGroupPrefixes))
            db.createTable(Database.tblGroupPrefixes);

        if (!db.tableExists(Database.tblGroupSuffixes))
            db.createTable(Database.tblGroupSuffixes);

        if (!db.tableExists(Database.tblPlayerGroups))
            db.createTable(Database.tblPlayerGroups);

        if (!db.tableExists(Database.tblPlayerPermissions))
            db.createTable(Database.tblPlayerPermissions);

        if (!db.tableExists(Database.tblGroups)) {
            db.createTable(Database.tblGroups);

            try {
                Response response = createGroupBase("Guest", "default", 100);
                if (response.succeeded())
                    tempPlugin.getLogger().info("Created group Guest");
                else
                    tempPlugin.getLogger().info("Could not create group Guest");
                response = setGroupPrefixBase(getGroup("Guest").getId(), "[Guest] ", "");
                if (response.succeeded())
                    tempPlugin.getLogger().info("Set group Guest prefix to \"[Guest] \"");
                else
                    tempPlugin.getLogger().info("Could not set group Guest prefix to \"[Guest] \"");
                response = setGroupSuffixBase(getGroup("Guest").getId(), ": ", "");
                if (response.succeeded())
                    tempPlugin.getLogger().info("Set group Guest suffix to \": \"");
                else
                    tempPlugin.getLogger().info("Coult not set group Guest suffix to \": \"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!db.tableExists(Database.tblPlayers)) {
            db.createTable(Database.tblPlayers);

            try {
                Response response = createPlayerBase("[default]", DefaultPermissionPlayer.getUUID());
                if (response.succeeded())
                    tempPlugin.getLogger().info("Inserted player [default]");
                else
                    tempPlugin.getLogger().info("Could not insert player [default]");

                Group guest = getGroup("Guest");
                if (guest != null) {
                    response = addPlayerGroupBase(DefaultPermissionPlayer.getUUID(), guest.getId(), "", false, null);
                    if (response.succeeded())
                        tempPlugin.getLogger().info("Added group Guest to player [default]");
                    else
                        tempPlugin.getLogger().info("Could not add group Guest to player [default]");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (redisEnabled)
            this.redis = new RedisConnection(this, plugin, redis_ip, redis_port, redis_password);

        checkTimedTaskId = this.getScheduler().runRepeating(() -> {
            List<Group> tempTempGroups;
            groupsLock.lock();
            try {
                tempTempGroups = new ArrayList<>(groups.values());
            } finally {
                groupsLock.unlock();
            }
            final List<Group> tempGroups = tempTempGroups;

            HashMap<UUID, PermissionPlayer> tempTempPlayers;
            playersLock.lock();
            try {
                tempTempPlayers = new HashMap<>(players);
            } finally {
                playersLock.unlock();
            }
            final HashMap<UUID, PermissionPlayer> tempPlayers = tempTempPlayers;

            getScheduler().runAsync(() -> {
                for (Entry<UUID, PermissionPlayer> e : tempPlayers.entrySet())
                    checkPlayerTimedGroupsAndPermissions(e.getKey(), e.getValue());
                for (Group group : tempGroups)
                    checkGroupTimedPermissions(group);
            }, false);

        }, 60);
    }

    protected void checkPlayerTimedGroupsAndPermissions(final UUID uuid, PermissionPlayer player) {
        LinkedHashMap<String, List<CachedGroup>> playerGroups = player.getCachedGroups();
        for (Entry<String, List<CachedGroup>> e : playerGroups.entrySet()) {
            for (final CachedGroup cachedGroup : e.getValue()) {
                if (cachedGroup.getExpireTaskId() == -1 && cachedGroup.willExpire()) {
                    final String server = e.getKey();
                    Runnable removePlayerGroupRunnable = () -> {
                        try {
                            debug("CachedGroup " + cachedGroup.getId() + " in player " + uuid.toString() + " expired.");
                            Response response = removePlayerGroupBase(uuid, cachedGroup.getGroupId(), server, cachedGroup.isNegated(), cachedGroup.getExpirationDate());
                            if (response.succeeded()) {
                                plugin.getLogger().info("Group " + cachedGroup.getGroupId() + " in player " + uuid.toString() + " expired and was removed.");
                                getEventHandler().fireEvent(new PlayerGroupExpiredEvent(uuid, cachedGroup));
                            } else
                                debug("Could not remove expired player group. " + response.getResponse());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    };

                    if (cachedGroup.hasExpired())
                        removePlayerGroupRunnable.run();
                    else {
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, 5);
                        if (cachedGroup.getExpirationDate().before(calendar.getTime())) {
                            // CachedGroup will expire within 5 minutes
                            int taskId = getScheduler().runDelayed(removePlayerGroupRunnable, cachedGroup.getExpirationDate());
                            cachedGroup.setExpireTaskId(taskId);
                        }
                    }
                }
            }
        }

        List<Permission> permissions = player.getPermissions();
        for (final Permission ap : permissions) {
            final PowerfulPermission p = (PowerfulPermission) ap;
            if (p.getExpireTaskId() == -1 && p.willExpire()) {
                Runnable removePlayerPermissionRunnable = () -> {
                    try {
                        debug("Permission " + p.getId() + " in player " + uuid.toString() + " expired.");
                        Response response = removePlayerPermissionBase(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                        if (response.succeeded()) {
                            plugin.getLogger().info("Permission " + p.getId() + " in player " + uuid.toString() + " expired and was removed.");
                            getEventHandler().fireEvent(new PlayerPermissionExpiredEvent(uuid, p));
                        } else
                            debug("Could not remove expired player permission. " + response.getResponse());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                if (p.hasExpired())
                    removePlayerPermissionRunnable.run();
                else {

                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 5);
                    if (p.getExpirationDate().before(calendar.getTime())) {
                        // CachedGroup will expire within 5 minutes
                        int taskId = getScheduler().runDelayed(removePlayerPermissionRunnable, p.getExpirationDate());
                        p.setExpireTaskId(taskId);
                    }
                }
            }
        }
    }

    protected void checkGroupTimedPermissions(final Group group) {
        List<Permission> permissions = group.getOwnPermissions();
        for (final Permission ap : permissions) {
            final PowerfulPermission p = (PowerfulPermission) ap;
            if (p.getExpireTaskId() == -1 && p.willExpire()) {
                Runnable removeGroupPermissionRunnable = () -> {
                    try {
                        debug("Permission " + p.getId() + " in group " + group.getId() + " expired.");
                        Response response = removeGroupPermissionBase(group.getId(), p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                        if (response.succeeded()) {
                            plugin.getLogger().info("Permission " + p.getId() + " in group " + group.getId() + " expired and was removed.");
                            getEventHandler().fireEvent(new GroupPermissionExpiredEvent(group, p));
                        } else
                            debug("Could not remove expired group permission. " + response.getResponse());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };

                if (p.hasExpired())
                    removeGroupPermissionRunnable.run();
                else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, 5);
                    if (p.getExpirationDate().before(calendar.getTime())) {
                        // CachedGroup will expire within 5 minutes
                        int taskId = getScheduler().runDelayed(removeGroupPermissionRunnable, p.getExpirationDate());
                        p.setExpireTaskId(taskId);
                    }
                }
            }
        }
    }

    protected void debug(String msg) {
        plugin.debug(msg);
    }

    protected LinkedHashMap<String, List<CachedGroup>> deepCopyDefaultGroups() {
        if (defaultGroups != null) {
            LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<>();
            LinkedHashMap<String, List<CachedGroup>> input = new LinkedHashMap<>(defaultGroups);
            for (Entry<String, List<CachedGroup>> next : input.entrySet()) {
                output.put(next.getKey(), new ArrayList<>(next.getValue()));
            }
            return output;
        }
        return null;
    }

    public Jedis getRedisConnection() {
        return redis.getConnection();
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public Database getDatabase() {
        return db;
    }

    public void notifyReloadGroups() {
        if (redisEnabled && redis != null) {
            plugin.runTaskAsynchronously(() -> {
                Jedis jedis = redis.getConnection();
                if (jedis != null) {
                    jedis.publish("PowerfulPerms", "[groups]" + " " + serverId);
                    jedis.close();
                }
            });
        }
    }

    public void notifyReloadPlayers() {
        if (redisEnabled && redis != null) {
            plugin.runTaskAsynchronously(() -> {
                Jedis jedis = redis.getConnection();
                if (jedis != null) {
                    jedis.publish("PowerfulPerms", "[players]" + " " + serverId);
                    jedis.close();
                }
            });
        }
    }

    public void notifyReloadPlayer(final UUID uuid) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            notifyReloadPlayers();
        } else if (redisEnabled && redis != null) {
            plugin.runTaskAsynchronously(() -> {
                Jedis jedis = redis.getConnection();
                if (jedis != null) {
                    jedis.publish("PowerfulPerms", uuid + " " + serverId);
                    jedis.close();
                }
            });
        }
    }

    public void reloadPlayers() {
        playersLock.lock();
        try {
            ArrayList<UUID> uuids = new ArrayList<>(players.keySet());
            for (UUID uuid : uuids) {
                if (!plugin.isPlayerOnline(uuid)) {
                    players.remove(uuid);
                }
                debug("Reloading player " + uuid.toString());
                loadPlayer(uuid, null, false, false);
            }
        } finally {
            playersLock.unlock();
        }
    }

    // TODO: add reloadplayer(uuid, samethread) to API, remove this
    public void reloadPlayer(UUID uuid) {
        reloadPlayer(uuid, false);
    }

    public void reloadPlayer(String name) {
        if (plugin.isPlayerOnline(name)) {
            UUID uuid = plugin.getPlayerUUID(name);
            if (uuid != null) {
                this.loadPlayer(uuid, name, false, false);
            }
        }
    }

    public void reloadPlayer(UUID uuid, boolean sameThread) {
        if (plugin.isPlayerOnline(uuid)) {
            String name = plugin.getPlayerName(uuid);
            if (name != null) {
                this.loadPlayer(uuid, name, false, sameThread);
                plugin.debug("Reloaded player \"" + uuid + "\".");
            }
        } else if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            reloadDefaultPlayers(sameThread);
            plugin.debug("Changes to default player found, players reloaded");
        }
    }

    public void reloadDefaultPlayers(final boolean samethread) {
        LinkedHashMap<String, List<CachedGroup>> result = loadPlayerGroups(DefaultPermissionPlayer.getUUID());
        try {
            if (result != null) {
                defaultGroups = result;
                reloadPlayers();
                debug("DEFAULT PLAYER LOADED: " + (defaultGroups != null));
            } else
                plugin.getLogger().severe(consolePrefix + "Can not get data from user [default].");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public PermissionPlayer getPermissionPlayer(UUID uuid) {
        playersLock.lock();
        try {
            return players.get(uuid);
        } finally {
            playersLock.unlock();
        }
    }

    public PermissionPlayer getPermissionPlayer(String name) {
        UUID uuid = plugin.getPlayerUUID(name);
        return getPermissionPlayer(uuid);
    }

    protected void putPermissionPlayer(UUID uuid, PermissionPlayer permissionPlayer) {
        playersLock.lock();
        try {
            players.put(uuid, permissionPlayer);
        } finally {
            playersLock.unlock();
        }
    }

    protected void removePermissionPlayer(UUID uuid) {
        playersLock.lock();
        try {
            players.remove(uuid);
        } finally {
            playersLock.unlock();
        }
    }

    protected boolean containsPermissionPlayer(UUID uuid) {
        playersLock.lock();
        try {
            return players.containsKey(uuid);
        } finally {
            playersLock.unlock();
        }
    }

    public void loadPlayer(final UUID uuid, final String name, final boolean storeCache, final boolean sameThread) {
        debug("loadPlayer begin");
        db.scheduler.runAsync(() -> {
            DBResult result = db.getPlayer(uuid);
            if (result.booleanValue()) {
                final DBDocument row = result.next();
                if (row != null) {
                    // The player exists in database.

                    String playerName_loaded = row.getString("name");
                    debug("playername_loaded " + playerName_loaded);

                    if (name != null) {
                        debug("playerName " + name);

                        // Check if name mismatch, update player name
                        if (!playerName_loaded.equals(name)) {
                            debug("PLAYER NAME MISMATCH.");
                            boolean success = db.setPlayerName(uuid, name);
                            if (!success)
                                debug("COULD NOT UPDATE PLAYER NAME OF PLAYER " + uuid.toString());
                            else
                                debug("PLAYER NAME UPDATED. NAMECHANGE");
                            loadPlayerFinished(uuid, row, storeCache, sameThread);
                        } else
                            loadPlayerFinished(uuid, row, storeCache, sameThread);
                    } else
                        loadPlayerFinished(uuid, row, storeCache, sameThread);
                } else {
                    // Could not find player with UUID. Create new player.
                    boolean success = db.insertPlayer(uuid, name, "", "");
                    if (!success)
                        debug("COULD NOT CREATE PLAYER " + uuid.toString() + " - " + name);
                    else
                        debug("NEW PLAYER CREATED");
                    loadPlayerFinished(uuid, null, storeCache, sameThread);
                }
            }
        }, sameThread);
    }

    protected void loadPlayerFinished(final UUID uuid, final DBDocument row, final boolean storeCache, final boolean sameThread) {
        db.scheduler.runAsync(() -> {
            debug("loadPlayerFinished begin. storeCache: " + storeCache + " sameThread: " + sameThread);
            final String prefix_loaded = (row != null ? row.getString("prefix") : "");
            final String suffix_loaded = (row != null ? row.getString("suffix") : "");

            try {
                LinkedHashMap<String, List<CachedGroup>> tempGroups = loadPlayerGroups(uuid);
                List<Permission> perms = loadPlayerOwnPermissions(uuid);
                if (perms == null)
                    perms = new ArrayList<>();

                if (storeCache) {
                    debug("Inserted into cachedPlayers allowing playerjoin to finish");
                    cachedPlayers.put(uuid, new CachedPlayer(tempGroups, prefix_loaded, suffix_loaded, perms));
                } else {
                    // Player should be reloaded if "login" is false. Reload already loaded player.

                    PermissionPlayerBase toUpdate = (PermissionPlayerBase) getPermissionPlayer(uuid);
                    if (plugin.isPlayerOnline(uuid) && toUpdate != null) {
                        debug("Player instance " + toUpdate.toString());
                        PermissionPlayerBase base;
                        debug("loadPlayerFinished reload group count:" + tempGroups.size());
                        if (tempGroups.isEmpty()) {
                            // Player has no groups, put default data
                            base = new PermissionPlayerBase(deepCopyDefaultGroups(), perms, prefix_loaded, suffix_loaded, plugin, true);
                        } else
                            base = new PermissionPlayerBase(tempGroups, perms, prefix_loaded, suffix_loaded, plugin, false);
                        toUpdate.update(base);
                        checkPlayerTimedGroupsAndPermissions(uuid, toUpdate);

                        cachedPlayers.remove(uuid);
                        eventHandler.fireEvent(new PlayerLoadedEvent(uuid, plugin.getPlayerName(uuid)));
                    }
                }
                debug("loadPlayerFinished runnable end");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, sameThread);
    }

    protected PermissionPlayerBase loadCachedPlayer(UUID uuid) {
        debug("continueLoadPlayer " + uuid);
        CachedPlayer cachedPlayer = cachedPlayers.get(uuid);
        if (cachedPlayer == null) {
            plugin.getLogger().severe(consolePrefix + "Could not continue load player. Cached player is null.");
            return null;
        }

        PermissionPlayerBase base;
        if (cachedPlayer.getGroups().isEmpty()) {
            // Player has no groups, put default data
            base = new PermissionPlayerBase(deepCopyDefaultGroups(), cachedPlayer.getPermissions(), cachedPlayer.getPrefix(), cachedPlayer.getSuffix(), plugin, true);
        } else
            base = new PermissionPlayerBase(cachedPlayer.getGroups(), cachedPlayer.getPermissions(), cachedPlayer.getPrefix(), cachedPlayer.getSuffix(), plugin, false);
        cachedPlayers.remove(uuid);
        return base;
    }

    public void onDisable() {
        if (checkTimedTaskId != -1)
            this.getScheduler().stopRepeating(checkTimedTaskId);
        if (redis != null)
            redis.destroy();
        groupsLock.lock();
        try {
            if (groups != null)
                groups.clear();
        } finally {
            groupsLock.unlock();
        }
        playersLock.lock();
        try {
            if (players != null)
                players.clear();
        } finally {
            playersLock.unlock();
        }
        if (cachedPlayers != null)
            cachedPlayers.clear();
    }

    public void reloadGroups() {
        loadGroups();
    }

    /**
     * Loads groups from MySQL, removes old group data. Will reload all players too.
     */
    public void loadGroups() {
        loadGroups(false);
    }

    public void loadGroups(boolean sameThread) {
        loadGroups(sameThread, sameThread);
    }

    /**
     * Loads groups from MySQL, removes old group data. Will reload all players too. beginSameThread: Set this to true if you want it to fetch group data on the same thread you call from. Set it to
     * false and it will run asynchronously. endSameThread: Set this to true if you want to finish and insert groups on the same thread. Note: This -MUST- be Bukkit main thread you execute on. Set to
     * false if you want to run it synchronously but scheduled.
     */
    protected void loadGroups(final boolean beginSameThread, final boolean endSameThread) {
        debug("loadGroups begin");

        db.scheduler.runAsync(() -> {
            DBResult result = db.getGroups();
            if (result.booleanValue()) {
                final DBResult groupResult = result;

                try {
                    HashMap<Integer, List<Integer>> parents = loadGroupParents();
                    debug("loadgroups parents size: " + parents.size());

                    final HashMap<Integer, HashMap<String, String>> prefixes = loadGroupPrefixes();
                    final HashMap<Integer, HashMap<String, String>> suffixes = loadGroupSuffixes();
                    final HashMap<Integer, List<PowerfulPermission>> permissions = loadGroupPermissions();

                    groupsLock.lock();
                    try {
                        groups.clear();
                        while (groupResult.hasNext()) {
                            DBDocument row = groupResult.next();
                            final int groupId = row.getInt("id");
                            final String name = row.getString("name");
                            String ladder1 = row.getString("ladder");
                            final String ladder = (ladder1 == null || ladder1.isEmpty() ? "default" : ladder1);
                            final int rank = row.getInt("rank");

                            PowerfulGroup group = new PowerfulGroup(groupId, name, permissions.get(groupId), prefixes.get(groupId), suffixes.get(groupId), ladder, rank, plugin);
                            group.setParents(parents.get(groupId));
                            groups.put(groupId, group);

                            checkGroupTimedPermissions(group);
                        }
                        debug("loadGroups end");
                    } finally {
                        groupsLock.unlock();
                    }
                    // Reload players too.
                    reloadDefaultPlayers(beginSameThread);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, beginSameThread);

    }

    public Group getGroup(String groupName) {
        groupsLock.lock();
        try {
            for (Map.Entry<Integer, Group> e : groups.entrySet())
                if (e.getValue().getName().equalsIgnoreCase(groupName))
                    return e.getValue();
        } finally {
            groupsLock.unlock();
        }
        return null;
    }

    public Group getGroup(int id) {
        groupsLock.lock();
        try {
            return groups.get(id);
        } finally {
            groupsLock.unlock();
        }
    }

    public Map<Integer, Group> getGroups() {
        groupsLock.lock();
        try {
            return new HashMap<>(this.groups);
        } finally {
            groupsLock.unlock();
        }
    }

    public LinkedHashMap<String, List<CachedGroup>> getPlayerOwnGroupsBase(final UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null && !gp.isDefault())
            return gp.getCachedGroups();
        return loadPlayerGroups(uuid);
    }

    public LinkedHashMap<String, List<CachedGroup>> getPlayerCurrentGroupsBase(final UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null)
            return gp.getCachedGroups();

        LinkedHashMap<String, List<CachedGroup>> result = loadPlayerGroups(uuid);
        if (result != null) {
            if (result.isEmpty()) {
                result.putAll(deepCopyDefaultGroups());
            }
        }
        return result;
    }

    public Group getPlayerPrimaryGroupBase(UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null)
            return gp.getPrimaryGroup();

        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, result);
        List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
        return PermissionPlayerBase.getPrimaryGroup(groups);
    }

    public Boolean isPlayerDefaultBase(final UUID uuid) {
        PermissionPlayer permissionPlayer = getPermissionPlayer(uuid);
        if (permissionPlayer != null)
            return permissionPlayer.isDefault();

        LinkedHashMap<String, List<CachedGroup>> result = getPlayerOwnGroupsBase(uuid);
        if (result == null || result.isEmpty())
            return true;
        else
            return false;
    }

    protected Response copyDefaultGroups(final UUID uuid) {
        LinkedHashMap<String, List<CachedGroup>> result = getPlayerOwnGroupsBase(DefaultPermissionPlayer.getUUID());
        if (result == null || result.isEmpty())
            return new Response(false, "Could not retrieve the default groups.");
        else {
            LinkedHashMap<String, List<CachedGroup>> defaultGroups = result;
            for (Entry<String, List<CachedGroup>> entry : defaultGroups.entrySet()) {
                String server = entry.getKey();
                for (CachedGroup toAdd : entry.getValue()) {
                    boolean inserted = db.insertPlayerGroup(uuid, toAdd.getGroupId(), server, toAdd.isNegated(), toAdd.getExpirationDate());
                    if (!inserted)
                        plugin.getLogger().severe("Could not copy default group " + toAdd.getGroupId() + " to player " + uuid.toString() + ".");
                }
            }
            return new Response(true, "Default groups copied.");
        }
    }

    protected Response copyDefaultGroupsIfDefault(final UUID uuid) {
        if (isPlayerDefaultBase(uuid)) {
            return copyDefaultGroups(uuid);
        } else
            return new Response(false, "Player is not default.");
    }

    public DBDocument getPlayerDataBase(final UUID uuid) {
        DBResult result = db.getPlayer(uuid);
        if (result.hasNext()) {
            DBDocument row = result.next();
            return row;
        }
        return null;
    }

    public List<Permission> getPlayerOwnPermissionsBase(final UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null)
            return gp.getPermissions();

        List<Permission> perms = loadPlayerOwnPermissions(uuid);
        if (perms == null)
            perms = new ArrayList<>();
        return perms;
    }

    protected List<Permission> loadPlayerOwnPermissions(final UUID uuid) {
        DBResult result = db.getPlayerPermissions(uuid);
        if (result.booleanValue()) {
            ArrayList<Permission> perms = new ArrayList<>();
            while (result.hasNext()) {
                DBDocument row = result.next();
                Permission tempPerm = new PowerfulPermission(row.getInt("id"), row.getString("permission"), row.getString("world"), row.getString("server"), row.getDate("expires"));
                perms.add(tempPerm);
            }
            return perms;
        } else
            plugin.getLogger().severe("Could not load player permissions.");
        return null;
    }

    protected LinkedHashMap<String, List<CachedGroup>> loadPlayerGroups(final UUID uuid) {
        DBResult result = db.getPlayerGroups(uuid);
        if (result.booleanValue()) {
            LinkedHashMap<String, List<CachedGroup>> localGroups = new LinkedHashMap<>();
            while (result.hasNext()) {
                DBDocument row = result.next();
                int groupId = row.getInt("groupid");
                Group group = getGroup(groupId);
                if (group == null) {
                    plugin.getLogger().warning("Could not load stored player group, group does not exist");
                    continue;
                }

                if (!localGroups.containsKey(row.getString("server")))
                    localGroups.put(row.getString("server"), new ArrayList<>());
                List<CachedGroup> serverGroups = localGroups.get(row.getString("server"));
                serverGroups.add(new CachedGroup(row.getInt("id"), groupId, row.getBoolean("negated"), row.getDate("expires")));
                localGroups.put(row.getString("server"), serverGroups);
            }
            return localGroups;
        } else
            plugin.getLogger().severe("Could not load player groups.");
        return null;
    }

    protected HashMap<Integer, List<Integer>> loadGroupParents() {
        DBResult result = db.getGroupParents();
        if (result.booleanValue()) {
            HashMap<Integer, List<Integer>> parents = new HashMap<>();
            while (result.hasNext()) {
                DBDocument row = result.next();

                int groupId = row.getInt("groupid");
                int parentId = row.getInt("parentgroupid");
                if (!parents.containsKey(groupId))
                    parents.put(groupId, new ArrayList<>());
                List<Integer> localParents = parents.get(groupId);
                localParents.add(parentId);
                parents.put(groupId, localParents);
            }
            return parents;
        } else
            plugin.getLogger().severe("Could not load group parents.");
        return null;
    }

    protected HashMap<Integer, HashMap<String, String>> loadGroupPrefixes() {
        DBResult result = db.getGroupPrefixes();
        if (result.booleanValue()) {
            HashMap<Integer, HashMap<String, String>> prefixes = new HashMap<>();
            while (result.hasNext()) {
                DBDocument row = result.next();

                int groupId = row.getInt("groupid");
                if (!prefixes.containsKey(groupId))
                    prefixes.put(groupId, new HashMap<>());
                HashMap<String, String> localPrefixes = prefixes.get(groupId);
                localPrefixes.put(row.getString("server"), row.getString("prefix"));
                prefixes.put(groupId, localPrefixes);
            }
            return prefixes;
        } else
            plugin.getLogger().severe("Could not load group prefixes.");
        return null;
    }

    protected HashMap<Integer, HashMap<String, String>> loadGroupSuffixes() {
        DBResult result = db.getGroupSuffixes();
        if (result.booleanValue()) {
            HashMap<Integer, HashMap<String, String>> suffixes = new HashMap<>();
            while (result.hasNext()) {
                DBDocument row = result.next();

                int groupId = row.getInt("groupid");
                if (!suffixes.containsKey(groupId))
                    suffixes.put(groupId, new HashMap<>());
                HashMap<String, String> localSuffixes = suffixes.get(groupId);
                localSuffixes.put(row.getString("server"), row.getString("suffix"));
                suffixes.put(groupId, localSuffixes);
            }
            return suffixes;
        } else
            plugin.getLogger().severe("Could not load group suffixes.");
        return null;
    }

    protected HashMap<Integer, List<PowerfulPermission>> loadGroupPermissions() {
        DBResult result = db.getGroupPermissions();
        if (result.booleanValue()) {
            HashMap<Integer, List<PowerfulPermission>> permissions = new HashMap<>();
            while (result.hasNext()) {
                DBDocument row = result.next();

                int groupId = row.getInt("groupid");
                if (!permissions.containsKey(groupId))
                    permissions.put(groupId, new ArrayList<>());
                List<PowerfulPermission> localPermissions = permissions.get(groupId);
                localPermissions.add(new PowerfulPermission(row.getInt("id"), row.getString("permission"), row.getString("world"), row.getString("server"), row.getDate("expires")));
                permissions.put(groupId, localPermissions);
            }
            return permissions;
        } else
            plugin.getLogger().severe("Could not load group permissions.");
        return null;
    }

    public String getPlayerPrefixBase(final UUID uuid, final String ladder) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            if (ladder != null)
                return gp.getPrefix(ladder);
            return gp.getPrefix();
        }

        LinkedHashMap<String, List<CachedGroup>> currentGroups = getPlayerCurrentGroupsBase(uuid);
        List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, currentGroups);
        List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
        String prefix = "";
        if (ladder != null)
            prefix = PermissionPlayerBase.getPrefix(ladder, groups);
        else
            prefix = PermissionPlayerBase.getPrefix(groups, getPlayerOwnPrefixBase(uuid));
        return prefix;
    }

    public String getPlayerSuffixBase(final UUID uuid, final String ladder) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            if (ladder != null)
                return gp.getSuffix(ladder);
            return gp.getSuffix();
        }

        LinkedHashMap<String, List<CachedGroup>> currentGroups = getPlayerCurrentGroupsBase(uuid);
        List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, currentGroups);
        List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
        String suffix = "";
        if (ladder != null)
            suffix = PermissionPlayerBase.getSuffix(ladder, groups);
        else
            suffix = PermissionPlayerBase.getSuffix(groups, getPlayerOwnSuffixBase(uuid));
        return suffix;
    }

    public String getPlayerOwnPrefixBase(final UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null)
            return gp.getOwnPrefix();

        DBResult result = db.getPlayer(uuid);
        if (result.hasNext()) {
            DBDocument row = result.next();
            return row.getString("prefix");
        }
        return null;
    }

    public String getPlayerOwnSuffixBase(final UUID uuid) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null)
            return gp.getOwnSuffix();

        DBResult result = db.getPlayer(uuid);
        if (result.hasNext()) {
            DBDocument row = result.next();
            return row.getString("suffix");
        }
        return null;
    }

    public String getGroupPrefix(int groupId, String server) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getPrefix(server);
        return null;
    }

    public String getGroupSuffix(int groupId, String server) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getSuffix(server);
        return null;
    }

    public HashMap<String, String> getGroupServerPrefix(int groupId) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getPrefixes();
        return null;
    }

    public HashMap<String, String> getGroupServerSuffix(int groupId) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getSuffixes();
        return null;
    }

    public UUID getConvertUUIDBase(final String playerName) {
        if (playerName.equalsIgnoreCase("[default]") || playerName.equalsIgnoreCase("{default}"))
            return DefaultPermissionPlayer.getUUID();

        // If player name is UUID, return it directly
        try {
            UUID uuid = UUID.fromString(playerName);
            return uuid;
        } catch (Exception e) {
        }

        // If player is online, get UUID directly
        if (plugin.isPlayerOnline(playerName))
            return plugin.getPlayerUUID(playerName);

        // If player UUID exists in database, use that
        DBResult result;
        if (plugin.getServerMode() == ServerMode.ONLINE)
            result = db.getPlayersCaseInsensitive(playerName);
        else
            result = db.getPlayers(playerName);

        if (result.hasNext()) {
            final DBDocument row = result.next();
            if (row != null) {
                String stringUUID = row.getString("uuid");
                UUID uuid = UUID.fromString(stringUUID);
                if (uuid != null) {
                    debug("UUID found in DB, skipping mojang api lookup");
                    return uuid;
                }
            }
        }

        // Check if DB contains online uuid. If so, return it.
        // Check if DB contains offline uuid. If so, return it. If not, return online uuid.
        if (plugin.getServerMode() == ServerMode.MIXED) {
            // Generate offline UUID and check database if it exists. If so, return it.
            final UUID offlineuuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
            debug("Generated mixed mode offline UUID " + offlineuuid);

            // Get online UUID.

            debug("Begin UUID retrieval...");
            ArrayList<String> list = new ArrayList<>();
            list.add(playerName);
            UUIDFetcher fetcher = new UUIDFetcher(list);
            try {
                Map<String, UUID> result2 = fetcher.call();
                if (result2 != null && result2.containsKey(playerName)) {
                    final UUID onlineuuid = result2.get(playerName);
                    debug("Retrieved UUID " + onlineuuid);
                    // Check if database contains online UUID.

                    DBResult result3 = db.getPlayer(onlineuuid);
                    if (result3.hasNext()) {
                        // Database contains online UUID. Return it.
                        debug("online UUID found in DB");
                        return onlineuuid;
                    } else {
                        // Could not find online UUID in database.
                        // Check if offline UUID exists.
                        debug("online UUID not found in DB");
                        DBResult offline = db.getPlayer(offlineuuid);
                        if (offline.hasNext()) {
                            // Found offline UUID in database. Return it.
                            debug("offline UUID found in DB, return offline");
                            return offlineuuid;
                        } else {
                            // Could not find neither of offline or online UUIDs in database.
                            // Online UUID exists for player name so return it.
                            debug("offline UUID not found in DB, return online");
                            return onlineuuid;
                        }
                    }
                } else {
                    // Could not find online UUID for specified name
                    debug("Did not find online UUID for player name " + playerName + ", return offline");
                    return offlineuuid;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            if (plugin.getServerMode() == ServerMode.ONLINE) {
                // Convert player name to UUID using Mojang API
                debug("Begin UUID retrieval...");
                ArrayList<String> list = new ArrayList<>();
                list.add(playerName);
                UUIDFetcher fetcher = new UUIDFetcher(list);
                try {
                    Map<String, UUID> result2 = fetcher.call();
                    if (result2 != null) {
                        for (Entry<String, UUID> e : result2.entrySet()) {
                            if (e.getKey().equalsIgnoreCase(playerName)) {
                                debug("Retrieved UUID " + e.getValue());
                                return e.getValue();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Generate UUID from player name
                UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
                debug("Generated offline mode UUID " + uuid);
                return uuid;
            }
        }
        return null;
    }

    public IScheduler getScheduler() {
        return db.scheduler;
    }

    public Response createPlayerBase(final String name, final UUID uuid) {
        loadPlayer(uuid, name, true, true);
        return new Response(true, "Player created.");
    }

    // -------------------------------------------------------------------//
    // //
    // ------------PLAYER PERMISSION MODIFYING FUNCTIONS BELOW------------//
    // //
    // -------------------------------------------------------------------//

    public Response addPlayerPermissionBase(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        final Date now = new Date();
        if (uuid.equals(DefaultPermissionPlayer.getUUID()))
            return new Response(false, "You can not add permissions to the default player. Add them to a group instead and add the group to the default player.");

        // Check if the same permission already exists.
        boolean hasPermission = db.playerHasPermission(uuid, permission, world, server, expires);
        if (hasPermission)
            return new Response(false, "Player already has the specified permission.");
        else {
            DBResult result = db.getPlayer(uuid);
            if (result.hasNext()) {
                if (expires != null) {
                    for (Permission p : getPlayerOwnPermissionsBase(uuid)) {
                        if (p.willExpire()) {
                            if (p.getPermissionString().equals(permission) && p.getServer().equalsIgnoreCase(server) && p.getWorld().equalsIgnoreCase(world)) {
                                final Date newExpiry = new Date(p.getExpirationDate().getTime() + (expires.getTime() - now.getTime()));
                                DBResult result2 = db.deletePlayerPermission(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                                if (!result2.booleanValue())
                                    return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                                else {
                                    boolean inserted = db.insertPlayerPermission(uuid, permission, world, server, newExpiry);
                                    if (!inserted) {
                                        return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                                    } else {
                                        reloadPlayer(uuid, true);
                                        notifyReloadPlayer(uuid);
                                        return new Response(true, "Permission expiration changed.");
                                    }
                                }
                            }
                        }
                    }
                }
                boolean inserted = db.insertPlayerPermission(uuid, permission, world, server, expires);
                if (inserted) {
                    reloadPlayer(uuid, true);
                    notifyReloadPlayer(uuid);
                    return new Response(true, "Permission added to player.");
                } else
                    return new Response(false, "Could not add permission. Check console for any errors.");
            } else
                return new Response(false, "Could not add permission. Player doesn't exist.");
        }
    }

    public Response removePlayerPermissionBase(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        boolean anyServer = false;
        if (server.equalsIgnoreCase("any"))
            anyServer = true;
        String tempServer = server;
        if (server.equalsIgnoreCase("all"))
            tempServer = "";

        boolean anyWorld = false;
        if (world.equalsIgnoreCase("any"))
            anyWorld = true;
        String tempWorld = world;
        if (world.equalsIgnoreCase("all"))
            tempWorld = "";

        List<Permission> result = getPlayerOwnPermissionsBase(uuid);
        if (result != null) {
            List<Permission> toDelete = new ArrayList<>();
            for (Permission e : result) {
                if (e.getPermissionString().equalsIgnoreCase(permission)) {
                    if (anyServer || e.getServer().equalsIgnoreCase(tempServer)) {
                        if (anyWorld || e.getWorld().equalsIgnoreCase(tempWorld)) {
                            if (Utils.dateApplies(e.getExpirationDate(), expires)) {
                                toDelete.add(e);
                            }
                        }
                    }
                }
            }

            if (toDelete.isEmpty())
                return new Response(false, "Player does not have this permission.");

            int deleted = 0;
            for (Permission e : toDelete) {
                DBResult dbresult = db.deletePlayerPermission(uuid, e.getPermissionString(), e.getWorld(), e.getServer(), e.getExpirationDate());
                if (dbresult.booleanValue())
                    deleted += dbresult.rowsChanged();
            }

            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            if (deleted > 0)
                return new Response(true, "Removed " + deleted + " of " + toDelete.size() + " player permissions.");
            else
                return new Response(false, "Removed " + deleted + " of " + toDelete.size() + " player permissions.");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Response removePlayerPermissionsBase(final UUID uuid) {
        DBResult result = db.deletePlayerPermissions(uuid);
        if (result.booleanValue()) {
            int amount = result.rowsChanged();
            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            return new Response(true, "Removed " + amount + " permissions from the player.");
        } else
            return new Response(false, "Player does not have any permissions.");
    }

    public Response setPlayerPrefixBase(final UUID uuid, final String prefix) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID()))
            return new Response(false, "You can not set the prefix of the default player. Add it to a group instead and add the group to the default player.");

        boolean success = db.setPlayerPrefix(uuid, prefix);
        if (success) {
            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            return new Response(true, "Player prefix set.");
        } else
            return new Response(false, "Could not set player prefix. Check console for errors.");
    }

    public Response setPlayerSuffixBase(final UUID uuid, final String suffix) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID()))
            return new Response(false, "You can not set the suffix of the default player. Add it to a group instead and add the group to the default player.");

        boolean success = db.setPlayerSuffix(uuid, suffix);
        if (success) {
            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            return new Response(true, "Player suffix set.");
        } else
            return new Response(false, "Could not set player suffix. Check console for errors.");
    }

    public Response removePlayerGroupBase(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        String tempServer = server;
        if (server.equalsIgnoreCase("all"))
            tempServer = "";
        boolean any = false;
        if (server.equalsIgnoreCase("any"))
            any = true;

        final String serverFinal = tempServer;

        final Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        if (result != null) {

            // Calc amount to see if there are any to remove at all.
            int amountToDelete = 0;
            for (Entry<String, List<CachedGroup>> e : result.entrySet()) {
                if (any || e.getKey().equalsIgnoreCase(serverFinal)) {
                    for (CachedGroup cachedGroup : e.getValue()) {
                        if (cachedGroup.getGroupId() == groupId && cachedGroup.isNegated() == negated && Utils.dateApplies(cachedGroup.getExpirationDate(), expires))
                            ++amountToDelete;
                    }
                }
            }

            if (amountToDelete == 0)
                return new Response(false, "Player does not have this group.");

            // Important: copy default groups
            copyDefaultGroupsIfDefault(uuid);

            // Reset amount and actually remove.
            int amount = 0;
            for (Entry<String, List<CachedGroup>> e : result.entrySet()) {
                if (any || e.getKey().equalsIgnoreCase(serverFinal)) {
                    for (CachedGroup cachedGroup : e.getValue()) {
                        if (cachedGroup.getGroupId() == groupId && cachedGroup.isNegated() == negated && Utils.dateApplies(cachedGroup.getExpirationDate(), expires)) {
                            if (db.deletePlayerGroup(uuid, cachedGroup.getGroupId(), e.getKey(), cachedGroup.isNegated(), cachedGroup.getExpirationDate()))
                                ++amount;
                        }
                    }
                }
            }

            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            if (amount > 0)
                return new Response(true, "Removed " + amount + " of " + amountToDelete + " player groups.");
            else
                return new Response(false, "Removed " + amount + " of " + amountToDelete + " player groups.");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Response addPlayerGroupBase(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        final Date now = new Date();
        String tempServer = server;
        if (server.equalsIgnoreCase("all"))
            tempServer = "";

        final String serverFinal = tempServer;

        final Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        if (result != null) {
            List<CachedGroup> groupList = result.get(serverFinal);
            if (groupList == null)
                groupList = new ArrayList<>();
            for (CachedGroup cachedGroup : groupList) {
                if (cachedGroup.getGroupId() == groupId && cachedGroup.isNegated() == negated) {
                    // Update expiration date instead
                    final Date newExpiry = (expires == null || cachedGroup.getExpirationDate() == null ? expires
                            : new Date(cachedGroup.getExpirationDate().getTime() + (expires.getTime() - now.getTime())));
                    if (newExpiry == cachedGroup.getExpirationDate() || (newExpiry != null && newExpiry.equals(cachedGroup.getExpirationDate())))
                        return new Response(false, "Player already has this group.");
                    boolean deleted = db.deletePlayerGroup(uuid, cachedGroup.getGroupId(), serverFinal, cachedGroup.isNegated(), cachedGroup.getExpirationDate());
                    if (!deleted) {
                        return new Response(false, "Could not update player group expiration date. Check console for any errors.");
                    } else {
                        boolean inserted = db.insertPlayerGroup(uuid, groupId, serverFinal, negated, newExpiry);
                        if (!inserted) {
                            return new Response(false, "Could not update player group expiration date. Check console for any errors.");
                        } else {
                            reloadPlayer(uuid, true);
                            notifyReloadPlayer(uuid);
                            return new Response(true, "Group expiration changed.");
                        }
                    }
                } else if (cachedGroup.getGroupId() == groupId && cachedGroup.isNegated() == negated && Utils.dateApplies(cachedGroup.getExpirationDate(), expires))
                    return new Response(false, "Player already has this group.");
            }

            copyDefaultGroupsIfDefault(uuid);

            boolean inserted = db.insertPlayerGroup(uuid, groupId, serverFinal, negated, expires);
            if (inserted) {
                reloadPlayer(uuid, true);
                notifyReloadPlayer(uuid);
                return new Response(true, "Player group added.");
            } else
                return new Response(false, "Could not add player group. Check console for errors.");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Response setPlayerRankBase(final UUID uuid, final int groupId) {
        // Get player groups on specified ladder
        // Use the group type of the first one of those groups
        // replace old group with group "groupname"

        final Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        if (result != null) {
            copyDefaultGroupsIfDefault(uuid);

            Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
            boolean changed = false;
            Group toUse = null;
            while (it.hasNext()) {
                Entry<String, List<CachedGroup>> next = it.next();
                final String server = next.getKey();
                List<CachedGroup> playerCurrentGroups = next.getValue();
                for (CachedGroup current : playerCurrentGroups) {
                    final Group currentGroup = getGroup(current.getGroupId());
                    if (currentGroup.getLadder().equals(group.getLadder()) && current.getExpirationDate() == null) {
                        if (toUse == null)
                            toUse = currentGroup;
                        // Replace with new group if they are on the same ladder and if toUse and current is the same group
                        if (toUse.getId() == currentGroup.getId()) {
                            boolean deleted = db.deletePlayerGroup(uuid, currentGroup.getId(), server, current.isNegated(), null);
                            debug("(setrank) removed group " + currentGroup.getId());
                            if (!deleted)
                                return new Response(false, "Could not remove group with ID " + currentGroup.getId() + ".");
                            else {
                                boolean inserted = db.insertPlayerGroup(uuid, groupId, server, current.isNegated(), null);
                                debug("(setrank) added group " + groupId);
                                if (!inserted)
                                    return new Response(false, "Could not add group with ID " + groupId + ".");
                            }
                            changed = true;
                        } else // Prevents multiple different groups in the same ladder
                            db.deletePlayerGroup(uuid, currentGroup.getId(), server, current.isNegated(), null);
                    }
                }
            }

            if (!changed) {
                boolean inserted = db.insertPlayerGroup(uuid, groupId, "", false, null);
                debug("(setrank) added group " + groupId + ". had no groups in ladder");
                if (!inserted)
                    return new Response(false, "Could not add group with ID " + groupId + ".");
            }
            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            return new Response(true, "Player rank set on ladder \"" + group.getLadder() + "\".");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Group getHigherGroup(Group group) {
        List<Group> groups;
        groupsLock.lock();
        try {
            groups = new ArrayList<>(this.groups.values());
        } finally {
            groupsLock.unlock();
        }

        TreeMap<Integer, Group> sortedGroups = new TreeMap<>();
        for (Group current : groups) {
            if (current.getLadder().equals(group.getLadder()))
                sortedGroups.put(current.getRank(), current);
        }

        Iterator<Entry<Integer, Group>> it = sortedGroups.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Group> entry = it.next();
            if (entry.getKey() == group.getRank() && entry.getValue().getName().equals(group.getName())) {
                if (it.hasNext()) {
                    Group next = it.next().getValue();
                    if (next.getId() == group.getId())
                        return null;
                    return next;
                } else
                    return null;
            }
        }
        return null;
    }

    public Group getLowerGroup(Group group) {
        List<Group> groups;
        groupsLock.lock();
        try {
            groups = new ArrayList<>(this.groups.values());
        } finally {
            groupsLock.unlock();
        }

        TreeMap<Integer, Group> sortedGroups = new TreeMap<>(Collections.reverseOrder());
        for (Group current : groups) {
            if (current.getLadder().equals(group.getLadder()))
                sortedGroups.put(current.getRank(), current);
        }

        Iterator<Entry<Integer, Group>> it = sortedGroups.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Integer, Group> entry = it.next();
            if (entry.getKey() == group.getRank() && entry.getValue().getName().equals(group.getName())) {
                if (it.hasNext()) {
                    Group next = it.next().getValue();
                    if (next.getId() == group.getId())
                        return null;
                    return next;
                } else
                    return null;
            }
        }
        return null;
    }

    public Response promotePlayerBase(final UUID uuid, final String ladder) {
        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        copyDefaultGroupsIfDefault(uuid);
        if (result != null) {
            if (!result.isEmpty()) {
                Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                boolean changed = false;
                Group toUse = null;
                Group toPromoteTo = null;
                while (it.hasNext()) {
                    Entry<String, List<CachedGroup>> next = it.next();
                    final String server = next.getKey();
                    List<CachedGroup> playerCurrentGroups = next.getValue();

                    for (CachedGroup current : playerCurrentGroups) {
                        final Group currentGroup = getGroup(current.getGroupId());
                        if (currentGroup.getLadder().equalsIgnoreCase(ladder) && !current.willExpire() && !current.isNegated()) {
                            if (toUse == null) {
                                toUse = currentGroup;
                                toPromoteTo = getHigherGroup(toUse);
                                if (toPromoteTo == null)
                                    return new Response(false, "There is no group on this ladder with a higher rank.");
                            }
                            // Replace with new group if they are on the same ladder and if toUse and current is the same group
                            if (toUse.getId() == currentGroup.getId()) {
                                // This is the group to promote from
                                final Group toPromoteToFinal = toPromoteTo;
                                boolean deleted = db.deletePlayerGroup(uuid, currentGroup.getId(), server, current.isNegated(), null);
                                debug("(promote) removed group " + currentGroup.getId() + " " + currentGroup.getName());
                                if (!deleted)
                                    return new Response(false, "Could not remove group with ID " + currentGroup.getId() + ".");
                                else {
                                    boolean inserted = db.insertPlayerGroup(uuid, toPromoteToFinal.getId(), server, current.isNegated(), null);
                                    debug("(promote) added group " + toPromoteToFinal.getId() + " " + toPromoteToFinal.getName());
                                    if (!inserted)
                                        return new Response(false, "Could not add group with ID " + toPromoteToFinal.getId() + ".");
                                }
                                changed = true;
                            }
                        }
                    }
                }

                if (!changed) {
                    return new Response(false, "Player has no groups on the specified ladder.");
                } else {
                    reloadPlayer(uuid, true);
                    notifyReloadPlayer(uuid);
                    return new Response(true, "Player was promoted to \"" + toPromoteTo.getName() + "\".");
                }

            } else
                return new Response(false, "Player has no groups.");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Response demotePlayerBase(final UUID uuid, final String ladder) {
        LinkedHashMap<String, List<CachedGroup>> result = getPlayerCurrentGroupsBase(uuid);
        copyDefaultGroupsIfDefault(uuid);
        if (result != null) {
            if (!result.isEmpty()) {
                Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                boolean changed = false;
                Group toUse = null;
                Group toDemoteTo = null;
                while (it.hasNext()) {
                    Entry<String, List<CachedGroup>> next = it.next();
                    final String server = next.getKey();
                    List<CachedGroup> playerCurrentGroups = next.getValue();

                    for (CachedGroup current : playerCurrentGroups) {
                        final Group currentGroup = getGroup(current.getGroupId());
                        if (currentGroup.getLadder().equalsIgnoreCase(ladder) && !current.willExpire() && !current.isNegated()) {
                            if (toUse == null) {
                                toUse = currentGroup;
                                toDemoteTo = getLowerGroup(toUse);
                                if (toDemoteTo == null)
                                    return new Response(false, "There is no group on this ladder with a lower rank.");
                            }
                            // Remove old group and add lower group if they are on the same ladder and if toUse and current is the same group
                            if (toUse.getId() == currentGroup.getId()) {
                                final Group toDemoteToFinal = toDemoteTo;
                                boolean deleted = db.deletePlayerGroup(uuid, currentGroup.getId(), server, current.isNegated(), null);
                                debug("(demote) removed group " + currentGroup.getId());
                                if (!deleted)
                                    return new Response(false, "Could not remove group with ID " + currentGroup.getId() + ".");
                                else {
                                    boolean inserted = db.insertPlayerGroup(uuid, toDemoteToFinal.getId(), server, current.isNegated(), null);
                                    debug("(demote) added group " + toDemoteToFinal.getId());
                                    if (!inserted)
                                        return new Response(false, "Could not add group with ID " + toDemoteToFinal.getId() + ".");
                                }
                                changed = true;
                            }
                        }
                    }
                }

                if (!changed) {
                    return new Response(false, "Player has no groups on the specified ladder.");
                } else {
                    reloadPlayer(uuid, true);
                    notifyReloadPlayer(uuid);
                    return new Response(true, "Player was demoted to \"" + toDemoteTo.getName() + "\".");
                }
            } else
                return new Response(false, "Player has no groups.");
        } else
            return new Response(false, "Player does not exist.");
    }

    public Response deletePlayerBase(UUID uuid) {
        DBResult result = db.deletePlayer(uuid);
        if (result.rowsChanged() > 0) {
            reloadPlayer(uuid, true);
            notifyReloadPlayer(uuid);
            return new Response(true, "Deleted " + result.rowsChanged() + " player" + (result.rowsChanged() > 1 ? "s" : "") + ".");
        } else
            return new Response(false, "Player does not exist.");

    }

    // -------------------------------------------------------------------//
    // //
    // ------------GROUP PERMISSION MODIFYING FUNCTIONS BELOW-------------//
    // //
    // -------------------------------------------------------------------//

    public Response createGroupBase(final String name, final String ladder, final int rank) {
        Map<Integer, Group> groupsClone;
        groupsLock.lock();
        try {
            groupsClone = new HashMap<>(groups);
        } finally {
            groupsLock.unlock();
        }
        for (Entry<Integer, Group> e : groupsClone.entrySet()) {
            if (e.getValue().getName().equalsIgnoreCase(name))
                return new Response(false, "Group already exists.");
        }

        boolean inserted = db.insertGroup(-1, name, ladder, rank);
        if (inserted) {
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Created group.");
        } else
            return new Response(false, "Group already exists.");
    }

    public Response deleteGroupBase(final int groupId) {
        boolean deleted = db.deleteGroup(groupId);
        if (deleted) {
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Deleted group.");
        } else
            return new Response(false, "Group does not exist.");
    }

    public Response addGroupPermissionBase(final int groupId, final String permission, final String world, final String server, final Date expires) {
        Date now = new Date();
        Group group = getGroup(groupId);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            if (expires != null) {
                List<Permission> result = group.getOwnPermissions();
                for (Permission p : result) {
                    if (p.willExpire()) {
                        if (p.getPermissionString().equals(permission) && p.getServer().equalsIgnoreCase(server) && p.getWorld().equalsIgnoreCase(world)) {
                            final Date newExpiry = new Date(p.getExpirationDate().getTime() + (expires.getTime() - now.getTime()));
                            DBResult result2 = db.deleteGroupPermission(groupId, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                            if (!result2.booleanValue())
                                return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                            else {
                                boolean inserted = db.insertGroupPermission(groupId, permission, world, server, newExpiry);
                                if (!inserted) {
                                    return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                                } else {
                                    loadGroups(true);
                                    notifyReloadGroups();
                                    return new Response(true, "Permission expiration changed.");
                                }
                            }
                        }
                    }
                }
            }

            for (Permission temp : groupPermissions) {
                if (temp.getPermissionString().equals(permission) && temp.getServer().equals(server) && temp.getWorld().equals(world)
                        && (expires == null ? true : (expires.equals(temp.getExpirationDate())))) {
                    return new Response(false, "Group already has the specified permission.");
                }
            }

            boolean inserted = db.insertGroupPermission(groupId, permission, world, server, expires);
            if (inserted) {
                loadGroups(true);
                notifyReloadGroups();
                return new Response(true, "Added permission to group.");
            } else
                return new Response(false, "Could not add permission to group. Check console for errors.");
        } else
            return new Response(false, "Group does not exist.");
    }

    public Response removeGroupPermissionBase(final int groupId, final String permission, final String world, final String server, final Date expires) {
        Group group = getGroup(groupId);
        if (group != null) {

            boolean anyServer = false;
            if (server.equalsIgnoreCase("any"))
                anyServer = true;
            String tempServer = server;
            if (server.equalsIgnoreCase("all"))
                tempServer = "";

            boolean anyWorld = false;
            if (world.equalsIgnoreCase("any"))
                anyWorld = true;
            String tempWorld = world;
            if (world.equalsIgnoreCase("all"))
                tempWorld = "";

            List<Permission> groupPermissions = group.getOwnPermissions();
            List<Permission> toDelete = new ArrayList<>();
            for (Permission e : groupPermissions) {
                if (e.getPermissionString().equalsIgnoreCase(permission)) {
                    if (anyServer || e.getServer().equalsIgnoreCase(tempServer)) {
                        if (anyWorld || e.getWorld().equalsIgnoreCase(tempWorld)) {
                            if (Utils.dateApplies(e.getExpirationDate(), expires)) {
                                toDelete.add(e);
                            }
                        }
                    }
                }
            }

            if (toDelete.isEmpty())
                return new Response(false, "The group does not have this permission.");

            int deleted = 0;
            for (Permission e : toDelete) {
                DBResult result = db.deleteGroupPermission(groupId, e.getPermissionString(), e.getWorld(), e.getServer(), e.getExpirationDate());
                if (result.booleanValue())
                    deleted += result.rowsChanged();
            }

            loadGroups(true);
            notifyReloadGroups();
            if (deleted > 0)
                return new Response(true, "Removed " + deleted + " of " + toDelete.size() + " group permissions.");
            else
                return new Response(false, "Removed " + deleted + " of " + toDelete.size() + " group permissions.");

        } else
            return new Response(false, "Group does not exist.");
    }

    public Response removeGroupPermissionsBase(final int groupId) {
        Group group = getGroup(groupId);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            if (groupPermissions.size() <= 0)
                return new Response(false, "Group does not have any permissions.");

            DBResult result = db.deleteGroupPermissions(groupId);
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Removed " + result.rowsChanged() + " permissions from the group.");

        } else
            return new Response(false, "Group does not exist.");
    }

    public Response addGroupParentBase(final int groupId, final int parentGroupId) {
        Group group = getGroup(groupId);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupId);
            if (parentGroup != null) {
                if (group.getParents().contains(parentGroup))
                    return new Response(false, "Group already has the specified parent.");

                boolean inserted = db.insertGroupParent(groupId, parentGroupId);
                if (inserted) {
                    loadGroups(true);
                    notifyReloadGroups();
                    return new Response(true, "Added parent to group.");
                } else
                    return new Response(false, "Could not add parent to group. Check console for errors.");

            } else
                return new Response(false, "Parent group does not exist.");
        } else
            return new Response(false, "Group does not exist.");
    }

    public Response removeGroupParentBase(final int groupId, final int parentGroupId) {
        Group group = getGroup(groupId);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupId);
            if (parentGroup != null) {
                if (!group.getParents().contains(parentGroup))
                    return new Response(false, "Group does not have the specified parent.");
                boolean deleted = db.deleteGroupParent(groupId, parentGroupId);
                if (deleted) {
                    loadGroups(true);
                    notifyReloadGroups();
                    return new Response(true, "Removed parent from group.");
                } else
                    return new Response(false, "Could not remove parent from group. Check console for errors.");
            } else
                return new Response(false, "Parent group does not exist.");
        } else
            return new Response(false, "Group does not exist.");
    }

    public Response setGroupPrefixBase(final int groupId, final String prefix, final String server) {
        String tempServer = server;
        if (server.equalsIgnoreCase("all"))
            tempServer = "";
        tempServer = tempServer.toLowerCase();
        final String finalServer = server;

        final Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        // Run async while editing db
        HashMap<String, String> currentPrefix = group.getPrefixes();
        if (prefix.isEmpty() || currentPrefix.containsKey(finalServer)) {
            boolean deleted = db.deleteGroupPrefix(groupId, currentPrefix.get(finalServer), finalServer);
            if (!deleted)
                return new Response(false, "Could not delete group prefix. Check console for errors.");
        }
        if (!prefix.isEmpty()) {
            boolean inserted = db.insertGroupPrefix(groupId, prefix, finalServer);
            if (!inserted)
                return new Response(false, "Could not add group prefix. Check console for errors.");
        }

        loadGroups(true);
        notifyReloadGroups();
        return new Response(true, "Group prefix set.");
    }

    public Response setGroupSuffixBase(final int groupId, final String suffix, final String server) {
        String tempServer = server;
        if (server.equalsIgnoreCase("all"))
            tempServer = "";
        tempServer = tempServer.toLowerCase();
        final String finalServer = server;

        final Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");
        // Run async while editing db
        HashMap<String, String> currentSuffix = group.getPrefixes();
        if (suffix.isEmpty() || currentSuffix.containsKey(finalServer)) {
            boolean deleted = db.deleteGroupSuffix(groupId, currentSuffix.get(finalServer), finalServer);
            if (!deleted)
                return new Response(false, "Could not delete group suffix. Check console for errors.");
        }
        if (!suffix.isEmpty()) {
            boolean inserted = db.insertGroupSuffix(groupId, suffix, finalServer);
            if (!inserted)
                return new Response(false, "Could not add group suffix. Check console for errors.");
        }

        loadGroups(true);
        notifyReloadGroups();
        return new Response(true, "Group suffix set.");
    }

    public Response setGroupLadderBase(final int groupId, final String ladder) {
        String tempLadder = ladder;
        if (ladder == null || ladder.isEmpty())
            tempLadder = "default";

        Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        boolean success = db.setGroupLadder(groupId, tempLadder);
        if (success) {
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Group ladder set.");
        } else
            return new Response(false, "Could not set group ladder. Check console for errors.");
    }

    public Response setGroupRankBase(final int groupId, final int rank) {
        Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        boolean success = db.setGroupRank(groupId, rank);
        if (success) {
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Group rank set.");
        } else
            return new Response(false, "Could not set group rank. Check console for errors.");
    }

    public Response setGroupNameBase(final int groupId, final String name) {
        Group group = getGroup(groupId);
        if (group == null)
            return new Response(false, "Group does not exist.");

        boolean success = db.setGroupName(groupId, name);
        if (success) {
            loadGroups(true);
            notifyReloadGroups();
            return new Response(true, "Group name set.");
        } else
            return new Response(false, "Could not set group name. Check console for errors.");
    }

}
