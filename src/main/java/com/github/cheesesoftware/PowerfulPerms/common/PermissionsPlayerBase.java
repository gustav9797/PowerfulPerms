package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class PermissionsPlayerBase implements IPermissionsPlayer {

    protected HashMap<String, List<Group>> serverGroups = new HashMap<String, List<Group>>(); // Contains all player main groups. Server "" is the global default group.
    protected ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    protected List<String> realPermissions = new ArrayList<String>();
    protected List<String> temporaryPermissions = new ArrayList<String>();
    protected String prefix = "";
    protected String suffix = "";
    protected IPlugin plugin;

    public PermissionsPlayerBase(HashMap<String, List<Group>> serverGroups, ArrayList<PowerfulPermission> permissions, String prefix, String suffix, IPlugin plugin) {
        this.serverGroups = serverGroups;
        this.permissions = permissions;
        this.prefix = prefix;
        this.suffix = suffix;
        this.plugin = plugin;
    }

    /**
     * Update this PermissionsPlayerBase with data from another one.
     */
    public void update(PermissionsPlayerBase base) {
        this.serverGroups = base.serverGroups;
        this.permissions = base.permissions;
        this.prefix = base.prefix;
        this.suffix = base.suffix;
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
    }

    /**
     * Returns all permissions for this player.
     */
    public ArrayList<PowerfulPermission> getPermissions() {
        return this.permissions;
    }

    /**
     * Returns all permissions in effect for this player.
     */
    public List<String> getPermissionsInEffect() {
        return this.realPermissions;
    }

    public void setTemporaryPermissions(List<String> permissions) {
        this.temporaryPermissions = permissions;
    }

    public boolean isPermissionSet(String permission) {
        return preHasPermission(permission) != null;
    }

    private Boolean preHasPermission(String permission) {
        Boolean has = null;

        List<String> lperm = toList(permission, ".");

        if (temporaryPermissions != null) {
            for (String p : temporaryPermissions) {
                Boolean check = internalPermissionCheck(permission, p, lperm);
                if (check != null)
                    has = check;
            }
        }

        for (String p : realPermissions) {
            Boolean check = internalPermissionCheck(permission, p, lperm);
            if (check != null)
                has = check;
        }

        return has;
    }

    private Boolean internalPermissionCheck(String toCheck, String toCheckAgainst, List<String> lperm) {
        Boolean has = null;
        if (toCheckAgainst.equalsIgnoreCase(toCheck)) {
            has = true;
        } else if (toCheckAgainst.equalsIgnoreCase("-" + toCheck)) {
            has = false;
        } else if (toCheckAgainst.endsWith("*")) {
            List<String> lp = toList(toCheckAgainst, ".");
            int index = 0;
            try {
                while (index < lp.size() && index < lperm.size()) {
                    if (lp.get(index).equalsIgnoreCase(lperm.get(index)) || (index == 0 && lp.get(index).equalsIgnoreCase("-" + lperm.get(index)))) {
                        index++;
                    } else {
                        break;
                    }
                }
                if (lp.get(index).equalsIgnoreCase("*") || (index == 0 && lp.get(0).equalsIgnoreCase("-*"))) {
                    has = !lp.get(0).startsWith("-");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return has;
    }

    /**
     * Check if this player has the specified permission.
     */
    public boolean hasPermission(String permission) {
        Boolean input = preHasPermission(permission);
        boolean output = false;
        if (input != null)
            output = input.booleanValue();
        return output;
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

    protected List<String> calculatePermissions(String playerServer, String playerWorld) {
        ArrayList<PowerfulPermission> unprocessedPerms = new ArrayList<PowerfulPermission>();

        Group primary = getPrimaryGroup();

        // Add permissions derived from groups.
        plugin.debug("serverGroups count " + serverGroups.size());
        for (Entry<String, List<Group>> entry : serverGroups.entrySet()) {
            //plugin.debug("playerServer: " + playerServer + " group key: " + entry.getKey());
            if (entry.getKey().isEmpty() || entry.getKey().equalsIgnoreCase("all") || entry.getKey().equals(playerServer)) {
                for (Group group : entry.getValue()) {
                    //plugin.debug("Group add permission test: ID:" + group.getId() + " Primary ID:" + primary.getId());
                    if (group != null && group.getId() != primary.getId()) {
                        unprocessedPerms.addAll(group.getPermissions());
                        //plugin.debug("Added permissions from " + group.getName());
                    }
                }
            }
        }

        // Add permissions from primary group and parents.
        unprocessedPerms.addAll(primary.getPermissions());

        // Add own permissions.
        unprocessedPerms.addAll(this.permissions);

        List<String> output = new ArrayList<String>();

        for (PowerfulPermission e : unprocessedPerms) {
            if (permissionApplies(e, playerServer, playerWorld)) {
                output.add(e.getPermissionString());
            }
        }

        if (plugin.isDebug()) {
            Iterator<String> it = output.iterator();
            while (it.hasNext()) {
                String perm = it.next();
                plugin.debug("added perm " + perm);
            }
        }

        return output;
    }

    /**
     * Calculate if the player should have this permission. Does not care about negated permissions. Simply checks if player is same server and world.
     */
    private boolean permissionApplies(PowerfulPermission e, String playerServer, String playerWorld) {
        boolean isSameServer = false;
        boolean isSameWorld = false;

        if (e.getServer().isEmpty() || e.getServer().equalsIgnoreCase("ALL") || playerServer == null || e.getServer().equals(playerServer))
            isSameServer = true;

        if (e.getWorld().isEmpty() || e.getWorld().equalsIgnoreCase("ALL") || playerWorld == null || e.getWorld().equals(playerWorld))
            isSameWorld = true;

        if (isSameServer && isSameWorld)
            return true;
        return false;
    }

    private static List<String> toList(String s, String seperator) {
        List<String> l = new ArrayList<>();
        String ls = "";
        for (int i = 0; i < (s.length() - seperator.length()) + 1; i++) {
            if (s.substring(i, i + seperator.length()).equalsIgnoreCase(seperator)) {
                l.add(ls);
                ls = "";
                i = i + seperator.length() - 1;
            } else {
                ls += s.substring(i, i + 1);
            }
        }
        if (ls.length() > 0) {
            l.add(ls);
        }
        return l;
    }

}
