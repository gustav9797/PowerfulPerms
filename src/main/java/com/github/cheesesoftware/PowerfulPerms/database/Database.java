package com.github.cheesesoftware.PowerfulPerms.database;

import com.github.cheesesoftware.PowerfulPerms.common.IScheduler;

public abstract class Database implements IDatabase {
    public static String tblPlayers = "players";
    public static String tblGroups = "groups";
    public static String tblPermissions = "permissions";
    public IScheduler scheduler;
    
    public Database(IScheduler scheduler) {
        this.scheduler = scheduler;
        
        tblPlayers = "players";
        tblGroups = "groups";
        tblPermissions = "permissions";
    }

    public void setTablePrefix(String prefix) {
        tblPlayers = prefix + tblPlayers;
        tblGroups = prefix + tblGroups;
        tblPermissions = prefix + tblPermissions;
    }
}
