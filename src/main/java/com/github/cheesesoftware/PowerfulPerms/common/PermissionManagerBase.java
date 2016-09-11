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
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.github.cheesesoftware.PowerfulPerms.common.event.PowerfulEventHandler;
import com.github.cheesesoftware.PowerfulPerms.database.DBResult;
import com.github.cheesesoftware.PowerfulPerms.database.DBRunnable;
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
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerPermissionExpiredEvent;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ServerMode;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public abstract class PermissionManagerBase implements PermissionManager {

    protected HashMap<UUID, PermissionPlayer> players = new HashMap<UUID, PermissionPlayer>();
    protected ReentrantLock playersLock = new ReentrantLock();

    protected ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();

    protected HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    protected ReentrantLock groupsLock = new ReentrantLock();

    protected JedisPool pool;
    protected JedisPubSub subscriber;

    private final Database db;
    protected PowerfulPermsPlugin plugin;
    protected int checkTimedTaskId = -1;

    protected LinkedHashMap<String, List<CachedGroup>> defaultGroups;

    protected EventHandler eventHandler;

    public static boolean redis;
    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public static String serverName;
    public static String consolePrefix = "[PowerfulPerms] ";
    public static String pluginPrefixShort = ChatColor.BLUE + "PP" + ChatColor.WHITE + "> ";
    public static String redisMessage = "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.";

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10)); // TODO: Configurable

    public PermissionManagerBase(Database database, PowerfulPermsPlugin plugin, String serverName) {
        this.db = database;
        this.plugin = plugin;

        PermissionManagerBase.serverName = serverName;

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

            this.createGroup("Guest", "default", 100, new ResponseRunnable(true) {

                @Override
                public void run() {
                    tempPlugin.getLogger().info("Created group Guest");
                    setGroupPrefix(getGroup("Guest").getId(), "[Guest] ", new ResponseRunnable(true) {

                        @Override
                        public void run() {
                            tempPlugin.getLogger().info("Set group Guest prefix to \"[Guest] \"");
                        }
                    });
                    setGroupSuffix(getGroup("Guest").getId(), ": ", new ResponseRunnable(true) {

                        @Override
                        public void run() {
                            tempPlugin.getLogger().info("Set group Guest suffix to \": \"");
                        }
                    });
                }
            });
        }

        if (!db.tableExists(Database.tblPlayers)) {
            db.createTable(Database.tblPlayers);

            this.createPlayer("[default]", DefaultPermissionPlayer.getUUID(), new ResponseRunnable(true) {

                @Override
                public void run() {
                    tempPlugin.getLogger().info("Inserted player [default]");
                    Group guest = getGroup("Guest");
                    if (guest != null) {
                        addPlayerGroup(DefaultPermissionPlayer.getUUID(), guest.getId(), new ResponseRunnable(true) {

                            @Override
                            public void run() {
                                tempPlugin.getLogger().info("Added group Guest to player [default]");
                            }
                        });
                    }
                }
            });

        }

        // Initialize Redis
        if (redis) {
            if (redis_password == null || redis_password.isEmpty())
                pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0);
            else
                pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0, redis_password);
        }

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
                            debug("CachedGroup " + cachedGroup.getId() + " in player " + uuid.toString() + " expired.");
                            removePlayerGroup(uuid, cachedGroup.getGroupId(), server, cachedGroup.isNegated(), cachedGroup.getExpirationDate(), new ResponseRunnable() {

                                @Override
                                public void run() {
                                    if (success) {
                                        plugin.getLogger().info("Group " + cachedGroup.getGroupId() + " in player " + uuid.toString() + " expired and was removed.");
                                        getEventHandler().fireEvent(new PlayerGroupExpiredEvent(uuid, cachedGroup));
                                    } else
                                        debug("Could not remove expired player group. " + response);
                                }
                            });
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
                        debug("Permission " + p.getId() + " in player " + uuid.toString() + " expired.");
                        removePlayerPermission(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate(), new ResponseRunnable() {

                            @Override
                            public void run() {
                                if (success) {
                                    plugin.getLogger().info("Permission " + p.getId() + " in player " + uuid.toString() + " expired and was removed.");
                                    getEventHandler().fireEvent(new PlayerPermissionExpiredEvent(uuid, p));
                                } else
                                    debug("Could not remove expired player permission. " + response);
                            }
                        });
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
                        debug("Permission " + p.getId() + " in group " + group.getId() + " expired.");
                        removeGroupPermission(group.getId(), p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate(), new ResponseRunnable() {

                            @Override
                            public void run() {
                                if (success) {
                                    plugin.getLogger().info("Permission " + p.getId() + " in group " + group.getId() + " expired and was removed.");
                                    getEventHandler().fireEvent(new GroupPermissionExpiredEvent(group, p));
                                } else
                                    debug("Could not remove expired group permission. " + response);
                            }
                        });
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
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    @Override
    public void getConvertUUID(final String playerName, final ResultRunnable<UUID> resultRunnable) {
        if (playerName.equalsIgnoreCase("[default]") || playerName.equalsIgnoreCase("{default}")) {
            resultRunnable.setResult(DefaultPermissionPlayer.getUUID());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        // If player name is UUID, return it directly
        try {
            UUID uuid = UUID.fromString(playerName);
            resultRunnable.setResult(uuid);
            db.scheduler.runSync(resultRunnable);
            return;
        } catch (Exception e) {
        }

        // If player is online, get UUID directly
        if (plugin.isPlayerOnline(playerName)) {
            resultRunnable.setResult(plugin.getPlayerUUID(playerName));
            db.scheduler.runSync(resultRunnable);
            return;
        }

        // If player UUID exists in database, use that
        db.getPlayers(playerName, new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
                if (result.hasNext()) {
                    final DBDocument row = result.next();
                    if (row != null) {
                        String stringUUID = row.getString("uuid");
                        UUID uuid = UUID.fromString(stringUUID);
                        if (uuid != null) {
                            debug("UUID found in DB, skipping mojang api lookup");
                            resultRunnable.setResult(uuid);
                            db.scheduler.runSync(resultRunnable);
                            return;
                        }
                    }
                }

                // Check if DB contains online uuid. If so, return it.
                // Check if DB contains offline uuid. If so, return it. If not, return online uuid.
                if (plugin.getServerMode() == ServerMode.MIXED) {
                    // Generate offline UUID and check database if it exists. If so, return it.

                    db.scheduler.runAsync(new Runnable() {

                        @Override
                        public void run() {
                            final UUID offlineuuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
                            debug("Generated mixed mode offline UUID " + offlineuuid);

                            // Get online UUID.

                            debug("Begin UUID retrieval...");
                            ArrayList<String> list = new ArrayList<String>();
                            list.add(playerName);
                            UUIDFetcher fetcher = new UUIDFetcher(list);
                            try {
                                Map<String, UUID> result = fetcher.call();
                                if (result != null && result.containsKey(playerName)) {
                                    final UUID onlineuuid = result.get(playerName);
                                    debug("Retrieved UUID " + onlineuuid);
                                    // Check if database contains online UUID.

                                    db.getPlayer(onlineuuid, new DBRunnable() {
                                        @Override
                                        public void run() {
                                            if (result.hasNext()) {
                                                // Database contains online UUID. Return it.
                                                debug("online UUID found in DB");
                                                resultRunnable.setResult(onlineuuid);
                                                db.scheduler.runSync(resultRunnable);
                                            } else {
                                                // Could not find online UUID in database.
                                                // Check if offline UUID exists.
                                                debug("online UUID not found in DB");
                                                db.getPlayer(offlineuuid, new DBRunnable() {

                                                    @Override
                                                    public void run() {
                                                        if (result.hasNext()) {
                                                            // Found offline UUID in database. Return it.
                                                            debug("offline UUID found in DB, return offline");
                                                            resultRunnable.setResult(offlineuuid);
                                                        } else {
                                                            // Could not find neither of offline or online UUIDs in database.
                                                            // Online UUID exists for player name so return it.
                                                            debug("offline UUID not found in DB, return online");
                                                            resultRunnable.setResult(onlineuuid);
                                                        }
                                                        db.scheduler.runSync(resultRunnable);
                                                    }
                                                });
                                            }

                                        }
                                    });
                                } else {
                                    // Could not find online UUID for specified name
                                    debug("Did not find online UUID for player name " + playerName + ", return offline");
                                    resultRunnable.setResult(offlineuuid);
                                    db.scheduler.runSync(resultRunnable);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }, false);

                } else {
                    if (plugin.getServerMode() == ServerMode.ONLINE) {
                        // Convert player name to UUID using Mojang API
                        db.scheduler.runAsync(new Runnable() {

                            @Override
                            public void run() {
                                debug("Begin UUID retrieval...");
                                ArrayList<String> list = new ArrayList<String>();
                                list.add(playerName);
                                UUIDFetcher fetcher = new UUIDFetcher(list);
                                try {
                                    Map<String, UUID> result = fetcher.call();
                                    if (result != null && result.containsKey(playerName)) {
                                        UUID uuid = result.get(playerName);
                                        debug("Retrieved UUID " + uuid);
                                        resultRunnable.setResult(uuid);
                                    } else
                                        resultRunnable.setResult(null);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                db.scheduler.runSync(resultRunnable);
                            }
                        }, false);
                    } else {

                        // Generate UUID from player name
                        UUID uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
                        resultRunnable.setResult(uuid);

                        debug("Generated offline mode UUID " + uuid);
                        db.scheduler.runSync(resultRunnable);
                    }

                }
            }
        });

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
                loadPlayer(uuid, name, true);
                return new Response(true, "Player created.");
            }
        });
        return listenableFuture;
    }

    @Override
    public void notifyReloadGroups() {
        if (redis) {
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
                        plugin.getLogger().warning(redisMessage);
                    }
                }
            });
        }
    }

    @Override
    public void notifyReloadPlayers() {
        if (redis) {
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
                        plugin.getLogger().warning(redisMessage);
                    }
                }
            });
        }
    }

    @Override
    public void notifyReloadPlayer(final UUID uuid) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            notifyReloadPlayers();
        } else if (redis) {
            plugin.runTaskAsynchronously(new Runnable() {
                @SuppressWarnings("deprecation")
                public void run() {
                    try {
                        Jedis jedis = pool.getResource();
                        try {
                            jedis.publish("PowerfulPerms", uuid + " " + serverName);
                        } catch (Exception e) {
                            pool.returnBrokenResource(jedis);
                        }
                        pool.returnResource(jedis);
                    } catch (Exception e) {
                        plugin.getLogger().warning(redisMessage);
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
                loadPlayer(uuid, null, false);
            }
        } finally {
            playersLock.unlock();
        }
    }

    @Override
    public void reloadPlayer(UUID uuid) {
        if (plugin.isPlayerOnline(uuid)) {
            String name = plugin.getPlayerName(uuid);
            if (name != null) {
                this.loadPlayer(uuid, name, false);
            }
        } else if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            reloadDefaultPlayers(false);
        }
    }

    @Override
    public void reloadDefaultPlayers(final boolean samethread) {
        loadPlayerGroups(DefaultPermissionPlayer.getUUID(), new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(samethread) {

            @Override
            public void run() {
                if (result != null) {
                    defaultGroups = result;
                    reloadPlayers();
                    debug("DEFAULT PLAYER LOADED: " + (defaultGroups != null));
                } else
                    plugin.getLogger().severe(consolePrefix + "Can not get data from user [default].");
            }
        });
    }

    @Override
    public void reloadPlayer(String name) {
        if (plugin.isPlayerOnline(name)) {
            UUID uuid = plugin.getPlayerUUID(name);
            if (uuid != null) {
                this.loadPlayer(uuid, name, false);
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

    protected void setPermissionPlayer(UUID uuid, PermissionPlayer permissionPlayer) {
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

    protected void loadPlayer(final UUID uuid, final String name, final boolean login) {
        debug("loadPlayer begin");

        db.getPlayer(uuid, new DBRunnable(login) {

            @Override
            public void run() {
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
                                db.setPlayerName(uuid, name, new DBRunnable(login) {

                                    @Override
                                    public void run() {
                                        debug("PLAYER NAME UPDATED. NAMECHANGE");
                                        loadPlayerFinished(row, login, uuid);
                                    }
                                });
                            } else
                                loadPlayerFinished(row, login, uuid);
                        } else
                            loadPlayerFinished(row, login, uuid);
                    } else {
                        // Could not find player with UUID. Create new player.
                        db.insertPlayer(uuid, name, "", "", new DBRunnable(login) {

                            @Override
                            public void run() {
                                debug("NEW PLAYER CREATED");
                                loadPlayerFinished(null, login, uuid);
                            }
                        });
                    }

                }
            }
        });
    }

    protected void loadPlayerFinished(DBDocument row, final boolean login, final UUID uuid) {
        debug("loadPlayerFinished begin");
        final String prefix_loaded = (row != null ? row.getString("prefix") : "");
        final String suffix_loaded = (row != null ? row.getString("suffix") : "");

        this.loadPlayerGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(login) {

            @Override
            public void run() {
                final LinkedHashMap<String, List<CachedGroup>> tempGroups = result;
                loadPlayerOwnPermissions(uuid, new ResultRunnable<List<Permission>>(login) {

                    @Override
                    public void run() {
                        debug("loadPlayerFinished runnable begin");
                        List<Permission> perms;
                        if (result != null) {
                            perms = result;
                        } else
                            perms = new ArrayList<Permission>();

                        if (login) {
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

                                if (cachedPlayers.get(uuid) != null)
                                    cachedPlayers.remove(uuid);
                            }
                        }
                        debug("loadPlayerFinished runnable end");
                    }
                });

            }
        });

    }

    protected PermissionPlayerBase loadCachedPlayer(UUID uuid) {
        debug("continueLoadPlayer " + uuid);
        CachedPlayer cachedPlayer = cachedPlayers.get(uuid);
        if (cachedPlayer == null) {
            plugin.getLogger().severe(consolePrefix + "Could not continue load player. Cached player is null.");
            return null;
        }

        playersLock.lock();
        try {
            if (players.containsKey(uuid)) {
                players.remove(uuid);
            }
        } finally {
            playersLock.unlock();
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
        if (subscriber != null)
            subscriber.unsubscribe();
        if (pool != null)
            pool.destroy();
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
    protected void loadGroups() {
        loadGroups(false);
    }

    protected void loadGroups(boolean sameThread) {
        loadGroups(sameThread, sameThread);
    }

    /**
     * Loads groups from MySQL, removes old group data. Will reload all players too. beginSameThread: Set this to true if you want it to fetch group data on the same thread you call from. Set it to
     * false and it will run asynchronously. endSameThread: Set this to true if you want to finish and insert groups on the same thread. Note: This -MUST- be Bukkit main thread you execute on. Set to
     * false if you want to run it synchronously but scheduled.
     */
    protected void loadGroups(final boolean beginSameThread, final boolean endSameThread) {
        debug("loadGroups begin");

        db.getGroups(new DBRunnable(beginSameThread) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    final DBResult groupResult = result;

                    loadGroupParents(new ResultRunnable<HashMap<Integer, List<Integer>>>(beginSameThread) {

                        @Override
                        public void run() {
                            final HashMap<Integer, List<Integer>> parents = result;
                            debug("loadgroups parents size: " + parents.size());

                            loadGroupPrefixes(new ResultRunnable<HashMap<Integer, HashMap<String, String>>>(beginSameThread) {

                                @Override
                                public void run() {
                                    final HashMap<Integer, HashMap<String, String>> prefixes = result;

                                    loadGroupSuffixes(new ResultRunnable<HashMap<Integer, HashMap<String, String>>>(beginSameThread) {

                                        @Override
                                        public void run() {
                                            final HashMap<Integer, HashMap<String, String>> suffixes = result;

                                            loadGroupPermissions(new ResultRunnable<HashMap<Integer, List<PowerfulPermission>>>(beginSameThread) {

                                                @Override
                                                public void run() {
                                                    final HashMap<Integer, List<PowerfulPermission>> permissions = result;

                                                    // Make sure to run on Bukkit main thread when altering the groups
                                                    db.scheduler.runSync(new Runnable() {

                                                        @Override
                                                        public void run() {
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

                                                                    PowerfulGroup group = new PowerfulGroup(groupId, name, permissions.get(groupId), prefixes.get(groupId), suffixes.get(groupId),
                                                                            ladder, rank, plugin);
                                                                    group.setParents(parents.get(groupId));
                                                                    groups.put(groupId, group);

                                                                    checkGroupTimedPermissions(group);
                                                                }

                                                                // Reload players too.
                                                                reloadDefaultPlayers(beginSameThread);
                                                                debug("loadGroups end");
                                                            } finally {
                                                                groupsLock.unlock();
                                                            }

                                                        }
                                                    }, endSameThread);

                                                }
                                            });

                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });
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
                this.loadPlayerGroups(uuid);
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
                    resultRunnable.setResult(true);
                else
                    resultRunnable.setResult(false);
                db.scheduler.runSync(resultRunnable);
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
    public void getPlayerData(UUID uuid, final ResultRunnable<DBDocument> resultRunnable) {
        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    resultRunnable.setResult(row);
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
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

    protected void loadGroupParents(final ResultRunnable<HashMap<Integer, List<Integer>>> resultRunnable) {
        db.getGroupParents(new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
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
                    resultRunnable.setResult(parents);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load group parents.");
            }
        });
    }

    protected void loadGroupPrefixes(final ResultRunnable<HashMap<Integer, HashMap<String, String>>> resultRunnable) {
        db.getGroupPrefixes(new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
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
                    resultRunnable.setResult(prefixes);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load group prefixes.");
            }
        });
    }

    protected void loadGroupSuffixes(final ResultRunnable<HashMap<Integer, HashMap<String, String>>> resultRunnable) {
        db.getGroupSuffixes(new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
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
                    resultRunnable.setResult(suffixes);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load group suffixes.");
            }
        });
    }

    protected void loadGroupPermissions(final ResultRunnable<HashMap<Integer, List<PowerfulPermission>>> resultRunnable) {
        db.getGroupPermissions(new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
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
                    resultRunnable.setResult(permissions);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load group permissions.");
            }
        });
    }

    @Override
    public void getPlayerPrefix(UUID uuid, final ResultRunnable<String> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getPrefix());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        // TODO: this is player own prefix...
        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    resultRunnable.setResult(row.getString("prefix"));
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    @Override
    public void getPlayerSuffix(UUID uuid, final ResultRunnable<String> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getSuffix());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        // TODO: this is player own suffix...
        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    resultRunnable.setResult(row.getString("suffix"));
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    @Override
    public void getPlayerOwnPrefix(UUID uuid, final ResultRunnable<String> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getOwnPrefix());
            db.scheduler.runSync(resultRunnable);
            return;
        }
        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    resultRunnable.setResult(row.getString("prefix"));
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    @Override
    public void getPlayerOwnSuffix(UUID uuid, final ResultRunnable<String> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = getPermissionPlayer(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getOwnSuffix());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    resultRunnable.setResult(row.getString("suffix"));
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
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
    public void addPlayerPermission(UUID uuid, String permission, ResponseRunnable response) {
        addPlayerPermission(uuid, permission, "", "", null, response);
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
                                            boolean deleted = db.deletePlayerPermission(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate());
                                            if (!deleted)
                                                return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                                            else {
                                                boolean inserted = db.insertPlayerPermission(uuid, permission, world, server, newExpiry);
                                                if (!inserted) {
                                                    return new Response(false, "Could not update permission expiration date. Check console for any errors.");
                                                } else {
                                                    return new Response(true, "Permission expiration changed.");
                                                    reloadPlayer(uuid);
                                                    notifyReloadPlayer(uuid);
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                            boolean inserted = db.insertPlayerPermission(uuid, permission, world, server, expires);
                            if (inserted) {
                                return new Response(true, "Permission added to player.");
                                reloadPlayer(uuid);
                                notifyReloadPlayer(uuid);
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
    public void removePlayerPermission(UUID uuid, String permission, ResponseRunnable response) {
        removePlayerPermission(uuid, permission, "", "", null, response);
    }

    @Override
    public void removePlayerPermission(final UUID uuid, String permission, String world, String server, Date expires) {
        db.deletePlayerPermission(uuid, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    return new Response(true, "Removed " + amount + " permissions from the player.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    return new Response(false, "Player does not have the specified permission.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void removePlayerPermissions(final UUID uuid) {

        db.deletePlayerPermissions(uuid, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    return new Response(true, "Removed " + amount + " permissions from the player.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    return new Response(false, "Player does not have any permissions.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setPlayerPrefix(final UUID uuid, String prefix) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            return new Response(false, "You can not set the prefix of the default player. Add it to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setPlayerPrefix(uuid, prefix, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    return new Response(true, "Player prefix set.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    return new Response(false, "Could not set player prefix. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setPlayerSuffix(final UUID uuid, String suffix) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            return new Response(false, "You can not set the suffix of the default player. Add it to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setPlayerSuffix(uuid, suffix, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    return new Response(true, "Player suffix set.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    return new Response(false, "Could not set player suffix. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void removePlayerGroup(UUID uuid, int groupId, ResponseRunnable response) {
        removePlayerGroup(uuid, groupId, "", false, null, response);
    }

    @Override
    public void removePlayerGroup(UUID uuid, int groupId, boolean negated, ResponseRunnable response) {
        removePlayerGroup(uuid, groupId, "", negated, null, response);
    }

    @Override
    public void removePlayerGroup(final UUID uuid, final int groupId, String server, final boolean negated, final Date expires, final ResponseRunnable resp) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        final String serverFinal = server;

        final Group group = getGroup(groupId);
        if (group == null) {
            return new Response(false, "Group does not exist.");
            db.scheduler.runSync(resp, resp.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(resp.isSameThread()) {

            @Override
            public void run() {
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

                    if (!removed) {
                        return new Response(false, "Player does not have this group.");
                        db.scheduler.runSync(resp, resp.isSameThread());
                        return;
                    }

                    copyDefaultGroupsIfDefault(uuid, new ResponseRunnable(resp.isSameThread()) {

                        @Override
                        public void run() {
                            db.deletePlayerGroup(uuid, groupId, serverFinal, negated, expires, new DBRunnable(resp.isSameThread()) {

                                @Override
                                public void run() {
                                    if (result.booleanValue()) {
                                        return new Response(true, "Player group removed.");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        return new Response(false, "Could not remove player group. Check console for errors.");
                                    db.scheduler.runSync(resp, resp.isSameThread());
                                }
                            });
                        }
                    });

                } else {
                    return new Response(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });
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
                                    reloadPlayer(uuid);
                                    notifyReloadPlayer(uuid);
                                    return new Response(true, "Group expiration changed.");
                                }
                            }
                        } else if (CachedGroup.isSimilar(cachedGroup, groupId, negated, expires))
                            return new Response(false, "Player already has this group.");
                    }

                    ListenableFuture<Response> second = copyDefaultGroupsIfDefault(uuid);
                    if (!second.get().succeeded())
                        plugin.getLogger().severe(second.get().getResponse());
                    boolean inserted = db.insertPlayerGroup(uuid, groupId, serverFinal, negated, expires);
                    if (inserted) {
                        reloadPlayer(uuid);
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
                        if (!third.get().succeeded())
                            plugin.getLogger().severe(third.get().getResponse());

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
                            reloadPlayer(uuid);
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
                if (!third.get().succeeded())
                    plugin.getLogger().severe(third.get().getResponse());
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
                                if (currentGroup.getLadder().equals(ladder) && !current.willExpire() && !current.isNegated()) {
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
                            reloadPlayer(uuid);
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
                if (!third.get().succeeded())
                    plugin.getLogger().severe(third.get().getResponse());
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
                                if (currentGroup.getLadder().equals(ladder) && !current.willExpire() && !current.isNegated()) {
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
                            reloadPlayer(uuid);
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
                    loadGroups();
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
                if (deleted)
                    return new Response(true, "Deleted group.");
                else
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
                                            loadGroups();
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
                        loadGroups();
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
                        loadGroups();
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
                    loadGroups();
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
                            loadGroups();
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
                            loadGroups();
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

                loadGroups();
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

                loadGroups();
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
                    loadGroups();
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
                    loadGroups();
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
                    loadGroups();
                    notifyReloadGroups();
                    return new Response(true, "Group name set.");
                } else
                    return new Response(false, "Could not set group name. Check console for errors.");
            }
        });
        return listenableFuture;
    }

}
