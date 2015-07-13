package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PMR;
import com.github.cheesesoftware.PowerfulPerms.PermissionManager;
import com.github.cheesesoftware.PowerfulPerms.PermissionsPlayer;

import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Permissions extends Permission {

    private PermissionManager permissionManager;

    public PowerfulPerms_Vault_Permissions(PermissionManager permissionManager) {
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
    public boolean groupAdd(String world, String group, String permission) {
	PMR result = permissionManager.addGroupPermission(group, permission, world, "");
	return result.isSucceeded();
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
	return permissionManager.groupHasPermission(group, permission, Bukkit.getServerName(), world);
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
	PMR result = permissionManager.removeGroupPermission(group, permission, world, "");
	return result.isSucceeded();
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
    public boolean playerAdd(String world, String player, String permission) {
	PMR result = permissionManager.addPlayerPermission(player, permission, world, "");
	return result.isSucceeded();
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
	PMR result = permissionManager.addPlayerGroup(player, group);
	return result.isSucceeded();
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
    public boolean playerRemove(String world, String player, String permission) {
	PMR result = permissionManager.removePlayerPermission(player, permission, world, "");
	return result.isSucceeded();
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
	PMR result = permissionManager.removePlayerGroup(player, group);
	return result.isSucceeded();
    }

}
