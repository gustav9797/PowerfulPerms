package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
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
    protected PermissionPlayerBase defaultPlayer;

    public static boolean redis;
    public static String redis_ip;
    public static int redis_port;
    public static String redis_password;

    public static String serverName;
    public static String consolePrefix = "[PowerfulPerms] ";
    public static String pluginPrefixShort = ChatColor.WHITE + "[" + ChatColor.BLUE + "PP" + ChatColor.WHITE + "] ";
    public static String redisMessage = "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.";

    public PermissionManagerBase(Database database, PowerfulPermsPlugin plugin, String serverName) {
        this.db = database;
        this.plugin = plugin;

        PermissionManagerBase.serverName = serverName;

        final PowerfulPermsPlugin tempPlugin = plugin;

        // Create table Groups, add group Guest
        db.tableExists(Database.tblGroups, new DBRunnable(true) {

            @Override
            public void run() {
                if (!result.booleanValue()) {
                    db.createGroupsTable(new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (result.booleanValue())
                                tempPlugin.getLogger().info("Created table \"" + Database.tblGroups + "\"");
                            else
                                tempPlugin.getLogger().info("Could not create table \"" + Database.tblGroups + "\"");
                        }
                    });
                }
            }
        });

        // Create table Players
        db.tableExists(Database.tblPlayers, new DBRunnable(true) {

            @Override
            public void run() {
                if (!result.booleanValue()) {
                    db.createPlayersTable(new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (result.booleanValue())
                                tempPlugin.getLogger().info("Created table \"" + Database.tblPlayers + "\"");
                            else
                                tempPlugin.getLogger().info("Could not create table \"" + Database.tblPlayers + "\"");
                        }
                    });
                }
            }
        });

        // Create table Permissions
        db.tableExists(Database.tblPermissions, new DBRunnable(true) {

            @Override
            public void run() {
                if (!result.booleanValue()) {
                    db.createPermissionsTable(new DBRunnable(true) {

                        @Override
                        public void run() {
                            if (result.booleanValue())
                                tempPlugin.getLogger().info("Created table \"" + Database.tblPermissions + "\"");
                            else
                                tempPlugin.getLogger().info("Could not create table \"" + Database.tblPermissions + "\"");
                        }
                    });
                }
            }
        });

        db.applyPatches(plugin);

        loadGroups(true, true);

        // Initialize Redis
        if (redis_password == null || redis_password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), redis_ip, redis_port, 0, redis_password);
    }

    protected void debug(String msg) {
        plugin.debug(msg);
    }

    @Override
    public void getConvertUUID(final String playerName, final ResultRunnable<UUID> resultRunnable) {
        if (playerName.equalsIgnoreCase("[default]")) {
            resultRunnable.setResult(DefaultPermissionPlayer.getUUID());
            db.scheduler.runSync(resultRunnable);
            return;
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
        for (UUID uuid : players.keySet()) {
            if (plugin.isPlayerOnline(uuid)) {
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
        final UUID defaultUUID = DefaultPermissionPlayer.getUUID();
        db.getPlayer(defaultUUID, new DBRunnable(samethread) {

            @Override
            public void run() {
                final DBDocument row = result.next();
                if (row != null) {

                    loadPlayerOwnPermissions(defaultUUID, new ResultRunnable<List<Permission>>(samethread) {

                        @Override
                        public void run() {
                            if (result != null) {
                                defaultPlayer = new PermissionPlayerBase(getPlayerGroups(row.getString("groups")), result, row.getString("prefix"), row.getString("suffix"), plugin, false);
                                reloadPlayers();
                                debug("DEFAULT PLAYER LOADED: " + (defaultPlayer != null));
                            } else
                                plugin.getLogger().severe(consolePrefix + "Can not get data from user [default]. 2");
                        }
                    });

                } else
                    plugin.getLogger().severe(consolePrefix + "Can not get data from user [default]. 1");
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
                                        db.updatePlayerPermissions(uuid, name, new DBRunnable(login) {

                                            @Override
                                            public void run() {
                                                debug("UPDATED PLAYER PERMISSIONS");
                                                loadPlayerFinished(row, login, uuid);
                                            }
                                        });
                                    }
                                });
                            } else
                                loadPlayerFinished(row, login, uuid);
                        } else
                            loadPlayerFinished(row, login, uuid);
                    } else {
                        // Could not find player with UUID. Create new player.
                        db.insertPlayer(uuid, name, "", "", "", new DBRunnable(login) {

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
        final String groups_loaded = (row != null ? row.getString("groups") : "");
        final String prefix_loaded = (row != null ? row.getString("prefix") : "");
        final String suffix_loaded = (row != null ? row.getString("suffix") : "");

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
                    cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));

                } else {
                    // Player should be reloaded if "login" is false. Reload already loaded player.
                    if (plugin.isPlayerOnline(uuid) && players.containsKey(uuid)) {
                        PermissionPlayerBase toUpdate = (PermissionPlayerBase) players.get(uuid);
                        PermissionPlayerBase base;
                        LinkedHashMap<String, List<CachedGroup>> tempGroups = getPlayerGroups(groups_loaded);
                        debug("loadPlayerFinished reload group count:" + tempGroups.size());
                        if (tempGroups.isEmpty()) {
                            // Player has no groups, put default data
                            base = new PermissionPlayerBase(defaultPlayer.getCachedGroups(), perms, prefix_loaded, suffix_loaded, plugin, true);
                        } else
                            base = new PermissionPlayerBase(tempGroups, perms, prefix_loaded, suffix_loaded, plugin, false);
                        toUpdate.update(base);

                        if (cachedPlayers.get(uuid) != null)
                            cachedPlayers.remove(uuid);
                    }
                }
                debug("loadPlayerFinished runnable end");
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
            base = new PermissionPlayerBase(defaultPlayer.getCachedGroups(), cachedPlayer.getPermissions(), cachedPlayer.getPrefix(), cachedPlayer.getSuffix(), plugin, true);
        } else
            base = new PermissionPlayerBase(this.getPlayerGroups(cachedPlayer.getGroups()), cachedPlayer.getPermissions(), cachedPlayer.getPrefix(), cachedPlayer.getSuffix(), plugin, false);
        cachedPlayers.remove(uuid);
        return base;
    }

    public void onDisable() {
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
                    HashMap<Integer, String> tempParents = new HashMap<Integer, String>();
                    while (result.hasNext()) {
                        DBDocument row = result.next();
                        final int groupId = row.getInt("id");
                        final String name = row.getString("name");
                        String parents = row.getString("parents");
                        final String prefix = row.getString("prefix");
                        final String suffix = row.getString("suffix");
                        String ladder1 = row.getString("ladder");
                        final String ladder = (ladder1 == null || ladder1.isEmpty() ? "default" : ladder1);
                        final int rank = row.getInt("rank");

                        tempParents.put(groupId, parents);

                        db.getGroupPermissions(name, new DBRunnable(true) {

                            @Override
                            public void run() {
                                PowerfulGroup group = new PowerfulGroup(groupId, name, loadGroupPermissions(result), prefix, suffix, ladder, rank);
                                groups.put(groupId, group);
                            }
                        });

                    }

                    final HashMap<Integer, String> tempParentsFinal = tempParents;

                    // Make sure to run on Bukkit main thread when altering the groups
                    db.scheduler.runSync(new Runnable() {

                        @Override
                        public void run() {
                            Iterator<Entry<Integer, String>> it = tempParentsFinal.entrySet().iterator();
                            while (it.hasNext()) {
                                Entry<Integer, String> e = it.next();
                                // debug("Adding parents to group with ID " + e.getKey());// + " and name " + groups.get(e.getKey()).getName());
                                ArrayList<Group> finalGroups = new ArrayList<Group>();
                                ArrayList<String> rawParents = getGroupParents(e.getValue());
                                for (String s : rawParents) {
                                    for (Group testGroup : groups.values()) {
                                        // debug("Comparing " + s + " with " + testGroup.getId());
                                        if (!s.isEmpty() && Integer.parseInt(s) == testGroup.getId()) {
                                            finalGroups.add(testGroup);
                                            // debug("Added parent ID " + testGroup.getId() + " to group with ID " + e.getKey());
                                            // debug("Added parent " + testGroup.getName() + " to " + groups.get(e.getKey()).getName());
                                            break;
                                        }
                                    }
                                }
                                Group temp = groups.get(e.getKey());
                                if (temp != null)
                                    temp.setParents(finalGroups);
                                else
                                    debug("Group with ID " + e.getKey() + " was null");
                            }

                            // Reload players too.
                            reloadDefaultPlayers(beginSameThread);
                            debug("loadGroups end");
                        }
                    }, endSameThread);
                }
            }
        });
    }

    protected ArrayList<PowerfulPermission> loadGroupPermissions(DBResult result) {
        ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
        while (result.hasNext()) {
            DBDocument row = result.next();
            PowerfulPermission tempPerm = new PowerfulPermission(row.getString("permission"), row.getString("world"), row.getString("server"));
            perms.add(tempPerm);
        }
        return perms;
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

    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, Group> getGroups() {
        return (Map<Integer, Group>) this.groups.clone();
    }

    protected LinkedHashMap<String, List<CachedGroup>> getPlayerGroups(String raw) {
        LinkedHashMap<String, List<CachedGroup>> tempGroups = new LinkedHashMap<String, List<CachedGroup>>();
        for (String s : raw.split(";")) {
            // Each group entry
            String[] split = s.split(":");
            if (split.length >= 2) {
                String server = split[0];

                // If list null, initialize list
                List<CachedGroup> input = tempGroups.get(server);
                if (input == null)
                    input = new ArrayList<CachedGroup>();

                boolean negated = split[1].startsWith("-");
                if (negated)
                    split[1] = split[1].substring(1);

                int groupId = Integer.parseInt(split[1]);

                Group group = groups.get(groupId);
                if (group != null) {
                    debug("add group:" + groupId + " negated:" + negated);
                    input.add(new CachedGroup(group, negated));
                    tempGroups.put(server, input);
                }
            } else {
                if (!s.isEmpty()) {
                    // If list null, initialize list
                    List<CachedGroup> input = tempGroups.get("");
                    if (input == null)
                        input = new ArrayList<CachedGroup>();

                    input.add(new CachedGroup(groups.get(Integer.parseInt(s)), false));
                    tempGroups.put("", input);
                    debug(s + " old ");
                }
            }
        }
        return tempGroups;
    }

    public static String getPlayerGroupsRawCached(Map<String, List<CachedGroup>> input) {
        String output = "";
        for (Entry<String, List<CachedGroup>> entry : input.entrySet()) {
            for (CachedGroup cachedGroup : entry.getValue()) {
                output += entry.getKey() + ":" + (cachedGroup.isNegated() ? "-" : "") + cachedGroup.getGroup().getId() + ":" /* old primary/secondary here */+ ";";
            }
        }
        return output;
    }

    @Override
    public void getPlayerGroups(UUID uuid, final ResultRunnable<Map<String, List<CachedGroup>>> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
        if (gp != null && !gp.isDefault()) {
            resultRunnable.setResult(gp.getCachedGroups());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    LinkedHashMap<String, List<CachedGroup>> output = getPlayerGroups(row.getString("groups"));
                    resultRunnable.setResult(output);
                }
                db.scheduler.runSync(resultRunnable);
            }
        });
    }

    @Override
    public void getPlayerCurrentGroups(UUID uuid, final ResultRunnable<Map<String, List<CachedGroup>>> resultRunnable) {
        // If player is online, get data directly from player
        PermissionPlayer gp = (PermissionPlayer) players.get(uuid);
        if (gp != null) {
            resultRunnable.setResult(gp.getCachedGroups());
            db.scheduler.runSync(resultRunnable);
            return;
        }

        db.getPlayer(uuid, new DBRunnable() {

            @Override
            public void run() {
                if (result.hasNext()) {
                    DBDocument row = result.next();
                    LinkedHashMap<String, List<CachedGroup>> output = getPlayerGroups(row.getString("groups"));
                    if (output.isEmpty()) {
                        output.putAll(defaultPlayer.getCachedGroups());
                    }
                    resultRunnable.setResult(output);
                }
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

        getPlayerGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                        Permission tempPerm = new PowerfulPermission(row.getString("permission"), row.getString("world"), row.getString("server"));
                        perms.add(tempPerm);
                    }
                    resultRunnable.setResult(perms);
                    db.scheduler.runSync(resultRunnable, resultRunnable.isSameThread());
                } else
                    plugin.getLogger().severe("Could not load player permissions.");
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
    public String getGroupPrefix(String groupName, String server) {
        Group g = getGroup(groupName);
        if (g != null)
            return g.getPrefix(server);
        return null;
    }

    @Override
    public String getGroupSuffix(String groupName, String server) {
        Group g = getGroup(groupName);
        if (g != null)
            return g.getSuffix(server);
        return null;
    }

    @Override
    public HashMap<String, String> getGroupServerPrefix(String groupName) {
        Group g = getGroup(groupName);
        if (g != null)
            return g.getPrefixes();
        return null;
    }

    @Override
    public HashMap<String, String> getGroupServerSuffix(String groupName) {
        Group g = getGroup(groupName);
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
        addPlayerPermission(uuid, permission, playerName, "", "", response);
    }

    @Override
    public void addPlayerPermission(final UUID uuid, final String playerName, final String permission, final String world, final String server, final ResponseRunnable response) {
        if (playerName.equalsIgnoreCase("[default]")) {
            response.setResponse(false, "You can not add permissions to the default player. Add them to a group instead and add the group to the default player.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }
        // Check if the same permission already exists.
        db.playerHasPermission(uuid, permission, world, server, new DBRunnable(response.isSameThread()) {

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
                                    db.insertPermission(uuid, playerName, "", permission, world, server, new DBRunnable(response.isSameThread()) {

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
        removePlayerPermission(uuid, permission, "", "", response);
    }

    @Override
    public void removePlayerPermission(final UUID uuid, String permission, String world, String server, final ResponseRunnable response) {
        db.deletePlayerPermission(uuid, permission, world, server, new DBRunnable(response.isSameThread()) {

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
    public void removePlayerGroup(UUID uuid, String groupName, ResponseRunnable response) {
        removePlayerGroup(uuid, groupName, "", false, response);
    }

    @Override
    public void removePlayerGroup(UUID uuid, String groupName, boolean negated, ResponseRunnable response) {
        removePlayerGroup(uuid, groupName, "", negated, response);
    }

    @Override
    public void removePlayerGroup(final UUID uuid, String groupName, String server, final boolean negated, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        final String serv = server;

        final Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                        if (cachedGroup.getGroup().getId() == group.getId() && cachedGroup.isNegated() == negated) {
                            it.remove();
                            removed = true;
                        }
                    }

                    if (removed)
                        playerGroups.put(serv, groupList);
                    else {
                        response.setResponse(false, "Player does not have the specified group for the specified server.");
                        db.scheduler.runSync(response, response.isSameThread());
                        return;
                    }

                    String playerGroupStringOutput = getPlayerGroupsRawCached(playerGroups);
                    db.setPlayerGroups(uuid, playerGroupStringOutput, new DBRunnable(response.isSameThread()) {

                        @Override
                        public void run() {
                            if (result.booleanValue()) {
                                response.setResponse(true, "Player group removed.");
                                reloadPlayer(uuid);
                                notifyReloadPlayer(uuid);
                            } else
                                response.setResponse(false, "Could not remove player group. Check console for errors.");
                            db.scheduler.runSync(response, response.isSameThread());
                        }
                    });
                } else {
                    response.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(response, response.isSameThread());
                }
            }
        });
    }

    @Override
    public void addPlayerGroup(UUID uuid, String groupName, ResponseRunnable response) {
        addPlayerGroup(uuid, groupName, false, response);
    }

    @Override
    public void addPlayerGroup(UUID uuid, String groupName, final boolean negated, ResponseRunnable response) {
        addPlayerGroup(uuid, groupName, "", negated, response);
    }

    @Override
    public void addPlayerGroup(final UUID uuid, String groupName, String server, final boolean negated, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        final String serv = server;

        final Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                        if (cachedGroup.getGroup().getId() == group.getId() && cachedGroup.isNegated() == negated) {
                            response.setResponse(false, "Player already has this group.");
                            db.scheduler.runSync(response, response.isSameThread());
                            return;
                        }
                    }

                    groupList.add(new CachedGroup(group, negated));
                    playerGroups.put(serv, groupList);

                    String playerGroupStringOutput = getPlayerGroupsRawCached(playerGroups);
                    db.setPlayerGroups(uuid, playerGroupStringOutput, new DBRunnable(response.isSameThread()) {

                        @Override
                        public void run() {
                            if (result.booleanValue()) {
                                response.setResponse(true, "Player group added.");
                                reloadPlayer(uuid);
                                notifyReloadPlayer(uuid);
                            } else
                                response.setResponse(false, "Could not add player group. Check console for errors.");
                            db.scheduler.runSync(response, response.isSameThread());
                        }
                    });

                } else {
                    response.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(response, response.isSameThread());
                }
            }
        });

    }

    @Override
    public void setPlayerRank(final UUID uuid, String groupName, final ResponseRunnable response) {
        // Get player groups on specified ladder
        // Use the group type of the first one of those groups
        // replace old group with group "groupname"

        final Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                                if (current.getGroup().getLadder().equals(group.getLadder())) {
                                    if (toUse == null)
                                        toUse = current.getGroup();
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        clone.set(clone.indexOf(current), new CachedGroup(group, current.isNegated()));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            response.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(response, response.isSameThread());
                        } else {
                            String playerGroupStringOutput = getPlayerGroupsRawCached(output);
                            db.setPlayerGroups(uuid, playerGroupStringOutput, new DBRunnable(response.isSameThread()) {

                                @Override
                                public void run() {
                                    if (result.booleanValue()) {
                                        response.setResponse(true, "Player rank set on ladder \"" + group.getLadder() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        response.setResponse(false, "Could not set player rank. Check console for errors.");
                                    db.scheduler.runSync(response, response.isSameThread());
                                }
                            });
                        }
                    } else {
                        response.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(response, response.isSameThread());
                    }
                } else {
                    response.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(response, response.isSameThread());
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
                if (it.hasNext())
                    return it.next().getValue();
                else
                    return null;
            }
        }
        return null;
    }

    public Group getLowerGroup(Group group, List<Group> groups) {
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>();
        for (Group current : groups) {
            if (current.getLadder().equals(group.getLadder()))
                sortedGroups.put(current.getRank(), current);
        }

        Iterator<Entry<Integer, Group>> it = sortedGroups.descendingMap().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Group> entry = it.next();
            if (entry.getKey() == group.getRank() && entry.getValue().getName().equals(group.getName())) {
                if (it.hasNext())
                    return it.next().getValue();
                else
                    return null;
            }
        }
        return null;
    }

    @Override
    public void promotePlayer(final UUID uuid, final String ladder, final ResponseRunnable response) {
        final List<Group> groupsClone = new ArrayList<Group>(groups.values());
        getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                                if (current.getGroup().getLadder().equals(ladder)) {
                                    if (toUse == null) {
                                        toUse = current.getGroup();
                                        toPromoteTo = getHigherGroup(toUse, groupsClone);
                                        if (toPromoteTo == null) {
                                            response.setResponse(false, "There is no group on this ladder with a higher rank.");
                                            db.scheduler.runSync(response, response.isSameThread());
                                            return;
                                        }
                                    }
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        // This is the group to promote from
                                        clone.set(clone.indexOf(current), new CachedGroup(toPromoteTo, current.isNegated()));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            response.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(response, response.isSameThread());
                        } else {
                            final Group toPromoteToFinal = toPromoteTo;
                            String playerGroupStringOutput = getPlayerGroupsRawCached(output);
                            db.setPlayerGroups(uuid, playerGroupStringOutput, new DBRunnable(response.isSameThread()) {

                                @Override
                                public void run() {
                                    if (result.booleanValue()) {
                                        response.setResponse(true, "Player was promoted to \"" + toPromoteToFinal.getName() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        response.setResponse(false, "Could not promote player. Check console for errors.");
                                    db.scheduler.runSync(response, response.isSameThread());
                                }
                            });
                        }

                    } else {
                        response.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(response, response.isSameThread());
                    }
                } else {
                    response.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(response, response.isSameThread());
                }
            }
        });
    }

    @Override
    public void demotePlayer(final UUID uuid, final String ladder, final ResponseRunnable response) {
        final List<Group> groupsClone = new ArrayList<Group>(groups.values());
        getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

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
                                if (current.getGroup().getLadder().equals(ladder)) {
                                    if (toUse == null) {
                                        toUse = current.getGroup();
                                        toPromoteTo = getLowerGroup(toUse, groupsClone);
                                        if (toPromoteTo == null) {
                                            response.setResponse(false, "There is no group on this ladder with a lower rank.");
                                            db.scheduler.runSync(response, response.isSameThread());
                                            return;
                                        }
                                    }
                                    // Replace with new group if they are on the same ladder and if toUse and current is the same group
                                    if (toUse.getId() == current.getGroup().getId()) {
                                        // This is the group to promote from
                                        clone.set(clone.indexOf(current), new CachedGroup(toPromoteTo, current.isNegated()));
                                        changed = true;
                                    }
                                }
                            }

                            output.put(server, clone);
                        }

                        if (!changed) {
                            response.setResponse(false, "Player has no groups on the specified ladder.");
                            db.scheduler.runSync(response, response.isSameThread());
                        } else {
                            final Group toPromoteToFinal = toPromoteTo;
                            String playerGroupStringOutput = getPlayerGroupsRawCached(output);
                            db.setPlayerGroups(uuid, playerGroupStringOutput, new DBRunnable(response.isSameThread()) {

                                @Override
                                public void run() {
                                    if (result.booleanValue()) {
                                        response.setResponse(true, "Player was demoted to \"" + toPromoteToFinal.getName() + "\".");
                                        reloadPlayer(uuid);
                                        notifyReloadPlayer(uuid);
                                    } else
                                        response.setResponse(false, "Could not demote player. Check console for errors.");
                                    db.scheduler.runSync(response, response.isSameThread());
                                }
                            });
                        }

                    } else {
                        response.setResponse(false, "Player has no groups.");
                        db.scheduler.runSync(response, response.isSameThread());
                    }
                } else {
                    response.setResponse(false, "Player does not exist.");
                    db.scheduler.runSync(response, response.isSameThread());
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

        db.insertGroup(name, "", "", "", ladder, rank, new DBRunnable(response.isSameThread()) {

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
    public void deleteGroup(String groupName, final ResponseRunnable response) {
        db.deleteGroup(groupName, new DBRunnable(response.isSameThread()) {

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
    public void addGroupPermission(String groupName, String permission, ResponseRunnable response) {
        addGroupPermission(groupName, permission, "", "", response);
    }

    @Override
    public void addGroupPermission(String groupName, String permission, String world, String server, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            PowerfulPermission sp = new PowerfulPermission(permission, world, server);

            for (Permission temp : groupPermissions) {
                if (temp.getPermissionString().equals(permission) && temp.getServer().equals(server) && temp.getWorld().equals(world)) {
                    response.setResponse(false, "Group already has the specified permission.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }
            }

            groupPermissions.add(sp);

            db.insertPermission((UUID) null, "", groupName, permission, world, server, new DBRunnable(response.isSameThread()) {

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
    public void removeGroupPermission(String groupName, String permission, ResponseRunnable response) {
        removeGroupPermission(groupName, permission, "", "", response);
    }

    @Override
    public void removeGroupPermission(String groupName, String permission, String world, String server, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group != null) {
            db.deleteGroupPermission(groupName, permission, world, server, new DBRunnable(response.isSameThread()) {

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
    public void removeGroupPermissions(String groupName, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group != null) {
            List<Permission> groupPermissions = group.getOwnPermissions();

            if (groupPermissions.size() <= 0) {
                response.setResponse(false, "Group does not have any permissions.");
                db.scheduler.runSync(response, response.isSameThread());
                return;
            }

            final Counter counter = new Counter();
            db.deleteGroupPermissions(groupName, new DBRunnable(response.isSameThread()) {

                @Override
                public void run() {
                    counter.add(result.rowsChanged());
                }
            });

            response.setResponse(true, "Removed " + counter.amount() + " permissions from the group.");
            db.scheduler.runSync(response, response.isSameThread());
            loadGroups(response.isSameThread());
            notifyReloadGroups();
        } else {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
        }
    }

    @Override
    public void addGroupParent(String groupName, String parentGroupName, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupName);
            if (parentGroup != null) {
                String currentParents = PowerfulGroup.encodeParents(group.getParents());
                if (currentParents.contains(parentGroupName)) {
                    response.setResponse(false, "Group already has the specified parent.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }
                currentParents += parentGroup.getId() + ";";

                db.setGroupParents(groupName, currentParents, new DBRunnable(response.isSameThread()) {

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
    public void removeGroupParent(String groupName, String parentGroupName, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group != null) {
            Group parentGroup = getGroup(parentGroupName);
            if (parentGroup != null) {
                String currentParents = PowerfulGroup.encodeParents(group.getParents());
                String toRemove = parentGroup.getId() + ";";
                if (!currentParents.contains(toRemove)) {
                    response.setResponse(false, "Group does not have the specified parent.");
                    db.scheduler.runSync(response, response.isSameThread());
                    return;
                }
                currentParents = currentParents.replaceFirst(parentGroup.getId() + ";", "");

                db.setGroupParents(groupName, currentParents, new DBRunnable(response.isSameThread()) {

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
    public void setGroupPrefix(String groupName, String prefix, final ResponseRunnable response) {
        setGroupPrefix(groupName, prefix, "", response);
    }

    @Override
    public void setGroupPrefix(String groupName, String prefix, String server, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        HashMap<String, String> currentPrefix = group.getPrefixes();
        if (prefix.isEmpty())
            currentPrefix.remove(server);
        else
            currentPrefix.put(server, prefix);
        final String output = PowerfulGroup.encodePrefixSuffix(currentPrefix);

        db.setGroupPrefix(groupName, output, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Group prefix set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Could not set group prefix. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setGroupSuffix(String groupName, String suffix, final ResponseRunnable response) {
        setGroupSuffix(groupName, suffix, "", response);
    }

    @Override
    public void setGroupSuffix(String groupName, String suffix, String server, final ResponseRunnable response) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        HashMap<String, String> currentSuffix = group.getSuffixes();
        if (suffix.isEmpty())
            currentSuffix.remove(server);
        else
            currentSuffix.put(server, suffix);
        final String output = PowerfulGroup.encodePrefixSuffix(currentSuffix);

        db.setGroupSuffix(groupName, output, new DBRunnable(response.isSameThread()) {

            @Override
            public void run() {
                if (result.booleanValue()) {
                    response.setResponse(true, "Group suffix set.");
                    loadGroups(response.isSameThread());
                    notifyReloadGroups();
                } else
                    response.setResponse(false, "Could not set group suffix. Check console for errors.");
                db.scheduler.runSync(response, response.isSameThread());
            }
        });
    }

    @Override
    public void setGroupLadder(String groupName, String ladder, final ResponseRunnable response) {
        if (ladder == null || ladder.isEmpty())
            ladder = "default";

        Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setGroupLadder(groupName, ladder, new DBRunnable(response.isSameThread()) {

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
    public void setGroupRank(String groupName, int rank, final ResponseRunnable response) {
        Group group = getGroup(groupName);
        if (group == null) {
            response.setResponse(false, "Group does not exist.");
            db.scheduler.runSync(response, response.isSameThread());
            return;
        }

        db.setGroupRank(groupName, rank, new DBRunnable(response.isSameThread()) {

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
