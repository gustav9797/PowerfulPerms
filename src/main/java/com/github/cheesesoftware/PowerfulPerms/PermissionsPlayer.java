package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class PermissionsPlayer {
    private Player player;
    private HashMap<String, List<Group>> serverGroups = new HashMap<String, List<Group>>(); // Contains all player main groups. Server "" is the global default group.
    private ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private PermissionAttachment pa;
    private String prefix = "";
    private String suffix = "";

    public PermissionsPlayer(Player p, HashMap<String, List<Group>> serverGroups, ArrayList<PowerfulPermission> permissions, PermissionAttachment pa, String prefix, String suffix) {
	this.player = p;
	this.serverGroups = serverGroups;
	this.permissions = permissions;
	this.pa = pa;
	this.prefix = prefix;
	this.suffix = suffix;
	this.UpdatePermissionAttachment();
    }

    public void Update(HashMap<String, List<Group>> serverGroups, PermissionAttachment pa, String prefix) {
	this.serverGroups = (serverGroups != null ? serverGroups : this.serverGroups);
	this.pa = pa != null ? pa : this.pa;
	this.prefix = prefix != null ? prefix : this.prefix;
    }

    /**
     * Returns the player attached to this PermissionsPlayer.
     */
    public Player getPlayer() {
	return this.player;
    }

    /**
     * Returns the player's primary group.
     */
    public Group getPrimaryGroup() {
	Iterator<Group> it = serverGroups.get("").iterator();
	return it.next(); // First group is primary group.
    }

    /**
     * Returns a list of groups which apply to a specific server.
     */
    public List<Group> getApplyingGroups(String server) {
	if (server.equalsIgnoreCase("all"))
	    server = "";
	List<Group> groups = new ArrayList<Group>();
	List<Group> serverGroupsTemp = serverGroups.get(server);
	if(serverGroupsTemp != null)
	    groups.addAll(serverGroupsTemp);
	if (!server.isEmpty())
	    groups.addAll(serverGroups.get(""));
	return groups;
    }

    /**
     * Returns all groups a player has, indexed by server name.
     */
    public HashMap<String, List<Group>> getServerGroups() {
	return this.serverGroups;
    }

    /**
     * Used when storing data in the database.
     */
    public String getRawServerGroups() {
	String output = "";
	for (Entry<String, List<Group>> entry : this.serverGroups.entrySet()) {
	    for (Group group : entry.getValue())
		output += entry.getKey() + ":" + group.getName() + ";";
	}
	return output;
    }

    /**
     * Sets the player's groups as seen in getServerGroups() Changes won't save for now.
     */
    public void setServerGroups(HashMap<String, List<Group>> serverGroups) {
	this.serverGroups = serverGroups;
	this.UpdatePermissionAttachment();
    }

    /**
     * Returns the PermissionAttachment used with Bukkit.
     */
    public PermissionAttachment getPermissionAttachment() {
	return this.pa;
    }

    /**
     * Clears the player-specific permissions of this player. Changes won't save for now.
     */
    public void clearPermissions() {
	permissions.clear();
	this.UpdatePermissionAttachment();
    }

    /**
     * Returns a list of all permissions this player has, including derived.
     */
    public ArrayList<PowerfulPermission> getAllPermissions() {
	ArrayList<PowerfulPermission> newTemp = new ArrayList<PowerfulPermission>();
	newTemp.addAll(this.permissions);

	for (Entry<String, List<Group>> e : serverGroups.entrySet()) {
	    // Check if same server.
	    if (e.getKey().isEmpty() || e.getKey().equalsIgnoreCase("ALL") || e.getKey().equals(Bukkit.getServerName())) {
		for (Group group : e.getValue()) {
		    // Add all permissions since they apply.
		    newTemp.addAll(group.getPermissions());
		}
	    }
	}
	return newTemp;
    }

    /**
     * Internal function to update the PermissionAttachment.
     */
    public void UpdatePermissionAttachment() {
	for (String permission : pa.getPermissions().keySet()) {
	    pa.unsetPermission(permission);
	}

	// Add permissions derived from groups.
	for (Entry<String, List<Group>> entry : serverGroups.entrySet()) {
	    if (entry.getKey().isEmpty() || entry.getKey().equalsIgnoreCase("ALL") || entry.getKey().equals(Bukkit.getServerName())) {
		for (Group group : entry.getValue()) {
		    if (group != null) {
			for (PowerfulPermission e : group.getPermissions()) {

			    boolean isSameServer = false;
			    boolean isSameWorld = false;

			    if (e.getServer().isEmpty() || e.getServer().equalsIgnoreCase("ALL") || e.getServer().equals(Bukkit.getServerName()))
				isSameServer = true;

			    if (e.getWorld().isEmpty() || e.getWorld().equalsIgnoreCase("ALL") || e.getWorld().equals(player.getWorld().getName()))
				isSameWorld = true;

			    if (e.getPermissionString().startsWith("-")) {
				String actualPermission = e.getPermissionString().substring(1);
				pa.setPermission(actualPermission, (isSameServer && isSameWorld ? false : true));
				continue;
			    }

			    if (pa.getPermissions().containsKey(e.getPermissionString()) && pa.getPermissions().get(e.getPermissionString()) == false)
				continue;

			    pa.setPermission(e.getPermissionString(), (isSameServer && isSameWorld ? true : false));
			}
		    }
		}
	    }
	}

	// Add own permissions.
	for (PowerfulPermission e : permissions) {
	    boolean isSameServer = false;
	    boolean isSameWorld = false;

	    if (e.getServer().isEmpty() || e.getServer().equalsIgnoreCase("ALL") || e.getServer().equals(Bukkit.getServerName()))
		isSameServer = true;

	    if (e.getWorld().isEmpty() || e.getWorld().equalsIgnoreCase("ALL") || e.getWorld().equals(player.getWorld().getName()))
		isSameWorld = true;

	    if (e.getPermissionString().startsWith("-")) {
		String actualPermission = e.getPermissionString().substring(1);
		pa.setPermission(actualPermission, (isSameServer && isSameWorld ? false : true));
		continue;
	    }

	    if (pa.getPermissions().containsKey(e.getPermissionString()) && pa.getPermissions().get(e.getPermissionString()) == false)
		continue;

	    pa.setPermission(e.getPermissionString(), (isSameServer && isSameWorld ? true : false));
	}
    }

    /**
     * Returns the player's prefix. If player has no prefix set, return the prefix of the primary group.
     * 
     * @return The prefix.
     */
    public String getPrefix() {
	Group group = getPrimaryGroup();
	return (!prefix.isEmpty() ? prefix : (group != null && group.getPrefix() != "" ? group.getPrefix() : ""));
    }

    /**
     * Returns the player's suffix. If player has no suffix set, return the suffix of the primary group.
     * 
     * @return The suffix.
     */
    public String getSuffix() {
	Group group = getPrimaryGroup();
	return (!suffix.isEmpty() ? suffix : (group != null && group.getSuffix() != "" ? group.getSuffix() : ": "));
    }
}
