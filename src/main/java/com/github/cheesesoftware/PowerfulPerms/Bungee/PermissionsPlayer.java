package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PermissionsPlayer {
    private ProxiedPlayer player;
    private HashMap<String, List<Group>> serverGroups = new HashMap<String, List<Group>>(); // Contains all player main groups. Server "" is the global default group.
    private ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private String prefix = "";
    private String suffix = "";
    private Map<String, Boolean> realPermissions = new HashMap<String, Boolean>();

    public PermissionsPlayer(ProxiedPlayer p, HashMap<String, List<Group>> serverGroups, ArrayList<PowerfulPermission> permissions, String prefix, String suffix) {
        this.player = p;
        this.serverGroups = serverGroups;
        this.permissions = permissions;
        if(PowerfulPerms.debug)
            ProxyServer.getInstance().getLogger().info("own permissions size " + permissions.size());
        this.prefix = prefix;
        this.suffix = suffix;
        this.UpdatePermissions();
    }

    /**
     * Returns the player attached to this PermissionsPlayer.
     */
    public ProxiedPlayer getPlayer() {
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
        if (serverGroupsTemp != null)
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
        this.UpdatePermissions();
    }

    /**
     * Clears the player-specific permissions of this player. Changes won't save for now.
     */
    public void clearPermissions() {
        permissions.clear();
        this.UpdatePermissions();
    }

    /**
     * Returns a list of all permissions this player has, including derived.
     */
    public ArrayList<PowerfulPermission> getAllPermissions() {
        ArrayList<PowerfulPermission> newTemp = new ArrayList<PowerfulPermission>();
        newTemp.addAll(this.permissions);

        for (Entry<String, List<Group>> e : serverGroups.entrySet()) {
            // Check if same server.
            if (e.getKey().isEmpty() || e.getKey().equalsIgnoreCase("ALL") || e.getKey().equals(player.getServer().getInfo().getName())) {
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
    public void UpdatePermissions() {
        // Map<String, Boolean> values = new HashMap<String, Boolean>();
        ArrayList<PowerfulPermission> unprocessedPerms = new ArrayList<PowerfulPermission>();

        ArrayList<String> permsToAdd = new ArrayList<String>();
        ArrayList<String> negatedPerms = new ArrayList<String>();

        // Add permissions derived from groups.
        for (Entry<String, List<Group>> entry : serverGroups.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getKey().equalsIgnoreCase("ALL") || entry.getKey().equals(player.getServer().getInfo().getName())) {
                for (Group group : entry.getValue()) {
                    if (group != null) {
                        unprocessedPerms.addAll(group.getPermissions());
                    }
                }
            }
        }

        // Add own permissions.
        unprocessedPerms.addAll(this.permissions);

        // Sort permissions by negated or not.
        for (PowerfulPermission e : unprocessedPerms) {
            if (permissionApplies(e, player)) {
                if (e.getPermissionString().startsWith("-"))
                    negatedPerms.add(e.getPermissionString());
                else
                    permsToAdd.add(e.getPermissionString());
            }

        }

        // Loop through each negated permission, check if any permissions in permsToAdd should be removed
        for (String negatedPerm : negatedPerms) {
            // Check if wildcard negated permission.
            if (negatedPerm.endsWith(".*")) {
                // Remove "-" and "*". Keep dot at end for easy indexing.
                String negatedPermClean = negatedPerm.substring(1).substring(0, negatedPerm.length() - 1);
                Iterator<String> it = permsToAdd.iterator();
                while (it.hasNext()) {
                    String permToAdd = it.next();
                    if (permToAdd.startsWith(negatedPermClean))
                        it.remove();
                }
            } else {
                // Nothing special to do, just remove the similar ones.
                Iterator<String> it = permsToAdd.iterator();
                while (it.hasNext()) {
                    String permToAdd = it.next();
                    if (permToAdd.substring(1).equalsIgnoreCase(negatedPerm))
                        it.remove();
                }
            }
        }

        Map<String, Boolean> dest = new HashMap<String, Boolean>();
        for (String perm : permsToAdd) {
            dest.put(perm, true);
            if(PowerfulPerms.debug)
                ProxyServer.getInstance().getLogger().info("Added permission " + perm + " to player " + player.getName());
        }
        for (String perm : negatedPerms) {
            dest.put(perm.substring(1), false);
        }
        this.realPermissions = dest;
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

    /**
     * Check if this player has the specified permission.
     * 
     * @param permission
     * @return
     */
    public boolean hasPermission(String permission) {
        Boolean set = realPermissions.get(permission);
        if (set != null)
            return set.booleanValue();
        return false;
    }

    /**
     * Calculate if the player should have this permission. Does not care about negated permissions. Simply checks if player is same server.
     */
    private boolean permissionApplies(PowerfulPermission e, ProxiedPlayer player) {
        boolean isSameServer = false;

        if (e.getServer().isEmpty() || 
                e.getServer().equalsIgnoreCase("ALL") || 
                e.getServer().equals((player.getServer() != null ? (player.getServer().getInfo() != null ? player.getServer().getInfo().getName() : null) : null)))
            isSameServer = true;

        return isSameServer;
    }
}
