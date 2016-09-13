package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

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
    protected ReentrantLock asyncPermLock = new ReentrantLock();

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
        asyncPermLock.lock();
        try {
            this.temporaryPrePermissions = new ArrayList<String>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    public void setTemporaryPostPermissions(List<String> permissions) {
        asyncPermLock.lock();
        try {
            this.temporaryPostPermissions = new ArrayList<String>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
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
                Group group = plugin.getPermissionManager().getGroup(cachedGroup.getGroupId());
                if (group != null) {
                    output.add(group);
                    it1.remove();
                }
            }
        }

        // Remaining groups are negated
        for (CachedGroup cachedGroup : tempGroups) {
            Iterator<Group> it2 = output.iterator();
            while (it2.hasNext()) {
                Group temp = it2.next();
                if (temp.getId() == cachedGroup.getGroupId()) {
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
        asyncPermLock.lock();
        try {
            return new ArrayList<String>(this.realPermissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    @Override
    public boolean isPermissionSet(String permission) {
        return preHasPermission(permission) != null;
    }

    private Boolean preHasPermission(String permission) {
        Boolean has = null;

        asyncPermLock.lock();
        try {
            if (temporaryPrePermissions != null) {
                ListIterator<String> it = temporaryPrePermissions.listIterator(temporaryPrePermissions.size());
                while (it.hasPrevious()) {
                    Boolean check = internalPermissionCheck(permission, it.previous());
                    if (check != null) {
                        has = check;
                        break;
                    }
                }
            }

            ListIterator<String> it = realPermissions.listIterator(realPermissions.size());
            while (it.hasPrevious()) {
                Boolean check = internalPermissionCheck(permission, it.previous());
                if (check != null) {
                    has = check;
                    break;
                }
            }

            if (temporaryPostPermissions != null) {
                it = temporaryPostPermissions.listIterator(temporaryPostPermissions.size());
                while (it.hasPrevious()) {
                    Boolean check = internalPermissionCheck(permission, it.previous());
                    if (check != null) {
                        has = check;
                        break;
                    }
                }
            }
        } finally {
            asyncPermLock.unlock();
        }

        return has;
    }

    private Boolean internalPermissionCheck(String toCheck, String ownPermission) {
        Boolean has = null;
        if (ownPermission.equalsIgnoreCase(toCheck)) {
            has = true;
        } else if (ownPermission.equalsIgnoreCase("-" + toCheck)) {
            has = false;
        } else if (ownPermission.endsWith("*")) {

            boolean ownNegated = ownPermission.startsWith("-");
            int ownOffset = (ownNegated ? 1 : 0);
            int i = 0;
            for (; i + ownOffset < ownPermission.length() && i < toCheck.length();) {
                if (ownPermission.charAt(i + ownOffset) == toCheck.charAt(i)) {
                    // plugin.debug(ownPermission.charAt(i + ownOffset) + " " + toCheck.charAt(i) + " " + i + " + 1");
                    ++i;
                } else
                    break;
            }

            // plugin.debug("ownPermission " + ownPermission);
            // plugin.debug("toCheck " + toCheck);
            // plugin.debug("ownPermission " + ownPermission.length());
            // plugin.debug("toCheck " + toCheck.length());
            // plugin.debug("i " + i);

            if (ownPermission.charAt(i + ownOffset) == '*') {
                has = !ownNegated;
                // plugin.debug("wildcard perm check: has = " + has + " ownPermission = " + ownPermission);
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
     * Returns the group with highest rank value across all ladders of the player.
     */
    @Override
    public Group getPrimaryGroup() {
        List<Group> input = getGroups();
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<Integer, List<Group>>();

        for (Group group : input) {
            List<Group> temp = sortedGroups.get(group.getRank());
            if (temp == null)
                temp = new ArrayList<Group>();
            temp.add(group);
            sortedGroups.put(group.getRank(), temp);
        }

        Iterator<List<Group>> it = sortedGroups.descendingMap().values().iterator();
        while (it.hasNext()) {
            List<Group> tempGroups = it.next();
            Iterator<Group> it2 = tempGroups.iterator();
            while (it2.hasNext()) {
                Group group = it2.next();
                if (group != null)
                    return group;
            }
        }
        return null;
    }

    /**
     * Returns the player's prefix on a specific ladder.
     */
    @Override
    public String getPrefix(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<Integer, List<Group>>();

        // Insert groups by rank value
        for (Group group : input) {
            if (ladder == null || group.getLadder().equalsIgnoreCase(ladder)) {
                List<Group> temp = sortedGroups.get(group.getRank());
                if (temp == null)
                    temp = new ArrayList<Group>();
                temp.add(group);
                sortedGroups.put(group.getRank(), temp);
            }
        }

        // Return prefix from group with highest rank, if not found, move on to next rank
        Iterator<List<Group>> it = sortedGroups.descendingMap().values().iterator();
        while (it.hasNext()) {
            List<Group> tempGroups = it.next();
            Iterator<Group> it2 = tempGroups.iterator();
            while (it2.hasNext()) {
                Group group = it2.next();
                String prefix = group.getPrefix(PermissionManagerBase.serverName);
                if (!prefix.isEmpty())
                    return prefix;
            }
        }
        return null;
    }

    /**
     * Returns the player's suffix on a specific ladder.
     */
    @Override
    public String getSuffix(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<Integer, List<Group>>();

        // Insert groups by rank value
        for (Group group : input) {
            if (ladder == null || group.getLadder().equalsIgnoreCase(ladder)) {
                List<Group> temp = sortedGroups.get(group.getRank());
                if (temp == null)
                    temp = new ArrayList<Group>();
                temp.add(group);
                sortedGroups.put(group.getRank(), temp);
            }
        }

        // Return suffix from group with highest rank, if not found, move on to next rank
        Iterator<List<Group>> it = sortedGroups.descendingMap().values().iterator();
        while (it.hasNext()) {
            List<Group> tempGroups = it.next();
            Iterator<Group> it2 = tempGroups.iterator();
            while (it2.hasNext()) {
                Group group = it2.next();
                String suffix = group.getSuffix(PermissionManagerBase.serverName);
                if (!suffix.isEmpty())
                    return suffix;
            }
        }
        return null;
    }

    /**
     * Returns the player's prefix from the group with highest rank across all ladders.
     */
    @Override
    public String getPrefix() {
        if (!prefix.isEmpty())
            return prefix;
        return getPrefix(null);
    }

    /**
     * Returns the player's suffix from the group with highest rank across all ladders.
     */
    @Override
    public String getSuffix() {
        if (!suffix.isEmpty())
            return suffix;
        return getSuffix(null);
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

        // Add permissions from groups in normal order.
        plugin.debug("current groups count " + currentGroups.size());
        List<Group> input = getGroups();
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<Integer, List<Group>>();

        // Insert groups by rank value
        for (Group group : input) {
            List<Group> temp = sortedGroups.get(group.getRank());
            if (temp == null)
                temp = new ArrayList<Group>();
            temp.add(group);
            sortedGroups.put(group.getRank(), temp);
        }

        // Add permissions from sorted groups
        Iterator<List<Group>> it = sortedGroups.values().iterator();
        while (it.hasNext()) {
            List<Group> tempGroups = it.next();
            Iterator<Group> it2 = tempGroups.iterator();
            while (it2.hasNext()) {
                Group group = it2.next();
                unprocessedPerms.addAll(group.getPermissions());
            }
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
