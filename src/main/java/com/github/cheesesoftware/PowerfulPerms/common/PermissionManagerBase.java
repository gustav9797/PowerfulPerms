package com.github.cheesesoftware.PowerfulPerms.common;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.github.cheesesoftware.PowerfulPerms.Redis.RedisConnection;
import com.github.cheesesoftware.PowerfulPerms.common.event.PowerfulEventHandler;
import com.github.cheesesoftware.PowerfulPerms.database.DBResult;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.EventHandler;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.GroupPermissionExpiredEvent;
import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerGroupExpiredEvent;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerLoadedEvent;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerPermissionExpiredEvent;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.github.cheesesoftware.PowerfulPermsAPI.ServerMode;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import redis.clients.jedis.Jedis;

public abstract class PermissionManagerBase implements PermissionManager {

    protected HashMap<UUID, PermissionPlayer> players = new HashMap<UUID, PermissionPlayer>();
    protected ReentrantLock playersLock = new ReentrantLock();

    protected ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();

    protected HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
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

    // Can't use singlethreadexecutor because of nested executions
    protected ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()); // TODO: Configurable

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
                ListenableFuture<Response> first = this.createGroup("Guest", "default", 100);
                first.get();
                tempPlugin.getLogger().info("Created group Guest");
                ListenableFuture<Response> second = setGroupPrefix(getGroup("Guest").getId(), "[Guest] ");
                second.get();
                tempPlugin.getLogger().info("Set group Guest prefix to \"[Guest] \"");
                ListenableFuture<Response> third = setGroupSuffix(getGroup("Guest").getId(), ": ");
                third.get();
                tempPlugin.getLogger().info("Set group Guest suffix to \": \"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!db.tableExists(Database.tblPlayers)) {
            db.createTable(Database.tblPlayers);

            try {
                ListenableFuture<Response> first = createPlayer("[default]", DefaultPermissionPlayer.getUUID());
                first.get();
                tempPlugin.getLogger().info("Inserted player [default]");
                Group guest = getGroup("Guest");
                if (guest != null) {
                    ListenableFuture<Response> second = addPlayerGroup(DefaultPermissionPlayer.getUUID(), guest.getId());
                    second.get();
                    tempPlugin.getLogger().info("Added group Guest to player [default]");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (redisEnabled)
            this.redis = new RedisConnection(this, plugin, redis_ip, redis_port, redis_password);

        checkTimedTaskId = this.getScheduler().runRepeating(new Runnable() {

            @Override
            public void run() {
                List<Group> tempTempGroups;
                groupsLock.lock();
                try {
                    tempTempGroups = new ArrayList<Group>(groups.values());
                } finally {
                    groupsLock.unlock();
                }
                final List<Group> tempGroups = tempTempGroups;

                HashMap<UUID, PermissionPlayer> tempTempPlayers;
                playersLock.lock();
                try {
                    tempTempPlayers = new HashMap<UUID, PermissionPlayer>(players);
                } finally {
                    playersLock.unlock();
                }
                final HashMap<UUID, PermissionPlayer> tempPlayers = tempTempPlayers;

                getScheduler().runAsync(new Runnable() {

                    @Override
                    public void run() {
                        for (Entry<UUID, PermissionPlayer> e : tempPlayers.entrySet())
                            checkPlayerTimedGroupsAndPermissions(e.getKey(), e.getValue());
                        for (Group group : tempGroups)
                            checkGroupTimedPermissions(group);
                    }
                }, false);

            }

        }, 60);
    }

    protected void checkPlayerTimedGroupsAndPermissions(final UUID uuid, PermissionPlayer player) {
        LinkedHashMap<String, List<CachedGroup>> playerGroups = player.getCachedGroups();
        for (Entry<String, List<CachedGroup>> e : playerGroups.entrySet()) {
            for (final CachedGroup cachedGroup : e.getValue()) {
                if (cachedGroup.getExpireTaskId() == -1 && cachedGroup.willExpire()) {
                    final String server = e.getKey();
                    Runnable removePlayerGroupRunnable = new Runnable() {

                        @Override
                        public void run() {
                            try {
                                debug("CachedGroup " + cachedGroup.getId() + " in player " + uuid.toString() + " expired.");
                                ListenableFuture<Response> first = removePlayerGroup(uuid, cachedGroup.getGroupId(), server, cachedGroup.isNegated(), cachedGroup.getExpirationDate());
                                if (first.get().succeeded()) {
                                    plugin.getLogger().info("Group " + cachedGroup.getGroupId() + " in player " + uuid.toString() + " expired and was removed.");
                                    getEventHandler().fireEvent(new PlayerGroupExpiredEvent(uuid, cachedGroup));
                                } else
                                    debug("Could not remove expired player group. " + first.get().getResponse());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                Runnable removePlayerPermissionRunnable = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            debug("Permission " + p.getId() + " in player " + uuid.toString() + " expired.");
                            ListenableFuture<Response> first = removePlayerPermission(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                            if (first.get().succeeded()) {
                                plugin.getLogger().info("Permission " + p.getId() + " in player " + uuid.toString() + " expired and was removed.");
                                getEventHandler().fireEvent(new PlayerPermissionExpiredEvent(uuid, p));
                            } else
                                debug("Could not remove expired player permission. " + first.get().getResponse());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
                Runnable removeGroupPermissionRunnable = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            debug("Permission " + p.getId() + " in group " + group.getId() + " expired.");
                            ListenableFuture<Response> first = removeGroupPermission(group.getId(), p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                            if (first.get().succeeded()) {
                                plugin.getLogger().info("Permission " + p.getId() + " in group " + group.getId() + " expired and was removed.");
                                getEventHandler().fireEvent(new GroupPermissionExpiredEvent(group, p));
                            } else
                                debug("Could not remove expired group permission. " + first.get().getResponse());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
            LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<String, List<CachedGroup>>();
            LinkedHashMap<String, List<CachedGroup>> input = new LinkedHashMap<String, List<CachedGroup>>(defaultGroups);
            Iterator<Entry<String, List<CachedGroup>>> it = input.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, List<CachedGroup>> next = it.next();
                output.put(next.getKey(), new ArrayList<CachedGroup>(next.getValue()));
            }
            return output;
        }
        return null;
    }

    @Override
    public Jedis getRedisConnection() {
        return redis.getConnection();
    }

    @Override
    public ExecutorService getExecutor() {
        return service;
    }

    @Override
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    @Override
    public ListenableFuture<UUID> getConvertUUID(final String playerName) {
        ListenableFuture<UUID> listenableFuture = service.submit(new Callable<UUID>() {

            @Override
            public UUID call() throws Exception {
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
                    ArrayList<String> list = new ArrayList<String>();
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
                        ArrayList<String> list = new ArrayList<String>();
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
        });
        return listenableFuture;
    }

    @Override
    public IScheduler getScheduler() {
        return db.scheduler;
    }

    @Override
    public ListenableFuture<Response> createPlayer(final String name, final UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                loadPlayer(uuid, name, true, true);
                return new Response(true, "Player created.");
            }
        });
        return listenableFuture;
    }

    public Database getDatabase() {
        return db;
    }

    @Override
    public void notifyReloadGroups() {
        if (redisEnabled) {
            plugin.runTaskAsynchronously(new Runnable() {
                public void run() {
                    Jedis jedis = redis.getConnection();
                    if (jedis != null) {
                        jedis.publish("PowerfulPerms", "[groups]" + " " + serverId);
                        jedis.close();
                    }
                }
            });
        }
    }

    @Override
    public void notifyReloadPlayers() {
        if (redisEnabled) {
            plugin.runTaskAsynchronously(new Runnable() {
                public void run() {
                    Jedis jedis = redis.getConnection();
                    if (jedis != null) {
                        jedis.publish("PowerfulPerms", "[players]" + " " + serverId);
                        jedis.close();
                    }
                }
            });
        }
    }

    @Override
    public void notifyReloadPlayer(final UUID uuid) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            notifyReloadPlayers();
        } else if (redisEnabled) {
            plugin.runTaskAsynchronously(new Runnable() {
                public void run() {
                    Jedis jedis = redis.getConnection();
                    if (jedis != null) {
                        jedis.publish("PowerfulPerms", uuid + " " + serverId);
                        jedis.close();
                    }
                }
            });
        }
    }

    @Override
    public void reloadPlayers() {
        playersLock.lock();
        try {
            ArrayList<UUID> uuids = new ArrayList<UUID>(players.keySet());
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
    @Override
    public void reloadPlayer(UUID uuid) {
        reloadPlayer(uuid, false);
    }

    public void reloadPlayer(UUID uuid, boolean sameThread) {
        if (plugin.isPlayerOnline(uuid)) {
            String name = plugin.getPlayerName(uuid);
            if (name != null) {
                this.loadPlayer(uuid, name, false, sameThread);
            }
        } else if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            reloadDefaultPlayers(sameThread);
        }
    }

    @Override
    public void reloadDefaultPlayers(final boolean samethread) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> first = loadPlayerGroups(DefaultPermissionPlayer.getUUID());
        LinkedHashMap<String, List<CachedGroup>> result;
        try {
            result = first.get();
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

    @Override
    public void reloadPlayer(String name) {
        if (plugin.isPlayerOnline(name)) {
            UUID uuid = plugin.getPlayerUUID(name);
            if (uuid != null) {
                this.loadPlayer(uuid, name, false, false);
            }
        }
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    @Override
    public PermissionPlayer getPermissionPlayer(UUID uuid) {
        playersLock.lock();
        try {
            return players.get(uuid);
        } finally {
            playersLock.unlock();
        }
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

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    @Override
    public PermissionPlayer getPermissionPlayer(String name) {
        UUID uuid = plugin.getPlayerUUID(name);
        return getPermissionPlayer(uuid);
    }

    public void loadPlayer(final UUID uuid, final String name, final boolean storeCache, final boolean sameThread) {
        debug("loadPlayer begin");
        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
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
            }
        }, sameThread);
    }

    protected void loadPlayerFinished(final UUID uuid, final DBDocument row, final boolean storeCache, final boolean sameThread) {
        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                debug("loadPlayerFinished begin. storeCache: " + storeCache + " sameThread: " + sameThread);
                final String prefix_loaded = (row != null ? row.getString("prefix") : "");
                final String suffix_loaded = (row != null ? row.getString("suffix") : "");

                try {
                    ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> first = loadPlayerGroups(uuid);
                    LinkedHashMap<String, List<CachedGroup>> tempGroups = first.get();
                    ListenableFuture<List<Permission>> second = loadPlayerOwnPermissions(uuid);
                    List<Permission> perms = second.get();
                    if (perms == null)
                        perms = new ArrayList<Permission>();

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

    @Override
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

        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result = db.getGroups();
                if (result.booleanValue()) {
                    final DBResult groupResult = result;

                    try {
                        ListenableFuture<HashMap<Integer, List<Integer>>> first = loadGroupParents();
                        HashMap<Integer, List<Integer>> parents = first.get();
                        debug("loadgroups parents size: " + parents.size());

                        ListenableFuture<HashMap<Integer, HashMap<String, String>>> second = loadGroupPrefixes();
                        final HashMap<Integer, HashMap<String, String>> prefixes = second.get();

                        ListenableFuture<HashMap<Integer, HashMap<String, String>>> third = loadGroupSuffixes();
                        final HashMap<Integer, HashMap<String, String>> suffixes = third.get();

                        ListenableFuture<HashMap<Integer, List<PowerfulPermission>>> fourth = loadGroupPermissions();
                        final HashMap<Integer, List<PowerfulPermission>> permissions = fourth.get();

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
            }
        }, beginSameThread);

    }

    @Override
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

    @Override
    public Group getGroup(int id) {
        groupsLock.lock();
        try {
            return groups.get(id);
        } finally {
            groupsLock.unlock();
        }
    }

    @Override
    public Map<Integer, Group> getGroups() {
        groupsLock.lock();
        try {
            return new HashMap<Integer, Group>(this.groups);
        } finally {
            groupsLock.unlock();
        }
    }

    @Override
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerOwnGroups(final UUID uuid) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> listenableFuture = service.submit(new Callable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public LinkedHashMap<String, List<CachedGroup>> call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null && !gp.isDefault())
                    return gp.getCachedGroups();
                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> first = loadPlayerGroups(uuid);
                return first.get();
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerCurrentGroups(final UUID uuid) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> listenableFuture = service.submit(new Callable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public LinkedHashMap<String, List<CachedGroup>> call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null)
                    return gp.getCachedGroups();

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> future = loadPlayerGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = future.get();
                if (result != null) {
                    if (result.isEmpty()) {
                        result.putAll(deepCopyDefaultGroups());
                    }
                }
                return result;
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Group> getPlayerPrimaryGroup(UUID uuid) {
        ListenableFuture<Group> listenableFuture = service.submit(new Callable<Group>() {

            @Override
            public Group call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null)
                    return gp.getPrimaryGroup();

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> future = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = future.get();
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, result);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
                return PermissionPlayerBase.getPrimaryGroup(groups);
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Boolean> isPlayerDefault(final UUID uuid) {
        ListenableFuture<Boolean> listenableFuture = service.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                PermissionPlayer permissionPlayer = getPermissionPlayer(uuid);
                if (permissionPlayer != null)
                    return permissionPlayer.isDefault();

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerOwnGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = second.get();
                if (result == null || result.isEmpty())
                    return true;
                else
                    return false;
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<Response> copyDefaultGroups(final UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerOwnGroups(DefaultPermissionPlayer.getUUID());
                LinkedHashMap<String, List<CachedGroup>> result = second.get();
                if (result == null || result.isEmpty())
                    return new Response(false, "Could not retrieve the default groups.");
                else {
                    LinkedHashMap<String, List<CachedGroup>> defaultGroups = result;
                    Iterator<Entry<String, List<CachedGroup>>> it = defaultGroups.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<String, List<CachedGroup>> entry = it.next();
                        String server = entry.getKey();
                        Iterator<CachedGroup> it2 = entry.getValue().iterator();
                        while (it2.hasNext()) {
                            final CachedGroup toAdd = it2.next();
                            boolean inserted = db.insertPlayerGroup(uuid, toAdd.getGroupId(), server, toAdd.isNegated(), toAdd.getExpirationDate());
                            if (!inserted)
                                plugin.getLogger().severe("Could not copy default group " + toAdd.getGroupId() + " to player " + uuid.toString() + ".");
                        }
                    }
                    return new Response(true, "Default groups copied.");
                }
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<Response> copyDefaultGroupsIfDefault(final UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                ListenableFuture<Boolean> second = isPlayerDefault(uuid);
                if (second.get()) {
                    ListenableFuture<Response> third = copyDefaultGroups(uuid);
                    return third.get();
                } else
                    return new Response(false, "Player is not default.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<DBDocument> getPlayerData(final UUID uuid) {
        ListenableFuture<DBDocument> listenableFuture = service.submit(new Callable<DBDocument>() {

            @Override
            public DBDocument call() throws Exception {
                DBResult result = db.getPlayer(uuid);
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    return row;
                }
                return null;
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<List<Permission>> getPlayerOwnPermissions(final UUID uuid) {
        ListenableFuture<List<Permission>> listenableFuture = service.submit(new Callable<List<Permission>>() {

            @Override
            public List<Permission> call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null)
                    return gp.getPermissions();

                ListenableFuture<List<Permission>> future = loadPlayerOwnPermissions(uuid);
                List<Permission> perms = future.get();
                if (perms == null)
                    perms = new ArrayList<Permission>();
                return perms;
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<List<Permission>> loadPlayerOwnPermissions(final UUID uuid) {
        ListenableFuture<List<Permission>> listenableFuture = service.submit(new Callable<List<Permission>>() {

            @Override
            public List<Permission> call() throws Exception {
                DBResult result = db.getPlayerPermissions(uuid);
                if (result.booleanValue()) {
                    ArrayList<Permission> perms = new ArrayList<Permission>();
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
        });
        return listenableFuture;
    }

    protected ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> loadPlayerGroups(final UUID uuid) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> listenableFuture = service.submit(new Callable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public LinkedHashMap<String, List<CachedGroup>> call() throws Exception {
                DBResult result = db.getPlayerGroups(uuid);
                if (result.booleanValue()) {
                    LinkedHashMap<String, List<CachedGroup>> localGroups = new LinkedHashMap<String, List<CachedGroup>>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();
                        int groupId = row.getInt("groupid");
                        Group group = getGroup(groupId);
                        if (group == null) {
                            plugin.getLogger().warning("Could not load stored player group, group does not exist");
                            continue;
                        }

                        if (!localGroups.containsKey(row.getString("server")))
                            localGroups.put(row.getString("server"), new ArrayList<CachedGroup>());
                        List<CachedGroup> serverGroups = localGroups.get(row.getString("server"));
                        serverGroups.add(new CachedGroup(row.getInt("id"), groupId, row.getBoolean("negated"), row.getDate("expires")));
                        localGroups.put(row.getString("server"), serverGroups);
                    }
                    return localGroups;
                } else
                    plugin.getLogger().severe("Could not load player groups.");
                return null;
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<HashMap<Integer, List<Integer>>> loadGroupParents() {
        ListenableFuture<HashMap<Integer, List<Integer>>> listenableFuture = service.submit(new Callable<HashMap<Integer, List<Integer>>>() {

            @Override
            public HashMap<Integer, List<Integer>> call() throws Exception {
                DBResult result = db.getGroupParents();
                if (result.booleanValue()) {
                    HashMap<Integer, List<Integer>> parents = new HashMap<Integer, List<Integer>>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();

                        int groupId = row.getInt("groupid");
                        int parentId = row.getInt("parentgroupid");
                        if (!parents.containsKey(groupId))
                            parents.put(groupId, new ArrayList<Integer>());
                        List<Integer> localParents = parents.get(groupId);
                        localParents.add(parentId);
                        parents.put(groupId, localParents);
                    }
                    return parents;
                } else
                    plugin.getLogger().severe("Could not load group parents.");
                return null;
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<HashMap<Integer, HashMap<String, String>>> loadGroupPrefixes() {
        ListenableFuture<HashMap<Integer, HashMap<String, String>>> listenableFuture = service.submit(new Callable<HashMap<Integer, HashMap<String, String>>>() {

            @Override
            public HashMap<Integer, HashMap<String, String>> call() throws Exception {
                DBResult result = db.getGroupPrefixes();
                if (result.booleanValue()) {
                    HashMap<Integer, HashMap<String, String>> prefixes = new HashMap<Integer, HashMap<String, String>>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();

                        int groupId = row.getInt("groupid");
                        if (!prefixes.containsKey(groupId))
                            prefixes.put(groupId, new HashMap<String, String>());
                        HashMap<String, String> localPrefixes = prefixes.get(groupId);
                        localPrefixes.put(row.getString("server"), row.getString("prefix"));
                        prefixes.put(groupId, localPrefixes);
                    }
                    return prefixes;
                } else
                    plugin.getLogger().severe("Could not load group prefixes.");
                return null;
            }
        });
        return listenableFuture;
    }

    protected ListenableFuture<HashMap<Integer, HashMap<String, String>>> loadGroupSuffixes() {
        ListenableFuture<HashMap<Integer, HashMap<String, String>>> listenableFuture = service.submit(new Callable<HashMap<Integer, HashMap<String, String>>>() {

            @Override
            public HashMap<Integer, HashMap<String, String>> call() throws Exception {
                DBResult result = db.getGroupSuffixes();
                if (result.booleanValue()) {
                    HashMap<Integer, HashMap<String, String>> suffixes = new HashMap<Integer, HashMap<String, String>>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();

                        int groupId = row.getInt("groupid");
                        if (!suffixes.containsKey(groupId))
                            suffixes.put(groupId, new HashMap<String, String>());
                        HashMap<String, String> localSuffixes = suffixes.get(groupId);
                        localSuffixes.put(row.getString("server"), row.getString("suffix"));
                        suffixes.put(groupId, localSuffixes);
                    }
                    return suffixes;
                } else
                    plugin.getLogger().severe("Could not load group suffixes.");
                return null;
            }
        });
        return listenableFuture;

    }

    protected ListenableFuture<HashMap<Integer, List<PowerfulPermission>>> loadGroupPermissions() {
        ListenableFuture<HashMap<Integer, List<PowerfulPermission>>> listenableFuture = service.submit(new Callable<HashMap<Integer, List<PowerfulPermission>>>() {

            @Override
            public HashMap<Integer, List<PowerfulPermission>> call() throws Exception {
                DBResult result = db.getGroupPermissions();
                if (result.booleanValue()) {
                    HashMap<Integer, List<PowerfulPermission>> permissions = new HashMap<Integer, List<PowerfulPermission>>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();

                        int groupId = row.getInt("groupid");
                        if (!permissions.containsKey(groupId))
                            permissions.put(groupId, new ArrayList<PowerfulPermission>());
                        List<PowerfulPermission> localPermissions = permissions.get(groupId);
                        localPermissions.add(new PowerfulPermission(row.getInt("id"), row.getString("permission"), row.getString("world"), row.getString("server"), row.getDate("expires")));
                        permissions.put(groupId, localPermissions);
                    }
                    return permissions;
                } else
                    plugin.getLogger().severe("Could not load group permissions.");
                return null;
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<String> getPlayerPrefix(final UUID uuid, final String ladder) {
        ListenableFuture<String> first = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null) {
                    if (ladder != null)
                        return gp.getPrefix(ladder);
                    return gp.getPrefix();
                }

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, currentGroups);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
                String prefix = "";
                if (ladder != null)
                    prefix = PermissionPlayerBase.getPrefix(ladder, groups);
                else
                    prefix = PermissionPlayerBase.getPrefix(groups, getPlayerOwnPrefix(uuid).get());
                return prefix;
            }
        });
        return first;
    }

    @Override
    public ListenableFuture<String> getPlayerPrefix(final UUID uuid) {
        return getPlayerPrefix(uuid, null);
    }

    @Override
    public ListenableFuture<String> getPlayerSuffix(final UUID uuid, final String ladder) {
        ListenableFuture<String> listenableFuture = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                // If player is online, get data directly from player
                PermissionPlayer gp = getPermissionPlayer(uuid);
                if (gp != null) {
                    if (ladder != null)
                        return gp.getSuffix(ladder);
                    return gp.getSuffix();
                }

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(serverName, currentGroups);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
                String suffix = "";
                if (ladder != null)
                    suffix = PermissionPlayerBase.getSuffix(ladder, groups);
                else
                    suffix = PermissionPlayerBase.getSuffix(groups, getPlayerOwnSuffix(uuid).get());
                return suffix;
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<String> getPlayerSuffix(final UUID uuid) {
        return getPlayerSuffix(uuid, null);
    }

    @Override
    public ListenableFuture<String> getPlayerOwnPrefix(final UUID uuid) {
        ListenableFuture<String> listenableFuture = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<String> getPlayerOwnSuffix(final UUID uuid) {
        ListenableFuture<String> listenableFuture = service.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public String getGroupPrefix(int groupId, String server) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getPrefix(server);
        return null;
    }

    @Override
    public String getGroupSuffix(int groupId, String server) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getSuffix(server);
        return null;
    }

    @Override
    public HashMap<String, String> getGroupServerPrefix(int groupId) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getPrefixes();
        return null;
    }

    @Override
    public HashMap<String, String> getGroupServerSuffix(int groupId) {
        Group g = getGroup(groupId);
        if (g != null)
            return g.getSuffixes();
        return null;
    }

    // -------------------------------------------------------------------//
    // //
    // ------------PLAYER PERMISSION MODIFYING FUNCTIONS BELOW------------//
    // //
    // -------------------------------------------------------------------//

    @Override
    public ListenableFuture<Response> addPlayerPermission(UUID uuid, String permission) {
        return addPlayerPermission(uuid, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> addPlayerPermission(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
                        final UUID uuid = UUID.fromString(result.next().getString("uuid"));
                        if (uuid != null) {
                            if (expires != null) {
                                ListenableFuture<List<Permission>> future = getPlayerOwnPermissions(uuid);
                                for (Permission p : future.get()) {
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
                            return new Response(false, "Could not add permission. Player's UUID is invalid.");
                    } else
                        return new Response(false, "Could not add permission. Player doesn't exist.");
                }
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerPermission(UUID uuid, String permission) {
        return removePlayerPermission(uuid, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> removePlayerPermission(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                DBResult result = db.deletePlayerPermission(uuid, permission, world, server, expires);
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    reloadPlayer(uuid, true);
                    notifyReloadPlayer(uuid);
                    return new Response(true, "Removed " + amount + " permissions from the player.");
                } else
                    return new Response(false, "Player does not have the specified permission.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerPermissions(final UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {

                DBResult result = db.deletePlayerPermissions(uuid);
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    reloadPlayer(uuid, true);
                    notifyReloadPlayer(uuid);
                    return new Response(true, "Removed " + amount + " permissions from the player.");
                } else
                    return new Response(false, "Player does not have any permissions.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerPrefix(final UUID uuid, final String prefix) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerSuffix(final UUID uuid, final String suffix) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId) {
        return removePlayerGroup(uuid, groupId, "", false, null);
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId, boolean negated) {
        return removePlayerGroup(uuid, groupId, "", negated, null);
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                String tempServer = server;
                if (server.equalsIgnoreCase("all"))
                    tempServer = "";

                final String serverFinal = tempServer;

                final Group group = getGroup(groupId);
                if (group == null)
                    return new Response(false, "Group does not exist.");

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> first = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = first.get();
                if (result != null) {
                    boolean removed = false;
                    List<CachedGroup> groupList = result.get(serverFinal);
                    if (groupList == null)
                        groupList = new ArrayList<CachedGroup>();
                    Iterator<CachedGroup> it = groupList.iterator();
                    while (it.hasNext()) {
                        CachedGroup cachedGroup = it.next();
                        debug("Check " + cachedGroup.getGroupId());
                        debug("plus " + (expires == null) + " - " + (cachedGroup.getExpirationDate() == null));
                        debug("plus " + negated + " - " + cachedGroup.isNegated());
                        debug("plus " + groupId + " - " + cachedGroup.getGroupId());
                        if (CachedGroup.isSimilar(cachedGroup, groupId, negated, expires)) {
                            it.remove();
                            removed = true;
                        }
                    }

                    if (!removed)
                        return new Response(false, "Player does not have this group.");

                    ListenableFuture<Response> second = copyDefaultGroupsIfDefault(uuid);
                    second.get();
                    boolean deleted = db.deletePlayerGroup(uuid, groupId, serverFinal, negated, expires);
                    if (deleted) {
                        reloadPlayer(uuid, true);
                        notifyReloadPlayer(uuid);
                        return new Response(true, "Player group removed.");
                    } else
                        return new Response(false, "Could not remove player group. Check console for errors.");
                } else
                    return new Response(false, "Player does not exist.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId) {
        return addPlayerGroup(uuid, groupId, false);
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId, final boolean negated) {
        return addPlayerGroup(uuid, groupId, "", negated, null);
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                final Date now = new Date();
                String tempServer = server;
                if (server.equalsIgnoreCase("all"))
                    tempServer = "";

                final String serverFinal = tempServer;

                final Group group = getGroup(groupId);
                if (group == null)
                    return new Response(false, "Group does not exist.");

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> first = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = first.get();
                if (result != null) {
                    List<CachedGroup> groupList = result.get(serverFinal);
                    if (groupList == null)
                        groupList = new ArrayList<CachedGroup>();
                    final Iterator<CachedGroup> it = groupList.iterator();
                    while (it.hasNext()) {
                        CachedGroup cachedGroup = it.next();
                        if (cachedGroup.getGroupId() == groupId && cachedGroup.isNegated() == negated && cachedGroup.willExpire()) {
                            // Update expiration date instead
                            final Date newExpiry = new Date(cachedGroup.getExpirationDate().getTime() + (expires.getTime() - now.getTime()));
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
                        } else if (CachedGroup.isSimilar(cachedGroup, groupId, negated, expires))
                            return new Response(false, "Player already has this group.");
                    }

                    ListenableFuture<Response> second = copyDefaultGroupsIfDefault(uuid);
                    second.get();
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerRank(final UUID uuid, final int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                // Get player groups on specified ladder
                // Use the group type of the first one of those groups
                // replace old group with group "groupname"

                final Group group = getGroup(groupId);
                if (group == null)
                    return new Response(false, "Group does not exist.");

                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = second.get();
                if (result != null) {
                    if (!result.isEmpty()) {
                        ListenableFuture<Response> third = copyDefaultGroupsIfDefault(uuid);
                        third.get();

                        Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                        boolean changed = false;
                        Group toUse = null;
                        while (it.hasNext()) {
                            Entry<String, List<CachedGroup>> next = it.next();
                            final String server = next.getKey();
                            List<CachedGroup> playerCurrentGroups = next.getValue();
                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();
                            while (it2.hasNext()) {
                                final CachedGroup current = it2.next();
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
                                    }
                                }
                            }
                        }

                        if (!changed) {
                            return new Response(false, "Player has no groups on the specified ladder.");
                        } else {
                            reloadPlayer(uuid, true);
                            notifyReloadPlayer(uuid);
                            return new Response(true, "Player rank set on ladder \"" + group.getLadder() + "\".");
                        }
                    } else
                        return new Response(false, "Player has no groups.");
                } else
                    return new Response(false, "Player does not exist.");
            }
        });
        return listenableFuture;
    }

    public Group getHigherGroup(Group group, List<Group> groups) {
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>();
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

    public Group getLowerGroup(Group group, List<Group> groups) {
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>(Collections.reverseOrder());
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

    @Override
    public ListenableFuture<Response> promotePlayer(final UUID uuid, final String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                List<Group> tempGroupsClone;
                groupsLock.lock();
                try {
                    tempGroupsClone = new ArrayList<Group>(groups.values());
                } finally {
                    groupsLock.unlock();
                }
                final List<Group> groupsClone = tempGroupsClone;
                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = second.get();
                ListenableFuture<Response> third = copyDefaultGroupsIfDefault(uuid);
                third.get();
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
                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();

                            while (it2.hasNext()) {
                                final CachedGroup current = it2.next();
                                final Group currentGroup = getGroup(current.getGroupId());
                                if (currentGroup.getLadder().equalsIgnoreCase(ladder) && !current.willExpire() && !current.isNegated()) {
                                    if (toUse == null) {
                                        toUse = currentGroup;
                                        toPromoteTo = getHigherGroup(toUse, groupsClone);
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> demotePlayer(final UUID uuid, final String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                List<Group> tempGroupsClone;
                groupsLock.lock();
                try {
                    tempGroupsClone = new ArrayList<Group>(groups.values());
                } finally {
                    groupsLock.unlock();
                }
                final List<Group> groupsClone = tempGroupsClone;
                ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = getPlayerCurrentGroups(uuid);
                LinkedHashMap<String, List<CachedGroup>> result = second.get();
                ListenableFuture<Response> third = copyDefaultGroupsIfDefault(uuid);
                third.get();
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
                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();

                            while (it2.hasNext()) {
                                final CachedGroup current = it2.next();
                                final Group currentGroup = getGroup(current.getGroupId());
                                if (currentGroup.getLadder().equalsIgnoreCase(ladder) && !current.willExpire() && !current.isNegated()) {
                                    if (toUse == null) {
                                        toUse = currentGroup;
                                        toDemoteTo = getLowerGroup(toUse, groupsClone);
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
        });
        return listenableFuture;
    }

    // -------------------------------------------------------------------//
    // //
    // ------------GROUP PERMISSION MODIFYING FUNCTIONS BELOW-------------//
    // //
    // -------------------------------------------------------------------//

    @Override
    public ListenableFuture<Response> createGroup(final String name, final String ladder, final int rank) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                Map<Integer, Group> groupsClone;
                groupsLock.lock();
                try {
                    groupsClone = new HashMap<Integer, Group>(groups);
                } finally {
                    groupsLock.unlock();
                }
                Iterator<Entry<Integer, Group>> it = groupsClone.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Integer, Group> e = it.next();
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> deleteGroup(final int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {

                boolean deleted = db.deleteGroup(groupId);
                if (deleted) {
                    loadGroups(true);
                    notifyReloadGroups();
                    return new Response(true, "Deleted group.");
                } else
                    return new Response(false, "Group does not exist.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addGroupPermission(int groupId, String permission) {
        return addGroupPermission(groupId, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> addGroupPermission(final int groupId, final String permission, final String world, final String server, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupPermission(int groupId, String permission) {
        return removeGroupPermission(groupId, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> removeGroupPermission(final int groupId, final String permission, final String world, final String server, final Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                Group group = getGroup(groupId);
                if (group != null) {
                    DBResult result = db.deleteGroupPermission(groupId, permission, world, server, expires);
                    if (result.booleanValue()) {
                        loadGroups(true);
                        notifyReloadGroups();
                        return new Response(true, "Removed " + result.rowsChanged() + " permissions from the group.");
                    } else
                        return new Response(false, "Group does not have the specified permission.");
                } else
                    return new Response(false, "Group does not exist.");
            }
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupPermissions(final int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addGroupParent(final int groupId, final int parentGroupId) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupParent(final int groupId, final int parentGroupId) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupPrefix(int groupId, String prefix) {
        return setGroupPrefix(groupId, prefix, "");
    }

    @Override
    public ListenableFuture<Response> setGroupPrefix(final int groupId, final String prefix, final String server) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupSuffix(int groupId, String suffix) {
        return setGroupSuffix(groupId, suffix, "");
    }

    @Override
    public ListenableFuture<Response> setGroupSuffix(final int groupId, final String suffix, final String server) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupLadder(final int groupId, final String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupRank(final int groupId, final int rank) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupName(final int groupId, final String name) {
        ListenableFuture<Response> listenableFuture = service.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
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
        });
        return listenableFuture;
    }

}
