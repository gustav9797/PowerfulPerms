package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;

public class CachedPlayer {

    private String groups;
    private String prefix;
    private String suffix;
    private ArrayList<PowerfulPermission> perms;
    private boolean loginEventFinished = false;

    public CachedPlayer(String groups, String prefix, String suffix, ArrayList<PowerfulPermission> perms) {
	this.groups = groups;
	this.prefix = prefix;
	this.suffix = suffix;
	this.perms = perms;
    }
    
    public CachedPlayer() {
	
    }

    public void setLoginEventfinished() {
	loginEventFinished = true;
    }

    public boolean getLoginEventFinished() {
	return loginEventFinished;
    }

    public String getGroups() {
	return groups;
    }
    
    public String getPrefix() {
	return prefix;
    }
    
    public String getSuffix() {
	return suffix;
    }
    
    public ArrayList<PowerfulPermission> getPermissions() {
	return perms;
    }
}
