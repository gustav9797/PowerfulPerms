package com.github.cheesesoftware.PowerfulPerms.database;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public interface IDatabase {

    public static String tblPlayers = "players";
    public static String tblGroups = "groups";
    public static String tblPermissions = "permissions";

    public void applyPatches(PowerfulPermsPlugin plugin);

    public void tableExists(String table, DBRunnable done);

    public void createGroupsTable(DBRunnable done);

    public void createPlayersTable(DBRunnable done);

    public void createPermissionsTable(DBRunnable done);

    public void insertGroup(String group, String parents, String prefix, String suffix, String ladder, int rank, DBRunnable done);

    public void insertPlayer(UUID uuid, String name, String groups, String prefix, String suffix, DBRunnable done);

    public void getPlayer(UUID uuid, DBRunnable done);

    public void getPlayers(String name, DBRunnable done);

    public void setPlayerName(UUID uuid, String name, DBRunnable done);

    public void setPlayerUUID(String name, UUID uuid, DBRunnable done);

    public void getGroups(DBRunnable done);

    public void getGroupPermissions(String group, DBRunnable done);

    public void getPlayerPermissions(UUID uuid, DBRunnable done);

    public void playerHasPermission(UUID uuid, String permission, String world, String server, DBRunnable done);

    public void insertPermission(UUID uuid, String name, String group, String permission, String world, String server, DBRunnable done);

    public void updatePlayerPermissions(UUID uuid, String name, DBRunnable done);

    public void deletePlayerPermission(UUID uuid, String permission, String world, String server, DBRunnable done);

    public void deletePlayerPermissions(UUID uuid, DBRunnable done);

    public void deleteGroupPermission(String group, String permission, String world, String server, DBRunnable done);

    public void deleteGroupPermissions(String group, DBRunnable done);

    public void setPlayerPrefix(UUID uuid, String prefix, DBRunnable done);

    public void setPlayerSuffix(UUID uuid, String suffix, DBRunnable done);

    public void setPlayerGroups(UUID uuid, String groups, DBRunnable done);

    public void deleteGroup(String group, DBRunnable done);

    public void setGroupParents(String group, String parents, DBRunnable done);

    public void setGroupPrefix(String group, String prefix, DBRunnable done);

    public void setGroupSuffix(String group, String suffix, DBRunnable done);
}
