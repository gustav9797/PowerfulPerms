package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.github.cheesesoftware.PowerfulPerms.database.DBResult;
import com.github.cheesesoftware.PowerfulPerms.database.DBRunnable;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ServerMode;
import com.google.common.base.Charsets;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public abstract class PermissionManagerBase implements PermissionManager {
    protected HashMap<UUID, PermissionPlayer> players = new HashMap<UUID, PermissionPlayer>();
    protected ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();
    protected HashMap<Integer, Group> groups = new HashMap<Integer, Group>();

    protected JedisPool pool;
    protected JedisPubSub subscriber;

    private final Database db;
    protected PowerfulPermsPlugin plugin;
    protected int checkTimedTaskId = -1;

    protected LinkedHashMap<String, List<CachedGroup>> defaultGroups;

    public static boolean redis;
    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public static String serverName;
    public static String consolePrefix = "[PowerfulPerms] ";
    public static String pluginPrefixShort = ChatColor.BLUE + "PP" + ChatColor.WHITE + "> ";
    public static String redisMessage = "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.";

    public PermissionManagerBase(Database database, PowerfulPermsPlugin plugin, String serverName) {
        this.db = database;
        this.plugin = plugin;

        PermissionManagerBase.serverName = serverName;

        final PowerfulPermsPlugin tempPlugin = plugin;

        db.applyPatches();

        if (!db.tableExists(Database.tblGroupSuffixes)) {
            db.createTables();

            this.createGroup("Guest", "default", 100, new ResponseRunnable() {

                @Override
                public void run() {

                }
            });
            this.createPlayer("[default]", DefaultPermissionPlayer.getUUID(), new ResponseRunnable() {

                @Override
                public void run() {

                }
            });

            tempPlugin.getLogger().info("Created tables.");
        }

        // Initialize Redis
        if (redis_password == null || redis_password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0, redis_password);

        checkTimedTaskId = this.getScheduler().runRepeating(new Runnable() {

            @Override
            public void run() {
                final List<Group> tempGroups = new ArrayList<Group>(groups.values());
                final HashMap<UUID, PermissionPlayer> tempPlayers = new HashMap<UUID, PermissionPlayer>(players);

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
                if (cachedGroup.willExpire() && cachedGroup.getExpirationDate().before(new Date())) {
                    // Date passed
                    debug("CachedGroup " + cachedGroup.getId() + " in player " + uuid.toString() + " expired.");
                    this.removePlayerGroup(uuid, cachedGroup.getId(), e.getKey(), cachedGroup.isNegated(), cachedGroup.getExpirationDate(), new ResponseRunnable() {

                        @Override
                        public void run() {
                            if (success)
                                debug("Group " + cachedGroup.getId() + " in player " + uuid.toString() + " expired, now removed.");
                            else
                                plugin.getLogger().warning("Could not remove expired player group. " + response);
                        }
                    });
                }
            }
        }

        List<Permission> permissions = player.getPermissions();
        for (final Permission p : permissions) {
            if (p.willExpire() && p.getExpirationDate().before(new Date())) {
                // Permission expired
                debug("Permission " + p.getId() + " in player " + uuid.toString() + " expired.");
                this.removePlayerPermission(uuid, p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate(), new ResponseRunnable() {

                    @Override
                    public void run() {
                        if (success)
                            debug("Permission with DB ID " + p.getId() + " in player " + uuid.toString() + " expired, now removed.");
                        else
                            plugin.getLogger().warning("Could not remove expired player permission. " + response);
                    }
                });
            }
        }
    }

    protected void checkGroupTimedPermissions(final Group group) {
        List<Permission> permissions = group.getOwnPermissions();
        for (final Permission p : permissions) {
            if (p.willExpire() && p.getExpirationDate().before(new Date())) {
                // Permission expired
                debug("Permission " + p.getId() + " in group " + group.getId() + " expired.");
                this.removeGroupPermission(group.getId(), p.getPermissionString(), p.getWorld(), p.getServer(), p.getExpirationDate(), new ResponseRunnable() {

                    @Override
                    public void run() {
                        if (success)
                            debug("Permission " + p.getId() + " in group " + group.getId() + " expired, now removed.");
                        else
                            plugin.getLogger().warning("Could not remove expired group permission. " + response);
                    }
                });
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

    @Override
    public IScheduler getScheduler() {
        return db.scheduler;
    }

    @Override
    public void createPlayer(final String name, final UUID uuid, final ResponseRunnable response) {
        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                loadPlayer(uuid, name, true);
                response.setResponse(true, "Player created.");
                db.scheduler.runSync(response);
            }
        }, false);

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
        ArrayList<UUID> uuids = new ArrayList<UUID>(players.keySet());
        for (UUID uuid : uuids) {
            if (!plugin.isPlayerOnline(uuid)) {
                players.remove(uuid);
            }
            debug("Reloading player " + uuid.toString());
            loadPlayer(uuid, null, false);
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
        return players.get(uuid);
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    @Override
    public PermissionPlayer getPermissionPlayer(String name) {
        UUID uuid = plugin.getPlayerUUID(name);
        return players.get(uuid);
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
                            if (plugin.isPlayerOnline(uuid) && players.containsKey(uuid)) {
                                PermissionPlayerBase toUpdate = (PermissionPlayerBase) players.get(uuid);
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

        if (players.containsKey(uuid)) {
            players.remove(uuid);
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
        if (groups != null)
            groups.clear();
        if (players != null)
            players.clear();
        if (cachedPlayers != null)
            cachedPlayers.clear();
    }

    @Override
    public void reloadGroups() {
        groups.clear();
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
        groups.clear();

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
                                                            while (groupResult.hasNext()) {
                                                                DBDocument row = groupResult.next();
                                                                final int groupId = row.getInt("id");
                                                                final String name = row.getString("name");
                                                                String ladder1 = row.getString("ladder");
                                                                final String ladder = (ladder1 == null || ladder1.isEmpty() ? "default" : ladder1);
                                                                final int rank = row.getInt("rank");

                                                                PowerfulGroup group = new PowerfulGroup(groupId, name, permissions.get(groupId), prefixes.get(groupId), suffixes.get(groupId), ladder,
                                                                        rank, plugin);
                                                                group.setParents(parents.get(groupId));
                                                                groups.put(groupId, group);
                                                            }

                                                            // Reload players too.
                                                            reloadDefaultPlayers(beginSameThread);
                                                            debug("loadGroups end");
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
        for (Map.Entry<Integer, Group> e : groups.entrySet())
            if (e.getValue().getName().equalsIgnoreCase(groupName))
                return e.getValue();
        return null;
    }

    @Override
    public Group getGroup(int id) {
        return groups.get(id);
    }

    @Override
    public Map<Integer, Group> getGroups() {
        return new HashMap<Integer, Group>(this.groups);
    }

    @Override
    public void getPlayerOwnGroups(UUID uuid, final ResultRunnable<LinkedHashMap<String, List<CachedGroup>>> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
        if (gp != null && !gp.isDefault()) {
            resultRunnable.setResult(gp.getCachedGroups());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        this.loadPlayerGroups(uuid, resultRunnable);
    }

    @Override
    public void getPlayerCurrentGroups(UUID uuid, final ResultRunnable<LinkedHashMap<String, List<CachedGroup>>> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getCachedGroups());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        this.loadPlayerGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public void run() {
                if (result != null) {
                    if (result.isEmpty()) {
                        result.putAll(deepCopyDefaultGroups());
                    }
                }
                resultRunnable.setResult(result);
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    @Override
    public void isPlayerDefault(UUID uuid, final ResultRunnable<Boolean> resultRunnable) {
        PermissionPlayer permissionPlayer = (PermissionPlayer) players.get(uuid);
        if (permissionPlayer != null) {
            resultRunnable.setResult(permissionPlayer.isDefault());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        getPlayerOwnGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public void run() {
                if (result == null || result.isEmpty())
                    resultRunnable.setResult(true);
                else
                    resultRunnable.setResult(false);
                db.scheduler.runSync(resultRunnable);
            }
        });
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
    public void getPlayerOwnPermissions(final UUID uuid, final ResultRunnable<List<Permission>> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getPermissions());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        loadPlayerOwnPermissions(uuid, new ResultRunnable<List<Permission>>() {

            @Override
            public void run() {
                final List<Permission> perms;
                if (result != null)
                    perms = result;
                else
                    perms = new ArrayList<Permission>();
                resultRunnable.setResult(perms);
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    protected void loadPlayerOwnPermissions(UUID uuid, final ResultRunnable<List<Permission>> resultRunnable) {
        db.getPlayerPermissions(uuid, new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    ArrayList<Permission> perms = new ArrayList<Permission>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();
                        Permission tempPerm = new PowerfulPermission(row.getInt("id"), row.getString("permission"), row.getString("world"), row.getString("server"), row.getDate("expires"));
                        perms.add(tempPerm);
                    }
                    resultRunnable.setResult(perms);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load player permissions.");
            }
        });
    }

    protected void loadPlayerGroups(UUID uuid, final ResultRunnable<LinkedHashMap<String, List<CachedGroup>>> resultRunnable) {
        db.getPlayerGroups(uuid, new DBRunnable(resultRunnable.isSameThread()) {

            @Override
            public void run() {
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
                        serverGroups.add(new CachedGroup(row.getInt("id"), group, row.getBoolean("negated"), row.getDate("expires")));
                        localGroups.put(row.getString("server"), serverGroups);
                    }
                    resultRunnable.setResult(localGroups);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load player groups.");
            }
        });
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

    protected void setPlayerGroups(final UUID uuid, final Map<String, List<CachedGroup>> newGroups, final ResponseRunnable response) {
        this.loadPlayerGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(response.isSameThread()) {

            @Override
            public void run() {
                final LinkedHashMap<String, List<CachedGroup>> currentGroups = result;
                db.scheduler.runAsync(new Runnable() {

                    @Override
                    public void run() {
                        // Start by checking groups that are missing
                        for (Entry<String, List<CachedGroup>> e : newGroups.entrySet()) {
                            String server = e.getKey();
                            List<CachedGroup> currentServerGroups = currentGroups.get(server);
                            if (currentServerGroups == null)
                                currentServerGroups = new ArrayList<CachedGroup>();
                            for (final CachedGroup newGroup : e.getValue()) {
                                boolean found = false;
                                for (CachedGroup currentGroup : currentServerGroups) {
                                    if (newGroup.getId() == currentGroup.getId()) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    // Couldn't find group, add it
                                    debug("adding group " + newGroup.getGroup().getId());
                                    db.addPlayerGroup(uuid, newGroup.getGroup().getId(), server, newGroup.isNegated(), newGroup.getExpirationDate(), new DBRunnable(true) {

                                        @Override
                                        public void run() {
                                            debug("added group " + newGroup.getGroup().getId());
                                            if (!result.booleanValue())
                                                response.setResponse(false, response.getResponse() + " Could not add group with ID " + newGroup.getGroup().getId() + ".");
                                        }
                                    });
                                }
                            }
                        }

                        // Check for groups that should be removed
                        for (Entry<String, List<CachedGroup>> oldEntrySet : currentGroups.entrySet()) {
                            String server = oldEntrySet.getKey();
                            List<CachedGroup> newServerGroups = newGroups.get(server);
                            if (newServerGroups == null)
                                newServerGroups = new ArrayList<CachedGroup>();
                            for (final CachedGroup oldGroup : oldEntrySet.getValue()) {
                                boolean found = false;
                                for (CachedGroup newGroup : newServerGroups) {
                                    if (newGroup.getId() == oldGroup.getId()) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    // Couldn't find the old group in new groups, remove it

                                    debug("deleting group " + oldGroup.getGroup().getId());
                                    db.deletePlayerGroup(uuid, oldGroup.getGroup().getId(), server, oldGroup.isNegated(), oldGroup.getExpirationDate(), new DBRunnable(true) {

                                        @Override
                                        public void run() {
                                            debug("deleted group " + oldGroup.getGroup().getId());
                                            if (!result.booleanValue())
                                                response.setResponse(false, response.getResponse() + " Could not delete group with ID " + oldGroup.getGroup().getId() + ".");
                                        }
                                    });
                                }
                            }
                        }

                        // Done changing groups
                        if (response.getResponse().isEmpty())
                            response.setResponse(true, "Player groups set.");
                        db.scheduler.runSync(response);
                    }
                }, response.isSameThread());
            }
        });
    }

    @Override
    public void getPlayerPrefix(UUID uuid, final ResultRunnable<String> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
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
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
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
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
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
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
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
    public void addPlayerPermission(UUID uuid, final String playerName, String permission, ResponseRunnable response) {
        addPlayerPermission(uuid, permission, playerName, "", "", null, response);
    }

    @Override
    public void addPlayerPermission(final UUID uuid, final String playerName, final String permission, final String world, final String server, final Date expires, final ResponseRunnable response) {
        if (playerName.equalsIgnoreCase("[default]")) {
            response.setResponse(false, "You can not add permissions to the default player. Add them to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }
        // Check if the same permission already exists.
        db.playerHasPermission(uuid, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(false, "Player already has the specified permission.");
                    db.scheduler.runSync(response, response.isSameThread());
                } else {
                    db.getPlayer(uuid, new DBRunnable(response.isSameThread()) {

                        @Override
                        public void run() {
                            if (result.hasNext()) {
                                final UUID uuid = UUID.fromString(result.next().getString("uuid"));
                                if (uuid != null) {
                                    db.insertPlayerPermission(uuid, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

                                        @Override
                                        public void run() {
                                            if (result.booleanValue()) {
                                                response.setResponse(true, "Permission added to player.");
                                                reloadPlayer(uuid);
                                                notifyReloadPlayer(uuid);
                                            } else
                                                response.setResponse(false, "Could not add permission. Check console for any errors.");
                                            db.scheduler.runSync(response, response.isSameThread());
                                        }
                                    });
                                } else {
                                    response.setResponse(false, "Could not add permission. Player's UUID is invalid.");
                                    db.scheduler.runSync(response, response.isSameThread());
                                }
                            } else {
                                response.setResponse(false, "Could not add permission. Player doesn't exist.");
                                db.scheduler.runSync(response, response.isSameThread());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void removePlayerPermission(UUID uuid, String permission, ResponseRunnable response) {
        removePlayerPermission(uuid, permission, "", "", null, response);
    }

    @Override
    public void removePlayerPermission(final UUID uuid, String permission, String world, String server, Date expires, final ResponseRunnable response) {
        db.deletePlayerPermission(uuid, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    response.setResponse(true, "Removed " + amount + " permissions from the player.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    response.setResponse(false, "Player does not have the specified permission.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void removePlayerPermissions(final UUID uuid, final ResponseRunnable response) {

        db.deletePlayerPermissions(uuid, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    int amount = result.rowsChanged();
                    response.setResponse(true, "Removed " + amount + " permissions from the player.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    response.setResponse(false, "Player does not have any permissions.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setPlayerPrefix(final UUID uuid, String prefix, final ResponseRunnable response) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            response.setResponse(false, "You can not set the prefix of the default player. Add it to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setPlayerPrefix(uuid, prefix, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Player prefix set.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    response.setResponse(false, "Could not set player prefix. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setPlayerSuffix(final UUID uuid, String suffix, final ResponseRunnable response) {
        if (uuid.equals(DefaultPermissionPlayer.getUUID())) {
            response.setResponse(false, "You can not set the suffix of the default player. Add it to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setPlayerSuffix(uuid, suffix, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Player suffix set.");
                    reloadPlayer(uuid);
                    notifyReloadPlayer(uuid);
                } else
                    response.setResponse(false, "Could not set player suffix. Check console for errors.");
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

        final String serv = server;

        final Group group = getGroup(groupId);
        if (group == null) {
            resp.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(resp, resp.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(resp.isSameThread()) {

            @Override
            public void run() {
                if (result != null) {

                    boolean removed = false;
                    Map<String, List<CachedGroup>> playerGroups = result;
                    List<CachedGroup> groupList = playerGroups.get(serv);
                    if (groupList == null)
                        groupList = new ArrayList<CachedGroup>();
                    Iterator<CachedGroup> it = groupList.iterator();
                    while (it.hasNext()) {
                        CachedGroup cachedGroup = it.next();
                        debug("Check " + cachedGroup.getGroup().getId());
                        debug("plus " + (expires == null) + " - " + (cachedGroup.getExpirationDate() == null));
                        debug("plus " + negated + " - " + cachedGroup.isNegated());
                        debug("plus " + groupId + " - " + cachedGroup.getGroup().getId());
                        if (CachedGroup.isSimilar(cachedGroup, groupId, negated, expires)) {
                            it.remove();
                            removed = true;
                        }
                    }

                    if (removed)
                        playerGroups.put(serv, groupList);
                    else {
                        resp.setResponse(false, "Player does not have the specified group for the specified server.");
                        db.scheduler.runSync(resp, resp.isSameThread());
                        return;
                    }

                    setPlayerGroups(uuid, playerGroups, new ResponseRunnable(resp.isSameThread()) {

                        @Override
                        public void run() {
                            if (success) {
                                resp.setResponse(true, "Player group removed.");
                                reloadPlayer(uuid);
                                notifyReloadPlayer(uuid);
                            } else
                                resp.setResponse(false, "Could not remove player group. Check console for errors.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                        }
                    });

                } else {
                    resp.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });
    }

    @Override
    public void addPlayerGroup(UUID uuid, int groupId, ResponseRunnable response) {
        addPlayerGroup(uuid, groupId, false, response);
    }

    @Override
    public void addPlayerGroup(UUID uuid, int groupId, final boolean negated, ResponseRunnable response) {
        addPlayerGroup(uuid, groupId, "", negated, null, response);
    }

    @Override
    public void addPlayerGroup(final UUID uuid, final int groupId, String server, final boolean negated, final Date expires, final ResponseRunnable resp) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        final String serv = server;

        final Group group = getGroup(groupId);
        if (group == null) {
            resp.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(resp, resp.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>(resp.isSameThread()) {

            @Override
            public void run() {
                if (result != null) {

                    Map<String, List<CachedGroup>> playerGroups = result;
                    List<CachedGroup> groupList = playerGroups.get(serv);
                    if (groupList == null)
                        groupList = new ArrayList<CachedGroup>();
                    Iterator<CachedGroup> it = groupList.iterator();
                    while (it.hasNext()) {
                        CachedGroup cachedGroup = it.next();
                        if (CachedGroup.isSimilar(cachedGroup, groupId, negated, expires)) {
                            resp.setResponse(false, "Player already has this group.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                            return;
                        }
                    }

                    groupList.add(new CachedGroup(-1, group, negated, expires));
                    playerGroups.put(serv, groupList);

                    setPlayerGroups(uuid, playerGroups, new ResponseRunnable(resp.isSameThread()) {

                        @Override
                        public void run() {
                            if (success) {
                                resp.setResponse(true, "Player group added.");
                                reloadPlayer(uuid);
                                notifyReloadPlayer(uuid);
                            } else
                                resp.setResponse(false, "Could not add player group. Check console for errors.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                        }
                    });

                } else {
                    resp.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });

    }

    @Override
    public void setPlayerRank(final UUID uuid, int groupId, final ResponseRunnable resp) {
        // Get player groups on specified ladder
        // Use the group type of the first one of those groups
        // replace old group with group "groupname"

        final Group group = getGroup(groupId);
        if (group == null) {
            resp.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(resp, resp.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public void run() {
                if (result != null) {
                    if (!result.isEmpty()) {
                        Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                        LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<String, List<CachedGroup>>();
                        boolean changed = false;
                        Group toUse = null;
                        while (it.hasNext()) {
                            Entry<String, List<CachedGroup>> next = it.next();
                            String server = next.getKey();
                            List<CachedGroup> playerCurrentGroups = next.getValue();

                            // Clone list to avoid making changes on online player
                            List<CachedGroup> clone = new ArrayList<CachedGroup>(playerCurrentGroups);

                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();
                            while (it2.hasNext()) {
                                CachedGroup current = it2.next();
                                if (current.getGroup().getLadder().equals(group.getLadder()) && current.getExpirationDate() == null) {
                                    if (toUse == null)
                                        toUse = current.getGroup();
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        clone.set(clone.indexOf(current), new CachedGroup(-1, group, current.isNegated(), null));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            resp.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                        } else {
                            setPlayerGroups(uuid, output, new ResponseRunnable(resp.isSameThread()) {

                                @Override
                                public void run() {
                                    if (success) {
                                        resp.setResponse(true, "Player rank set on ladder \"" + group.getLadder() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        resp.setResponse(false, "Could not set player rank. Check console for errors.");
                                    db.scheduler.runSync(resp, resp.isSameThread());
                                }
                            });
                        }
                    } else {
                        resp.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(resp, resp.isSameThread());
                    }
                } else {
                    resp.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });

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
    public void promotePlayer(final UUID uuid, final String ladder, final ResponseRunnable resp) {
        final List<Group> groupsClone = new ArrayList<Group>(groups.values());
        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public void run() {
                if (result != null) {
                    if (!result.isEmpty()) {
                        Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                        LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<String, List<CachedGroup>>();
                        boolean changed = false;
                        Group toUse = null;
                        Group toPromoteTo = null;
                        while (it.hasNext()) {
                            Entry<String, List<CachedGroup>> next = it.next();
                            String server = next.getKey();
                            List<CachedGroup> playerCurrentGroups = next.getValue();

                            // Clone list to avoid making changes on online player
                            List<CachedGroup> clone = new ArrayList<CachedGroup>(playerCurrentGroups);

                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();

                            while (it2.hasNext()) {
                                CachedGroup current = it2.next();
                                if (current.getGroup().getLadder().equals(ladder) && !current.willExpire() && !current.isNegated()) {
                                    if (toUse == null) {
                                        toUse = current.getGroup();
                                        toPromoteTo = getHigherGroup(toUse, groupsClone);
                                        if (toPromoteTo == null) {
                                            resp.setResponse(false, "There is no group on this ladder with a higher rank.");
                                            db.scheduler.runSync(resp, resp.isSameThread());
                                            return;
                                        }
                                    }
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        // This is the group to promote from
                                        clone.set(clone.indexOf(current), new CachedGroup(-1, toPromoteTo, current.isNegated(), null));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            resp.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                        } else {
                            final Group toPromoteToFinal = toPromoteTo;
                            setPlayerGroups(uuid, output, new ResponseRunnable(resp.isSameThread()) {

                                @Override
                                public void run() {
                                    if (success) {
                                        resp.setResponse(true, "Player was promoted to \"" + toPromoteToFinal.getName() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        resp.setResponse(false, "Could not promote player. Check console for errors.");
                                    db.scheduler.runSync(resp, resp.isSameThread());
                                }
                            });
                        }

                    } else {
                        resp.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(resp, resp.isSameThread());
                    }
                } else {
                    resp.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });
    }

    @Override
    public void demotePlayer(final UUID uuid, final String ladder, final ResponseRunnable resp) {
        final List<Group> groupsClone = new ArrayList<Group>(groups.values());
        getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

            @Override
            public void run() {
                if (result != null) {
                    if (!result.isEmpty()) {
                        Iterator<Entry<String, List<CachedGroup>>> it = result.entrySet().iterator();
                        LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<String, List<CachedGroup>>();
                        boolean changed = false;
                        Group toUse = null;
                        Group toDemoteTo = null;
                        while (it.hasNext()) {
                            Entry<String, List<CachedGroup>> next = it.next();
                            String server = next.getKey();
                            List<CachedGroup> playerCurrentGroups = next.getValue();

                            // Clone list to avoid making changes on online player
                            List<CachedGroup> clone = new ArrayList<CachedGroup>(playerCurrentGroups);

                            Iterator<CachedGroup> it2 = playerCurrentGroups.iterator();

                            while (it2.hasNext()) {
                                CachedGroup current = it2.next();
                                if (current.getGroup().getLadder().equals(ladder) && !current.willExpire() && !current.isNegated()) {
                                    if (toUse == null) {
                                        toUse = current.getGroup();
                                        toDemoteTo = getLowerGroup(toUse, groupsClone);
                                        if (toDemoteTo == null) {
                                            resp.setResponse(false, "There is no group on this ladder with a lower rank.");
                                            db.scheduler.runSync(resp, resp.isSameThread());
                                            return;
                                        }
                                    }
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        // This is the group to promote from
                                        clone.set(clone.indexOf(current), new CachedGroup(-1, toDemoteTo, current.isNegated(), null));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            resp.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(resp, resp.isSameThread());
                        } else {
                            final Group toDemoteToFinal = toDemoteTo;
                            setPlayerGroups(uuid, output, new ResponseRunnable(resp.isSameThread()) {

                                @Override
                                public void run() {
                                    if (success) {
                                        resp.setResponse(true, "Player was demoted to \"" + toDemoteToFinal.getName() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        resp.setResponse(false, "Could not demote player. Check console for errors.");
                                    db.scheduler.runSync(resp, resp.isSameThread());
                                }
                            });
                        }

                    } else {
                        resp.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(resp, resp.isSameThread());
                    }
                } else {
                    resp.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(resp, resp.isSameThread());
                }
            }
        });
    }

    // -------------------------------------------------------------------//
    // //
    // ------------GROUP PERMISSION MODIFYING FUNCTIONS BELOW-------------//
    // //
    // -------------------------------------------------------------------//

    @Override
    public void createGroup(String name, String ladder, int rank, final ResponseRunnable response) {
        Iterator<Entry<Integer, Group>> it = this.groups.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Group> e = it.next();
            if (e.getValue().getName().equalsIgnoreCase(name)) {
                // Group already exists
                response.setResponse(false, "Group already exists.");
                db.scheduler.runSync(response, response.isSameThread());
                return;
            }
        }

        db.insertGroup(name, ladder, rank, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Created group.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Group already exists.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void deleteGroup(int groupId, final ResponseRunnable response) {
        db.deleteGroup(groupId, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Deleted group.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Group does not exist.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void addGroupPermission(int groupId, String permission, ResponseRunnable response) {
        addGroupPermission(groupId, permission, "", "", null, response);
    }

    @Override
    public void addGroupPermission(int groupId, String permission, String world, String server, final Date expires, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            for (Permission temp : groupPermissions) {
                if (temp.getPermissionString().equals(permission) && temp.getServer().equals(server) && temp.getWorld().equals(world)
                        && (expires == null ? true : (expires.equals(temp.getExpirationDate())))) {
                    response.setResponse(false, "Group already has the specified permission.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }
            }

            db.insertGroupPermission(groupId, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

                @Override
                public void run() {
                    if (result.booleanValue()) {
                        response.setResponse(true, "Added permission to group.");
                        loadGroups(response.isSameThread());
                        notifyReloadGroups();
                    } else
                        response.setResponse(false, "Could not add permission to group. Check console for errors.");
                    db.scheduler.runSync(response, response.isSameThread());
                }
            });
        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void removeGroupPermission(int groupId, String permission, ResponseRunnable response) {
        removeGroupPermission(groupId, permission, "", "", null, response);
    }

    @Override
    public void removeGroupPermission(int groupId, String permission, String world, String server, Date expires, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group != null) {
            db.deleteGroupPermission(groupId, permission, world, server, expires, new DBRunnable(response.isSameThread()) {

                @Override
                public void run() {
                    if (result.booleanValue()) {
                        response.setResponse(true, "Removed " + result.rowsChanged() + " permissions from the group.");
                        loadGroups(response.isSameThread());
                        notifyReloadGroups();
                    } else
                        response.setResponse(false, "Group does not have the specified permission.");
                    db.scheduler.runSync(response, response.isSameThread());
                }
            });

        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void removeGroupPermissions(int groupId, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            if (groupPermissions.size() <= 0) {
                response.setResponse(false, "Group does not have any permissions.");
                db.scheduler.runSync(response, response.isSameThread());
                return;
            }

            final Counter counter = new Counter();
            db.deleteGroupPermissions(groupId, new DBRunnable(response.isSameThread()) {

                @Override
                public void run() {
                    counter.add(result.rowsChanged());
                    response.setResponse(true, "Removed " + counter.amount() + " permissions from the group.");
                    db.scheduler.runSync(response, response.isSameThread());
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                }
            });

        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void addGroupParent(int groupId, int parentGroupId, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupId);
            if (parentGroup != null) {
                if (group.getParents().contains(parentGroup)) {
                    response.setResponse(false, "Group already has the specified parent.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }

                db.addGroupParent(groupId, parentGroupId, new DBRunnable(response.isSameThread()) {

                    @Override
                    public void run() {
                        if (result.booleanValue()) {
                            response.setResponse(true, "Added parent to group.");
                            loadGroups(response.isSameThread());
                            notifyReloadGroups();
                        } else
                            response.setResponse(false, "Could not add parent to group. Check console for errors.");
                        db.scheduler.runSync(response, response.isSameThread());
                    }
                });

            } else {
                response.setResponse(false, "Parent group does not exist.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void removeGroupParent(int groupId, int parentGroupId, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupId);
            if (parentGroup != null) {
                if (!group.getParents().contains(parentGroup)) {
                    response.setResponse(false, "Group does not have the specified parent.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }

                db.deleteGroupParent(groupId, parentGroupId, new DBRunnable(response.isSameThread()) {

                    @Override
                    public void run() {
                        if (result.booleanValue()) {
                            response.setResponse(true, "Removed parent from group.");
                            loadGroups(response.isSameThread());
                            notifyReloadGroups();
                        } else
                            response.setResponse(false, "Could not remove parent from group. Check console for errors.");
                        db.scheduler.runSync(response, response.isSameThread());
                    }
                });

            } else {
                response.setResponse(false, "Parent group does not exist.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void setGroupPrefix(int groupId, String prefix, final ResponseRunnable response) {
        setGroupPrefix(groupId, prefix, "", response);
    }

    @Override
    public void setGroupPrefix(final int groupId, final String prefix, String server, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";
        server = server.toLowerCase();
        final String finalServer = server;

        final Group group = getGroup(groupId);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                // Run async while editing db
                HashMap<String, String> currentPrefix = group.getPrefixes();
                if (prefix.isEmpty() || currentPrefix.containsKey(finalServer)) {
                    db.deleteGroupPrefix(groupId, currentPrefix.get(finalServer), finalServer, new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (!result.booleanValue())
                                response.setResponse(false, "Could not delete group prefix. Check console for errors.");
                        }
                    });
                }
                if (!prefix.isEmpty()) {
                    db.addGroupPrefix(groupId, prefix, finalServer, new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (!result.booleanValue())
                                response.setResponse(false, "Could not add group prefix. Check console for errors.");
                        }
                    });
                }

                if (response.getResponse().isEmpty()) {
                    response.setResponse(true, "Group prefix set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                }
                db.scheduler.runSync(response, response.isSameThread());
            }
        }, response.isSameThread());

    }

    @Override
    public void setGroupSuffix(int groupId, String suffix, final ResponseRunnable response) {
        setGroupSuffix(groupId, suffix, "", response);
    }

    @Override
    public void setGroupSuffix(final int groupId, final String suffix, String server, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";
        server = server.toLowerCase();
        final String finalServer = server;

        final Group group = getGroup(groupId);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                // Run async while editing db
                HashMap<String, String> currentSuffix = group.getPrefixes();
                if (suffix.isEmpty() || currentSuffix.containsKey(finalServer)) {
                    db.deleteGroupSuffix(groupId, currentSuffix.get(finalServer), finalServer, new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (!result.booleanValue())
                                response.setResponse(false, "Could not delete group suffix. Check console for errors.");
                        }
                    });
                }
                if (!suffix.isEmpty()) {
                    db.addGroupSuffix(groupId, suffix, finalServer, new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (!result.booleanValue())
                                response.setResponse(false, "Could not add group suffix. Check console for errors.");
                        }
                    });
                }

                if (response.getResponse().isEmpty()) {
                    response.setResponse(true, "Group suffix set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                }
                db.scheduler.runSync(response, response.isSameThread());
            }
        }, response.isSameThread());

    }

    @Override
    public void setGroupLadder(int groupId, String ladder, final ResponseRunnable response) {
        if (ladder == null || ladder.isEmpty())
            ladder = "default";

        Group group = getGroup(groupId);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setGroupLadder(groupId, ladder, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Group ladder set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Could not set group ladder. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setGroupRank(int groupId, int rank, final ResponseRunnable response) {
        Group group = getGroup(groupId);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setGroupRank(groupId, rank, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Group rank set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Could not set group rank. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

}
