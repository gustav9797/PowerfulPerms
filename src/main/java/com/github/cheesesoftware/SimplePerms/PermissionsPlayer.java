package com.github.cheesesoftware.SimplePerms;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class PermissionsPlayer {
    private Group group;
    private Map<String, String> permissions = new HashMap<String, String>();
    private PermissionAttachment pa;
    private String prefix = "";

    public PermissionsPlayer(Player p, Group group, Map<String, String> permissions, PermissionAttachment pa, String prefix) {
	this.group = group;
	this.permissions = permissions;
	this.pa = pa;
	this.prefix = prefix;
	this.UpdatePermissionAttachment();
    }

    public void Update(Group group, PermissionAttachment pa, String prefix) {
	this.group = group != null ? group : this.group;
	this.pa = pa != null ? pa : this.pa;
	this.prefix = prefix != null ? prefix : this.prefix;
    }

    public Group getGroup() {
	return this.group;
    }

    public PermissionAttachment getPermissionAttachment() {
	return this.pa;
    }

    public void clearPermissions() {
	permissions.clear();
    }

    public Map<String, String> getPermissions() {
	Map<String, String> newTemp = new HashMap<String, String>();
	newTemp.putAll(this.permissions);
	if (group != null) {
	    newTemp.putAll(group.getPermissions());
	}
	return newTemp;
    }

    public void UpdatePermissionAttachment() {
	if (group != null) {
	    for (Map.Entry<String, String> e : group.getPermissions().entrySet()) {
		pa.setPermission(e.getKey(), (e.getValue().equals(Bukkit.getServerName()) || e.getValue().isEmpty() || e.getValue().equals("ALL")) ? true : false);
		// Bukkit.getLogger().info("setPermission " + e.getKey() + " server " + e.getValue());
	    }
	    for (Map.Entry<String, String> e : permissions.entrySet())
		pa.setPermission(e.getKey(), (e.getValue().equals(Bukkit.getServerName()) || e.getValue().isEmpty() || e.getValue().equals("ALL")) ? true : false);

	}
    }

    public String getPrefix() {
	return (!this.prefix.isEmpty() ? this.prefix : (group != null && group.getPrefix() != "" ? group.getPrefix() : ""));
    }
}
