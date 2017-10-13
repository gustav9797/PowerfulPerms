package com.github.gustav9797.PowerfulPermsAPI;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.ListenableFuture;

import redis.clients.jedis.Jedis;

public interface PermissionManager {

    // TODO: replace "with specified name"

    /*
     * Returns the Redis connection.
     */
    public Jedis getRedisConnection();

    /*
     * Returns the executor service.
     */
    public ExecutorService getExecutor();

    /*
     * Returns the event handler.
     */
    public EventHandler getEventHandler();

    /**
     * If using Redis: Tells other servers to reload groups.
     */
    public void notifyReloadGroups();

    /**
     * If using Redis: Tells other servers to reload players.
     */
    public void notifyReloadPlayers();

    /**
     * If using Redis: Tells other server to reload a player with the specified UUID.
     */
    public void notifyReloadPlayer(UUID uuid);

    /**
     * Reloads permission data for online players.
     */
    public void reloadPlayers();

    /**
     * Reloads permission data for an online player with the specified name.
     */
    public void reloadPlayer(String name);

    /**
     * Reloads permission data for an online player with the specified UUID.
     */
    public void reloadPlayer(UUID uuid);

    /**
     * Reloads data for default players.
     */
    public void reloadDefaultPlayers(boolean samethread);

    /**
     * Returns the PermissionPlayer instance for the player with the specified UUID. Player has to be online.
     */
    public PermissionPlayer getPermissionPlayer(UUID uuid);

    /**
     * Returns the PermissionPlayer instance for the player with the specified name. Player has to be online.
     */
    public PermissionPlayer getPermissionPlayer(String name);

    /**
     * Reloads permission data for groups and finally reloads online players.
     */
    public void reloadGroups();

    /**
     * Retrieves a group from its name.
     */
    public Group getGroup(String groupName);

    /**
     * Retrieves a group from its ID.
     */
    public Group getGroup(int id);

    /**
     * Retrieves a clone of all groups.
     */
    public Map<Integer, Group> getGroups();

    /**
     * Retrieves all groups of the player with the specified name as they are in the database.
     */
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerOwnGroups(UUID uuid);

    /**
     * Retrieves all current groups of the player with the specified name. If player does not have any groups it includes the groups of player [default].
     */
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerCurrentGroups(UUID uuid);

    /**
     * Retrieves the group with highest rank value of the player.
     */
    public ListenableFuture<Group> getPlayerPrimaryGroup(UUID uuid);

    /**
     * Checks if player uses groups from player [default].
     */
    public ListenableFuture<Boolean> isPlayerDefault(UUID uuid);

    /**
     * Retrieves a DBDocument with permission data of the player with the specified name.
     */
    public ListenableFuture<DBDocument> getPlayerData(UUID uuid);

    /**
     * Retrieves a map containing all the permissions of the player with the specified name.
     */
    public ListenableFuture<List<Permission>> getPlayerOwnPermissions(UUID uuid);

    /**
     * Offline permission check.
     */
    public ListenableFuture<Boolean> playerHasPermission(UUID uuid, String permission, String world, String server);

    /**
     * Retrieves the prefix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerPrefix(UUID uuid, String ladder);

    /**
     * Retrieves the prefix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerPrefix(UUID uuid);

    /**
     * Retrieves the suffix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerSuffix(UUID uuid, String ladder);

    /**
     * Retrieves the suffix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerSuffix(UUID uuid);

    /**
     * Retrieves the own prefix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerOwnPrefix(UUID uuid);

    /**
     * Retrieves the own suffix of the player with the specified name.
     */
    public ListenableFuture<String> getPlayerOwnSuffix(UUID uuid);

    /**
     * Retrieves the prefix of the group with the specified name on the specified server. Set server to an empty String or "all" for all servers.
     */
    public String getGroupPrefix(int groupId, String server);

    /**
     * Retrieves the suffix of the group with the specified name on the specified server. Set server to an empty String or "all" for all servers.
     */
    public String getGroupSuffix(int groupId, String server);

    /**
     * Retrieves the prefixes of the group with the specified name. The map is indexed by server name.
     */
    public HashMap<String, String> getGroupServerPrefix(int groupId);

    /**
     * Retrieves the suffixes of the group with the specified name. The map is indexed by server name.
     */
    public HashMap<String, String> getGroupServerSuffix(int groupId);

    /**
     * Retrieves UUID from player name. If player is not online it uses Mojang API.
     */
    public ListenableFuture<UUID> getConvertUUID(final String playerName);

    /**
     * Retrieves the scheduler used for sync and asynchronous operations, working on both BungeeCord and Spigot.
     */
    public IScheduler getScheduler();

    // Database accessing functions below

    public ListenableFuture<Response> createPlayer(String name, UUID uuid);

    public ListenableFuture<Response> addPlayerPermission(UUID uuid, String permission);

    public ListenableFuture<Response> addPlayerPermission(UUID uuid, String permission, String world, String server, final Date expires);

    public ListenableFuture<Response> removePlayerPermission(UUID uuid, String permission);

    public ListenableFuture<Response> removePlayerPermission(UUID uuid, String permission, String world, String server, final Date expires);

    public ListenableFuture<Response> removePlayerPermissions(UUID uuid);

    public ListenableFuture<Response> setPlayerPrefix(UUID uuid, String prefix);

    public ListenableFuture<Response> setPlayerSuffix(UUID uuid, String suffix);

    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId);

    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId, boolean negated);

    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId, String server, boolean negated, final Date expires);

    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId);

    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId, boolean negated);

    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId, String server, boolean negated, final Date expires);

    public ListenableFuture<Response> setPlayerRank(UUID uuid, int groupId);

    public ListenableFuture<Response> promotePlayer(UUID uuid, String ladder);

    public ListenableFuture<Response> demotePlayer(UUID uuid, String ladder);

    public ListenableFuture<Response> deletePlayer(UUID uuid);

    public ListenableFuture<Response> createGroup(String name, String ladder, int rank);

    public ListenableFuture<Response> deleteGroup(int groupId);

    public ListenableFuture<Response> addGroupPermission(int groupId, String permission);

    public ListenableFuture<Response> addGroupPermission(int groupId, String permission, String world, String server, final Date expires);

    public ListenableFuture<Response> removeGroupPermission(int groupId, String permission);

    public ListenableFuture<Response> removeGroupPermission(int groupId, String permission, String world, String server, final Date expires);

    public ListenableFuture<Response> removeGroupPermissions(int groupId);

    public ListenableFuture<Response> addGroupParent(int groupId, int parentGroupId);

    public ListenableFuture<Response> removeGroupParent(int groupId, int parentGroupId);

    public ListenableFuture<Response> setGroupPrefix(int groupId, String prefix);

    public ListenableFuture<Response> setGroupPrefix(int groupId, String prefix, String server);

    public ListenableFuture<Response> setGroupSuffix(int groupId, String suffix);

    public ListenableFuture<Response> setGroupSuffix(int groupId, String suffix, String server);

    public ListenableFuture<Response> setGroupLadder(int groupId, String ladder);

    public ListenableFuture<Response> setGroupRank(int groupId, int rank);

    public ListenableFuture<Response> setGroupName(int groupId, String name);

}