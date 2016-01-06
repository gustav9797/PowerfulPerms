package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.database.DBDocument;

public interface IPermissionManager {

    public void notifyReloadGroups();

    public void notifyReloadPlayers();

    public void notifyReloadPlayer(String playerName);

    public void reloadPlayers();

    public void reloadPlayer(String name);

    public void reloadPlayer(UUID uuid);

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(UUID uuid);

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(String name);

    public void onDisable();

    public void reloadGroups();

    /**
     * Gets a group from its name.
     * 
     * @param groupName
     *            The name of the group to get.
     */
    public Group getGroup(String groupName);

    /**
     * Get all groups.
     * 
     * @return All groups.
     */
    public Collection<Group> getGroups();

    public void getPlayerGroups(String playerName, ResultRunnable<HashMap<String, List<CachedGroup>>> resultRunnable);

    public void getPlayerData(String playerName, ResultRunnable<DBDocument> resultRunnable);

    public Group getPlayerPrimaryGroup(HashMap<String, List<CachedGroup>> groups);

    public void getPlayerPrimaryGroup(String playerName, ResultRunnable<Group> resultRunnable);

    /**
     * Gets a map containing all the permissions a player has. If player is not online data will be loaded from database.
     */
    public void getPlayerOwnPermissions(String playerName, ResultRunnable<List<PowerfulPermission>> resultRunnable);

    /**
     * Gets the prefix of a player. If player isn't online it retrieves player own prefix from database.
     */
    public void getPlayerPrefix(String playerName, ResultRunnable<String> resultRunnable);

    /**
     * Gets the suffix of a player. If player isn't online it retrieves player own suffix from database.
     */
    public void getPlayerSuffix(String playerName, ResultRunnable<String> resultRunnable);

    /**
     * Gets the own prefix of a player. If player isn't online it retrieves data from database.
     */
    public void getPlayerOwnPrefix(String playerName, ResultRunnable<String> resultRunnable);

    /**
     * Gets the own suffix of a player. If player isn't online it retrieves data from database.
     */
    public void getPlayerOwnSuffix(String playerName, ResultRunnable<String> resultRunnable);

    public String getGroupPrefix(String groupName, String server);

    public String getGroupSuffix(String groupName, String server);

    public HashMap<String, String> getGroupServerPrefix(String groupName);

    public HashMap<String, String> getGroupServerSuffix(String groupName);

    public void addPlayerPermission(String playerName, String permission, ResponseRunnable response);

    public void addPlayerPermission(String playerName, String permission, String world, String server, ResponseRunnable response);

    public void removePlayerPermission(String playerName, String permission, ResponseRunnable response);

    public void removePlayerPermission(String playerName, String permission, String world, String server, ResponseRunnable response);

    public void removePlayerPermissions(String playerName, ResponseRunnable response);

    public void setPlayerPrefix(String playerName, String prefix, ResponseRunnable response);

    public void setPlayerSuffix(String playerName, String suffix, ResponseRunnable response);

    public void setPlayerPrimaryGroup(String playerName, String groupName, String server, ResponseRunnable response);

    public void removePlayerGroup(String playerName, String groupName, ResponseRunnable response);

    public void removePlayerGroup(String playerName, String groupName, boolean negated, ResponseRunnable response);

    public void removePlayerGroup(String playerName, String groupName, String server, boolean negated, ResponseRunnable response);

    public void addPlayerGroup(String playerName, String groupName, ResponseRunnable response);

    public void addPlayerGroup(String playerName, String groupName, boolean negated, ResponseRunnable response);

    public void addPlayerGroup(String playerName, String groupName, String server, boolean negated, ResponseRunnable response);

    public void createGroup(String name, ResponseRunnable response);

    public void deleteGroup(String groupName, ResponseRunnable response);

    public void addGroupPermission(String groupName, String permission, ResponseRunnable response);

    public void addGroupPermission(String groupName, String permission, String world, String server, ResponseRunnable response);

    public void removeGroupPermission(String groupName, String permission, ResponseRunnable response);

    public void removeGroupPermission(String groupName, String permission, String world, String server, ResponseRunnable response);

    public void removeGroupPermissions(String groupName, ResponseRunnable response);

    public void addGroupParent(String groupName, String parentGroupName, ResponseRunnable response);

    public void removeGroupParent(String groupName, String parentGroupName, ResponseRunnable response);

    public void setGroupPrefix(String groupName, String prefix, ResponseRunnable response);

    public void setGroupPrefix(String groupName, String prefix, String server, ResponseRunnable response);

    public void setGroupSuffix(String groupName, String suffix, ResponseRunnable response);

    public void setGroupSuffix(String groupName, String suffix, String server, ResponseRunnable response);

}