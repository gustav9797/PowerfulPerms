package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault extends Permission {

    private PermissionManager permissionManager;

    public PowerfulPerms_Vault(PermissionManager permissionManager) {
	this.permissionManager = permissionManager;
    }

    @Override
    public String[] getGroups() {
	List<Group> groups = permissionManager.getGroups();
	List<String> groupNames = new ArrayList<String>();
	for (Group group : groups)
	    groupNames.add(group.getName());
	return (String[]) groupNames.toArray();
    }

    @Override
    public String getName() {
	return "PowerfulPerms";
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
	PermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
	if (p != null) {
	    List<Group> groups = p.getApplyingGroups(Bukkit.getServerName());
	    List<String> groupNames = new ArrayList<String>();
	    for (Group group : groups)
		groupNames.add(group.getName());
	    return (String[]) groupNames.toArray();
	}
	return null;
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
	Group group = permissionManager.getPlayerPrimaryGroup(player);
	if (group != null)
	    return group.getName();
	return null;
    }

    @Override
    public boolean groupAdd(String arg0, String arg1, String arg2) {
	return false;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
	return permissionManager.groupHasPermission(group, permission, Bukkit.getServerName(), world);
    }

    @Override
    public boolean groupRemove(String arg0, String arg1, String arg2) {
	return false;
    }

    @Override
    public boolean hasGroupSupport() {
	return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
	return true;
    }

    @Override
    public boolean isEnabled() {
	return true;
    }

    @Override
    public boolean playerAdd(String arg0, String arg1, String arg2) {
	return false;
    }

    @Override
    public boolean playerAddGroup(String arg0, String arg1, String arg2) {
	return false;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
	return permissionManager.playerHasPermission(player, permission, Bukkit.getServerName(), world);
    }

    @Override
    public boolean playerInGroup(String world, String player, String groupName) {
	PermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
	if (p != null) {
	    List<Group> groups = p.getApplyingGroups(Bukkit.getServerName());
	    for (Group group : groups) {
		if (group.getName().equalsIgnoreCase(groupName))
		    return true;
	    }
	}
	return false;
    }

    @Override
    public boolean playerRemove(String arg0, String arg1, String arg2) {
	return false;
    }

    @Override
    public boolean playerRemoveGroup(String arg0, String arg1, String arg2) {
	return false;
    }

}
