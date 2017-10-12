package com.github.gustav9797.PowerfulPerms.database;

import com.github.gustav9797.PowerfulPermsAPI.IScheduler;

public abstract class Database implements IDatabase {
    public IScheduler scheduler;
    public static String tblGroupParents = "groupparents";
    public static String tblGroupPermissions = "grouppermissions";
    public static String tblGroupPrefixes = "groupprefixes";
    public static String tblGroups = "groups";
    public static String tblGroupSuffixes = "groupsuffixes";
    public static String tblPlayerGroups = "playergroups";
    public static String tblPlayerPermissions = "playerpermissions";
    public static String tblPlayers = "players";
    public static String prefix = "";

    public Database(IScheduler scheduler, String prefix) {
        this.scheduler = scheduler;

        tblGroupParents = "groupparents";
        tblGroupPermissions = "grouppermissions";
        tblGroupPrefixes = "groupprefixes";
        tblGroups = "groups";
        tblGroupSuffixes = "groupsuffixes";
        tblPlayerGroups = "playergroups";
        tblPlayerPermissions = "playerpermissions";
        tblPlayers = "players";

        if (prefix != null && !prefix.isEmpty())
            setTablePrefix(prefix);
    }

    public void setTablePrefix(String prefix) {
        Database.prefix = prefix;
        tblGroupParents = prefix + tblGroupParents;
        tblGroupPermissions = prefix + tblGroupPermissions;
        tblGroupPrefixes = prefix + tblGroupPrefixes;
        tblGroups = prefix + tblGroups;
        tblGroupSuffixes = prefix + tblGroupSuffixes;
        tblPlayerGroups = prefix + tblPlayerGroups;
        tblPlayerPermissions = prefix + tblPlayerPermissions;
        tblPlayers = prefix + tblPlayers;
    }
}
