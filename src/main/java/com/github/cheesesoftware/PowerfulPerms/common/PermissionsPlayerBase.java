package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class PermissionsPlayerBase implements IPermissionsPlayer {

    protected HashMap<String, List<CachedGroup>> groups = new HashMap<String, List<CachedGroup>>(); // Contains -all- groups for this player.

    protected List<Group> currentGroups = new ArrayList<Group>();
    protected Group currentPrimaryGroup = null;

    protected ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    protected List<String> realPermissions = new ArrayList<String>();
    protected List<String> temporaryPermissions = new ArrayList<String>();
    protected String prefix = "";
    protected String suffix = "";
    protected IPlugin plugin;

    public PermissionsPlayerBase(HashMap<String, List<CachedGroup>> groups, ArrayList<PowerfulPermission> permissions, String prefix, String suffix, IPlugin plugin) {
        this.groups = groups;
        this.permissions = permissions;
        this.prefix = prefix;
        this.suffix = suffix;
        this.plugin = plugin;
    }

    public void update(PermissionsPlayerBase base) {
        this.groups = base.groups;
        this.permissions = base.permissions;
        this.prefix = base.prefix;
        this.suffix = base.suffix;
    }

    public void updateGroups(String server) {
        if (server.equalsIgnoreCase("all"))
            server = "";

        this.currentGroups = getGroups(server);
        this.currentPrimaryGroup = this.getPrimaryGroup(server);
    }

    /**
     * Sets the player's groups as seen in getServerGroups() Changes won't save.
     */
    public void setGroups(HashMap<String, List<CachedGroup>> groups) {
        this.groups = groups;
    }

    public void setTemporaryPermissions(List<String> permissions) {
        this.temporaryPermissions = permissions;
    }

    /**
     * Returns all primary groups a player has, indexed by server name.
     */
    @Override
    public HashMap<String, Group> getPrimaryGroups() {
        HashMap<String, Group> tempPrimaryGroups = new HashMap<String, Group>();
        for (Entry<String, List<CachedGroup>> entry : groups.entrySet()) {
            for (CachedGroup cachedGroup : entry.getValue()) {
                if (cachedGroup.isPrimary())
                    tempPrimaryGroups.put(entry.getKey(), cachedGroup.getGroup());
            }
        }
        return tempPrimaryGroups;
    }

    /**
     * Returns the primary group for a specific server.
     */
    @Override
    public Group getPrimaryGroup(String server) {
        HashMap<String, Group> primaryGroups = this.getPrimaryGroups();
        Group primary = primaryGroups.get(server);
        if (primary != null)
            return primary;
        Group second = primaryGroups.get("");
        if (second != null)
            return second;
        // Has no primary groups, use old system
        List<CachedGroup> temp = groups.get("");
        if (temp != null) {
            Iterator<CachedGroup> it = temp.iterator();
            Group group = it.next().getGroup();
            if (group != null)
                plugin.debug("Database syntax for player is old. Setting " + group.getName() + " as current primary.");
            return group;
        }
        return null;
    }

    /**
     * Returns the primary group for the current server.
     */
    @Override
    public Group getPrimaryGroup() {
        return this.currentPrimaryGroup;
    }

    /**
     * Returns all groups a player has, including primary groups, indexed by server name.
     */
    @Override
    public HashMap<String, List<CachedGroup>> getCachedGroups() {
        return this.groups;
    }

    /**
     * Returns a list of cached groups including primary groups which apply to a specific server.
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
     * Returns a list of groups including primary groups which apply to a specific server.
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
     * Returns all permissions for this player.
     */
    @Override
    public ArrayList<PowerfulPermission> getPermissions() {
        return this.permissions;
    }

    /**
     * Returns all permissions in effect for this player.
     */
    @Override
    public List<String> getPermissionsInEffect() {
        return this.realPermissions;
    }

    @Override
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
     * Returns the player's prefix. If player has no prefix set, return the prefix of the primary group.
     * 
     * @return The prefix.
     */
    @Override
    public String getPrefix(String server) {
        Group group = getPrimaryGroup();
        if (!prefix.isEmpty())
            return prefix;
        if (group != null) {
            String groupPrefix = group.getPrefix(server);
            if (groupPrefix != null)
                return groupPrefix;
        }
        return "";
    }

    /**
     * Returns the player's suffix. If player has no suffix set, return the suffix of the primary group.
     * 
     * @return The suffix.
     */
    @Override
    public String getSuffix(String server) {
        Group group = getPrimaryGroup();
        if (!suffix.isEmpty())
            return suffix;
        if (group != null) {
            String groupSuffix = group.getSuffix(server);
            if (groupSuffix != null)
                return groupSuffix;
        }
        return "";
    }

    /**
     * Returns the player's prefix. If player has no prefix set, return the prefix of the primary group.
     * 
     * @return The prefix.
     */
    @Override
    public String getPrefix() {
        return getPrefix(PermissionManagerBase.serverName);
    }

    /**
     * Returns the player's suffix. If player has no suffix set, return the suffix of the primary group.
     * 
     * @return The suffix.
     */
    @Override
    public String getSuffix() {
        return getSuffix(PermissionManagerBase.serverName);
    }

    /**
     * Returns the player's own prefix.
     * 
     * @return The prefix.
     */
    @Override
    public String getOwnPrefix() {
        return prefix;
    }

    /**
     * Returns the player's own suffix.
     * 
     * @return The suffix.
     */
    @Override
    public String getOwnSuffix() {
        return suffix;
    }

    protected List<String> calculatePermissions(String playerServer, String playerWorld) {
        ArrayList<PowerfulPermission> unprocessedPerms = new ArrayList<PowerfulPermission>();

        Group primary = this.getPrimaryGroup();

        // Add permissions derived from groups. Not from groups same as primary.
        plugin.debug("current groups count " + currentGroups.size());
        for (Group group : currentGroups) {
            if (group != null && primary != null && group.getId() != primary.getId()) {
                unprocessedPerms.addAll(group.getPermissions());
            }
        }

        // Add permissions from primary group and parents.
        if (primary != null)
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
                plugin.debug("base added perm " + perm);
            }
        }

        return output;
    }

    /**
     * Checks if permission applies for server and world.
     */
    private static boolean permissionApplies(PowerfulPermission e, String playerServer, String playerWorld) {
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
