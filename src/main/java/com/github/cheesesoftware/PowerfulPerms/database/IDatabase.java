package com.github.cheesesoftware.PowerfulPerms.database;

import java.util.UUID;

public interface IDatabase {

    public void applyPatches();

    public boolean tableExists(String table);

    public void createTables();

    public void insertGroup(String group, String ladder, int rank, DBRunnable done);

    public void insertPlayer(UUID uuid, String name, String prefix, String suffix, DBRunnable done);
    
    public void getPlayer(UUID uuid, DBRunnable done);

    public void getPlayers(String name, DBRunnable done);

    public void setPlayerName(UUID uuid, String name, DBRunnable done);

    public void setPlayerUUID(String name, UUID uuid, DBRunnable done);

    public void getGroups(DBRunnable done);

    public void getGroupPermissions(int groupId, DBRunnable done);
    
    public void getGroupPermissions(DBRunnable done);

    public void getPlayerPermissions(UUID uuid, DBRunnable done);

    public void playerHasPermission(UUID uuid, String permission, String world, String server, DBRunnable done);

    public void insertPlayerPermission(UUID uuid, String permission, String world, String server, DBRunnable done);

    public void insertGroupPermission(int groupId, String permission, String world, String server, DBRunnable done);

    public void deletePlayerPermission(UUID uuid, String permission, String world, String server, DBRunnable done);

    public void deletePlayerPermissions(UUID uuid, DBRunnable done);

    public void deleteGroupPermission(int groupId, String permission, String world, String server, DBRunnable done);

    public void deleteGroupPermissions(int groupId, DBRunnable done);

    public void setPlayerPrefix(UUID uuid, String prefix, DBRunnable done);

    public void setPlayerSuffix(UUID uuid, String suffix, DBRunnable done);

    public void addPlayerGroup(UUID uuid, int groupId, String server, boolean negated, DBRunnable done);

    public void deletePlayerGroup(UUID uuid, int groupId, String server, boolean negated, DBRunnable done);
    
    public void getPlayerGroups(UUID uuid, DBRunnable done);

    public void deleteGroup(int groupId, DBRunnable done);

    public void addGroupParent(int groupId, int parentGroupId, DBRunnable done);

    public void deleteGroupParent(int groupId, int parentGroupId, DBRunnable done);

    public void deleteGroupParents(int groupId, DBRunnable done);

    public void getGroupParents(int groupId, DBRunnable done);
    
    public void getGroupParents(DBRunnable done);

    public void addGroupPrefix(int groupId, String prefix, String server, DBRunnable done);

    public void deleteGroupPrefix(int groupId, String prefix, String server, DBRunnable done);

    public void deleteGroupPrefixes(int groupId, DBRunnable done);

    public void getGroupPrefixes(int groupId, DBRunnable done);
    
    public void getGroupPrefixes(DBRunnable done);

    public void addGroupSuffix(int groupId, String suffix, String server, DBRunnable done);

    public void deleteGroupSuffix(int groupId, String suffix, String server, DBRunnable done);

    public void deleteGroupSuffixes(int groupId, DBRunnable done);

    public void getGroupSuffixes(int groupId, DBRunnable done);
    
    public void getGroupSuffixes(DBRunnable done);

    public void setGroupLadder(int groupId, String ladder, DBRunnable done);

    public void setGroupRank(int groupId, int rank, DBRunnable done);
}
