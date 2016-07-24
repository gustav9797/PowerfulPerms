package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class PermissionPlayerBase implements PermissionPlayer {

    protected LinkedHashMap<String, List<CachedGroup>> groups = new LinkedHashMap<String, List<CachedGroup>>(); // Contains -all- groups for this player.

    protected List<Group> currentGroups = new ArrayList<Group>();

    protected List<Permission> permissions = new ArrayList<Permission>();
    protected List<String> realPermissions = new ArrayList<String>();
    protected List<String> temporaryPrePermissions = new ArrayList<String>();
    protected List<String> temporaryPostPermissions = new ArrayList<String>();
    protected String prefix = "";
    protected String suffix = "";
    protected PowerfulPermsPlugin plugin;
    protected boolean isDefault = false;

    public PermissionPlayerBase(LinkedHashMap<String, List<CachedGroup>> groups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin, boolean isDefault) {
        this.groups = groups;
        this.permissions = permissions;
        this.prefix = prefix;
        this.suffix = suffix;
        this.plugin = plugin;
        this.isDefault = isDefault;
    }

    public void update(PermissionPlayerBase base) {
        this.groups = base.groups;
        this.permissions = base.permissions;
        this.prefix = base.prefix;
        this.suffix = base.suffix;
        this.isDefault = base.isDefault;
    }

    public void updateGroups(String server) {
        if (server == null || server.equalsIgnoreCase("all"))
            server = "";

        this.currentGroups = getGroups(server);
    }

    public void setGroups(LinkedHashMap<String, List<CachedGroup>> groups) {
        this.groups = groups;
    }

    public void setTemporaryPrePermissions(List<String> permissions) {
        this.temporaryPrePermissions = permissions;
    }

    public void setTemporaryPostPermissions(List<String> permissions) {
        this.temporaryPostPermissions = permissions;
    }

    /**
     * Returns all groups a player has, including primary groups, indexed by server name.
     */
    @Override
    public LinkedHashMap<String, List<CachedGroup>> getCachedGroups() {
        return new LinkedHashMap<String, List<CachedGroup>>(this.groups);
    }

    /**
     * Returns a list of cached groups which apply to a specific server.
     */
    @Override
    public List<CachedGroup> getCachedGroups(String server) {
        List<CachedGroup> tempGroups = new ArrayList<CachedGroup>();

        // Get server specific groups and add them
        List<CachedGroup> serverGroupsTemp = groups.get(server);
        if (serverGroupsTemp != null)
            tempGroups.addAll(serverGroupsTemp);

        // Get groups that apply on all servers and add them
        if (!server.isEmpty()) {
            List<CachedGroup> all = groups.get("");
            if (all != null)
                tempGroups.addAll(all);
        }

        return tempGroups;
    }

    /**
     * Returns a list of groups which apply to a specific server. Removes negated groups.
     */
    @Override
    public List<Group> getGroups(String server) {
        List<CachedGroup> tempGroups = getCachedGroups(server);
        List<Group> output = new ArrayList<Group>();

        Iterator<CachedGroup> it1 = tempGroups.iterator();
        while (it1.hasNext()) {
            CachedGroup cachedGroup = it1.next();
            if (!cachedGroup.isNegated()) {
                output.add(cachedGroup.getGroup());
                it1.remove();
            }
        }

        // Remaining groups are negated
        for (CachedGroup cachedGroup : tempGroups) {
            Iterator<Group> it2 = output.iterator();
            while (it2.hasNext()) {
                Group temp = it2.next();
                if (temp.getId() == cachedGroup.getGroup().getId()) {
                    it2.remove();
                    plugin.debug("Removed negated group " + temp.getId());
                }
            }
        }
        return output;
    }

    /**
     * Returns a list of groups which apply to the current server.
     */
    @Override
    public List<Group> getGroups() {
        return new ArrayList<Group>(this.currentGroups);
    }

    /**
     * Returns all permissions for this player.
     */
    @Override
    public List<Permission> getPermissions() {
        return new ArrayList<Permission>(this.permissions);
    }

    /**
     * Returns all permissions in effect for this player.
     */
    @Override
    public List<String> getPermissionsInEffect() {
        return new ArrayList<String>(this.realPermissions);
    }

    @Override
    public boolean isPermissionSet(String permission) {
        return preHasPermission(permission) != null;
    }

    private Boolean preHasPermission(String permission) {
        Boolean has = null;

        List<String> lperm = Utils.toList(permission, ".");

        if (temporaryPrePermissions != null) {
            for (String p : temporaryPrePermissions) {
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

        if (temporaryPostPermissions != null) {
            for (String p : temporaryPostPermissions) {
                Boolean check = internalPermissionCheck(permission, p, lperm);
                if (check != null)
                    has = check;
            }
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
            List<String> lp = Utils.toList(toCheckAgainst, ".");
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
                    // plugin.debug("wildcard perm check: has = " + has + " toCheckAgainst = " + toCheckAgainst);
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
    @Override
    public Boolean hasPermission(String permission) {
        return preHasPermission(permission);
    }

    /**
     * Returns the player's group on a specific ladder.
     */
    @Override
    public Group getGroup(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>();
        // Sort groups by rank if same ladder
        for (Group group : input) {
            if (group.getLadder().equalsIgnoreCase(ladder)) {
                sortedGroups.put(group.getRank(), group);
            }
        }

        Iterator<Group> it = sortedGroups.descendingMap().values().iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    /**
     * Returns the player's group on the default ladder.
     */
    @Override
    public Group getGroup() {
        return getGroup("default");
    }

    /**
     * Returns the player's prefix on a specific ladder.
     */
    @Override
    public String getPrefix(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>();
        // Sort groups by rank if same ladder
        for (Group group : input) {
            if (group.getLadder().equalsIgnoreCase(ladder)) {
                sortedGroups.put(group.getRank(), group);
            }
        }

        // Return prefix from group with highest rank, if not found, move on to next rank
        Iterator<Group> it = sortedGroups.descendingMap().values().iterator();
        while (it.hasNext()) {
            Group group = it.next();
            String prefix = group.getPrefix(PermissionManagerBase.serverName);
            if (!prefix.isEmpty())
                return prefix;
        }
        return null;
    }

    /**
     * Returns the player's suffix on a specific ladder.
     */
    @Override
    public String getSuffix(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, Group> sortedGroups = new TreeMap<Integer, Group>();
        // Sort groups by rank if same ladder
        for (Group group : input) {
            if (group.getLadder().equalsIgnoreCase(ladder)) {
                sortedGroups.put(group.getRank(), group);
            }
        }

        // Return prefix from group with highest rank, if not found, move on to next rank
        Iterator<Group> it = sortedGroups.descendingMap().values().iterator();
        while (it.hasNext()) {
            Group group = it.next();
            String suffix = group.getSuffix(PermissionManagerBase.serverName);
            if (!suffix.isEmpty())
                return suffix;
        }
        return null;
    }

    /**
     * Returns the player's default prefix. Uses group rank.
     */
    @Override
    public String getPrefix() {
        if (!prefix.isEmpty())
            return prefix;
        return getPrefix("default");
    }

    /**
     * Returns the player's default suffix. Uses group rank.
     */
    @Override
    public String getSuffix() {
        if (!suffix.isEmpty())
            return suffix;
        return getSuffix("default");
    }

    /**
     * Returns the player's own prefix.
     */
    @Override
    public String getOwnPrefix() {
        return prefix;
    }

    /**
     * Returns the player's own suffix.
     */
    @Override
    public String getOwnSuffix() {
        return suffix;
    }

    protected List<String> calculatePermissions(String playerServer, String playerWorld) {
        ArrayList<Permission> unprocessedPerms = new ArrayList<Permission>();

        // Add permissions from groups in reverse order.
        plugin.debug("current groups count " + currentGroups.size());
        List<Group> input = getGroups();
        ListIterator<Group> it = input.listIterator(input.size());
        while (it.hasPrevious()) {
            Group group = it.previous();
            unprocessedPerms.addAll(group.getPermissions());
        }

        // Add own permissions.
        unprocessedPerms.addAll(this.permissions);

        List<String> output = new ArrayList<String>();

        for (Permission e : unprocessedPerms) {
            if (permissionApplies(e, playerServer, playerWorld)) {
                output.add(e.getPermissionString());
            }
        }

        if (plugin.isDebug()) {
            Iterator<String> it2 = output.iterator();
            while (it2.hasNext()) {
                String perm = it2.next();
                plugin.debug("base added perm " + perm);
            }
        }

        return output;
    }

    /**
     * Checks if permission applies for server and world.
     */
    private static boolean permissionApplies(Permission e, String playerServer, String playerWorld) {
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

    @Override
    public boolean isDefault() {
        return isDefault;
    }

}
