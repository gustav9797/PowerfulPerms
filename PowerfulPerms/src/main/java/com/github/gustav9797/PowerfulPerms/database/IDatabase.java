package com.github.gustav9797.PowerfulPerms.database;

import java.util.Date;
import java.util.UUID;

public interface IDatabase {

    public void applyPatches();

    public boolean ping();

    public boolean tableExists(String table);

    public void createTable(String tableName);

    public void renameTable(String tableName, String newTableName);

    public boolean insertGroup(int id, String group, String ladder, int rank);

    public boolean insertPlayer(UUID uuid, String name, String prefix, String suffix);

    public DBResult getPlayer(UUID uuid);

    public DBResult deletePlayer(UUID uuid);

    public DBResult getPlayersInGroup(int groupId, int limit, int offset);

    public DBResult getPlayers(String name);

    public DBResult getPlayersCaseInsensitive(String name);

    public boolean setPlayerName(UUID uuid, String name);

    public boolean setPlayerUUID(String name, UUID uuid);

    public DBResult getGroups();

    public DBResult getGroupPermissions(int groupId);

    public DBResult getGroupPermissions();

    public DBResult getPlayerPermissions(UUID uuid);

    public boolean playerHasPermission(UUID uuid, String permission, String world, String server, Date expires);

    public boolean insertPlayerPermission(UUID uuid, String permission, String world, String server, Date expires);

    public boolean insertGroupPermission(int groupId, String permission, String world, String server, Date expires);

    public DBResult deletePlayerPermission(UUID uuid, String permission, String world, String server, Date expires);

    public DBResult deletePlayerPermissions(UUID uuid);

    public DBResult deleteGroupPermission(int groupId, String permission, String world, String server, Date expires);

    public DBResult deleteGroupPermissions(int groupId);

    public boolean setPlayerPrefix(UUID uuid, String prefix);

    public boolean setPlayerSuffix(UUID uuid, String suffix);

    public boolean insertPlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires);

    public boolean deletePlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires);

    public boolean deletePlayerGroups(UUID uuid);

    public DBResult getPlayerGroups(UUID uuid);

    public boolean deleteGroup(int groupId);

    public boolean insertGroupParent(int groupId, int parentGroupId);

    public boolean deleteGroupParent(int groupId, int parentGroupId);

    public boolean deleteGroupParents(int groupId);

    public DBResult getGroupParents(int groupId);

    public DBResult getGroupParents();

    public boolean insertGroupPrefix(int groupId, String prefix, String server);

    public boolean deleteGroupPrefix(int groupId, String prefix, String server);

    public boolean deleteGroupPrefixes(int groupId);

    public DBResult getGroupPrefixes(int groupId);

    public DBResult getGroupPrefixes();

    public boolean insertGroupSuffix(int groupId, String suffix, String server);

    public boolean deleteGroupSuffix(int groupId, String suffix, String server);

    public boolean deleteGroupSuffixes(int groupId);

    public DBResult getGroupSuffixes(int groupId);

    public DBResult getGroupSuffixes();

    public boolean setGroupLadder(int groupId, String ladder);

    public boolean setGroupRank(int groupId, int rank);

    public boolean setGroupName(int groupId, String name);
}
