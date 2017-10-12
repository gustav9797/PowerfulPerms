package com.github.gustav9797.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class PermissionPlayerBase extends PermissionContainer implements PermissionPlayer {

    protected LinkedHashMap<String, List<CachedGroup>> groups = new LinkedHashMap<>(); // Contains -all- groups for this player.

    protected List<Group> currentGroups = new ArrayList<>();

    protected String prefix = "";
    protected String suffix = "";
    protected static PowerfulPermsPlugin plugin;
    protected boolean isDefault = false;

    protected ReentrantLock asyncGroupLock = new ReentrantLock();

    public PermissionPlayerBase(LinkedHashMap<String, List<CachedGroup>> groups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin, boolean isDefault) {
        super(permissions);
        this.groups = groups;
        this.prefix = prefix;
        this.suffix = suffix;
        PermissionPlayerBase.plugin = plugin;
        this.isDefault = isDefault;
    }

    public void update(PermissionPlayerBase base) {
        this.groups = base.groups;
        this.ownPermissions = base.ownPermissions;
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
        asyncGroupLock.lock();
        try {
            this.groups = groups;
        } finally {
            asyncGroupLock.unlock();
        }
    }

    /**
     * Returns all groups a player has, including primary groups, indexed by server name.
     */
    @Override
    public LinkedHashMap<String, List<CachedGroup>> getCachedGroups() {
        LinkedHashMap<String, List<CachedGroup>> output = new LinkedHashMap<>();
        asyncGroupLock.lock();
        try {
            for (Entry<String, List<CachedGroup>> entry : this.groups.entrySet()) {
                output.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        } finally {
            asyncGroupLock.unlock();
        }
        return output;
    }

    public static List<CachedGroup> getCachedGroups(String server, LinkedHashMap<String, List<CachedGroup>> groups) {
        List<CachedGroup> tempGroups = new ArrayList<>();

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
     * Returns a list of cached groups which apply to a specific server.
     */
    @Override
    public List<CachedGroup> getCachedGroups(String server) {
        asyncGroupLock.lock();
        try {
            return getCachedGroups(server, this.groups);
        } finally {
            asyncGroupLock.unlock();
        }
    }

    public static List<Group> getGroups(List<CachedGroup> groups, PowerfulPermsPlugin plugin) {
        List<Group> output = new ArrayList<>();

        Iterator<CachedGroup> it1 = groups.iterator();
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
        for (CachedGroup cachedGroup : groups) {
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
     * Returns a list of groups which apply to a specific server. Removes negated groups.
     */
    @Override
    public List<Group> getGroups(String server) {
        return getGroups(getCachedGroups(server), plugin);
    }

    /**
     * Returns a list of groups which apply to the current server.
     */
    @Override
    public List<Group> getGroups() {
        return new ArrayList<>(this.currentGroups);
    }

    /**
     * Returns the player's group on a specific ladder.
     */
    @Override
    public Group getGroup(String ladder) {
        List<Group> input = getGroups();
        TreeMap<Integer, Group> sortedGroups = new TreeMap<>();
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

    public static Group getPrimaryGroup(List<Group> input) {
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<>();

        for (Group group : input) {
            List<Group> temp = sortedGroups.get(group.getRank());
            if (temp == null)
                temp = new ArrayList<>();
            temp.add(group);
            sortedGroups.put(group.getRank(), temp);
        }

        for (List<Group> tempGroups : sortedGroups.descendingMap().values()) {
            for (Group group : tempGroups) {
                if (group != null)
                    return group;
            }
        }
        return null;
    }

    /**
     * Returns the group with highest rank value across all ladders of the player.
     */
    @Override
    public Group getPrimaryGroup() {
        return getPrimaryGroup(getGroups());
    }

    public static String getPrefix(String ladder, List<Group> input) {
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<>();

        // Insert groups by rank value
        for (Group group : input) {
            if (ladder == null || group.getLadder().equalsIgnoreCase(ladder)) {
                List<Group> temp = sortedGroups.get(group.getRank());
                if (temp == null)
                    temp = new ArrayList<>();
                temp.add(group);
                sortedGroups.put(group.getRank(), temp);
            }
        }

        // Return prefix from group with highest rank, if not found, move on to next rank
        for (List<Group> tempGroups : sortedGroups.descendingMap().values()) {
            for (Group group : tempGroups) {
                String prefix = group.getPrefix(PermissionManagerBase.serverName);
                if (!prefix.isEmpty())
                    return prefix;
            }
        }
        return null;
    }

    /**
     * Returns the player's prefix on a specific ladder.
     */
    @Override
    public String getPrefix(String ladder) {
        return getPrefix(ladder, getGroups());
    }

    public static String getSuffix(String ladder, List<Group> input) {
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<>();

        // Insert groups by rank value
        for (Group group : input) {
            if (ladder == null || group.getLadder().equalsIgnoreCase(ladder)) {
                List<Group> temp = sortedGroups.get(group.getRank());
                if (temp == null)
                    temp = new ArrayList<>();
                temp.add(group);
                sortedGroups.put(group.getRank(), temp);
            }
        }

        // Return suffix from group with highest rank, if not found, move on to next rank
        for (List<Group> tempGroups : sortedGroups.descendingMap().values()) {
            for (Group group : tempGroups) {
                String suffix = group.getSuffix(PermissionManagerBase.serverName);
                if (!suffix.isEmpty())
                    return suffix;
            }
        }
        return null;
    }

    /**
     * Returns the player's suffix on a specific ladder.
     */
    @Override
    public String getSuffix(String ladder) {
        return getSuffix(ladder, getGroups());
    }

    public static String getPrefix(List<Group> input, String ownPrefix) {
        if (!ownPrefix.isEmpty())
            return ownPrefix;
        return getPrefix(null, input);
    }

    /**
     * Returns the player's prefix from the group with highest rank across all ladders.
     */
    @Override
    public String getPrefix() {
        if (!prefix.isEmpty())
            return prefix;
        return getPrefix(null, getGroups());
    }

    public static String getSuffix(List<Group> input, String ownSuffix) {
        if (!ownSuffix.isEmpty())
            return ownSuffix;
        return getSuffix(null, input);
    }

    /**
     * Returns the player's suffix from the group with highest rank across all ladders.
     */
    @Override
    public String getSuffix() {
        if (!suffix.isEmpty())
            return suffix;
        return getSuffix(null, getGroups());
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

    public static List<Permission> getAllPermissions(List<Group> input, PermissionContainer out, PowerfulPermsPlugin plugin) {
        ArrayList<Permission> unprocessedPerms = new ArrayList<>();

        // Add permissions from groups in normal order.
        plugin.debug("groups count " + input.size());
        TreeMap<Integer, List<Group>> sortedGroups = new TreeMap<>();

        // Insert groups by rank value
        for (Group group : input) {
            List<Group> temp = sortedGroups.get(group.getRank());
            if (temp == null)
                temp = new ArrayList<>();
            temp.add(group);
            sortedGroups.put(group.getRank(), temp);
        }

        // Add permissions from sorted groups
        for (List<Group> tempGroups : sortedGroups.values()) {
            for (Group group : tempGroups) {
                unprocessedPerms.addAll(group.getPermissions());
            }
        }

        // Add own permissions.
        unprocessedPerms.addAll(out.ownPermissions);
        return unprocessedPerms;
    }

    public static List<String> calculatePermissions(String playerServer, String playerWorld, List<Group> input, PermissionContainer out, PowerfulPermsPlugin plugin) {
        return calculatePermissions(playerServer, playerWorld, input, out, getAllPermissions(input, out, plugin), plugin);
    }

    public static List<String> calculatePermissions(String playerServer, String playerWorld, List<Group> input, PermissionContainer out, List<Permission> unprocessedPerms,
            PowerfulPermsPlugin plugin) {
        List<String> output = new ArrayList<>();

        for (Permission e : unprocessedPerms) {
            if (PermissionContainer.permissionApplies(e, playerServer, playerWorld)) {
                output.add(e.getPermissionString());
            }
        }

        if (plugin.isDebug()) {
            for (String perm : output) {
                plugin.debug("base added perm " + perm);
            }
        }

        return output;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

}
