package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class PowerfulGroup implements Group {

    private int id;
    private String name;
    private List<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private List<Integer> parents;
    private String ladder;
    private int rank;

    private PowerfulPermsPlugin plugin;

    private HashMap<String, String> serverPrefix = new HashMap<String, String>();
    private HashMap<String, String> serverSuffix = new HashMap<String, String>();

    public PowerfulGroup(int id, String name, List<PowerfulPermission> permissions, HashMap<String, String> prefixes, HashMap<String, String> suffixes, String ladder, int rank,
            PowerfulPermsPlugin plugin) {
        this.id = id;
        this.name = name;
        this.permissions = (permissions != null ? permissions : new ArrayList<PowerfulPermission>());
        this.serverPrefix = (prefixes != null ? prefixes : new HashMap<String, String>());
        this.serverSuffix = (suffixes != null ? suffixes : new HashMap<String, String>());
        this.ladder = ladder;
        this.rank = rank;
        this.plugin = plugin;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Group> getParents() {
        List<Group> out = new ArrayList<Group>();
        for (int groupId : parents) {
            Group group = plugin.getPermissionManager().getGroup(groupId);
            if (group != null)
                out.add(group);
            else
                plugin.debug("Could not add parent group " + groupId + " to group " + this.id);
        }
        return out;
    }

    @Override
    public String getPrefix(String server) {
        String prefix = serverPrefix.get(server);
        if (prefix != null)
            return prefix;
        prefix = serverPrefix.get("");
        return (prefix != null ? prefix : "");
    }

    @Override
    public String getSuffix(String server) {
        String suffix = serverSuffix.get(server);
        if (suffix != null)
            return suffix;
        suffix = serverSuffix.get("");
        return (suffix != null ? suffix : "");
    }

    @Override
    public HashMap<String, String> getPrefixes() {
        return this.serverPrefix;
    }

    @Override
    public HashMap<String, String> getSuffixes() {
        return this.serverSuffix;
    }

    @Override
    public ArrayList<Permission> getOwnPermissions() {
        return new ArrayList<Permission>(permissions);
    }

    @Override
    public ArrayList<Permission> getPermissions() {
        ArrayList<Permission> output = new ArrayList<Permission>();

        if (this.parents.size() > 1) {
            // Sort parents
            TreeMap<Integer, List<Group>> sortedParents = new TreeMap<Integer, List<Group>>();
            for (Integer parent : this.parents) {
                Group group = plugin.getPermissionManager().getGroup(parent);
                if (group != null) {
                    List<Group> temp = sortedParents.get(group.getRank());
                    if (temp == null)
                        temp = new ArrayList<Group>();
                    temp.add(group);
                    sortedParents.put(group.getRank(), temp);
                } else
                    plugin.debug("Could not use parent group " + parent + " to group " + this.id);
            }

            // Add permissions from sorted parents
            for (List<Group> tempGroups : sortedParents.values()) {
                for (Group group : tempGroups) {
                    output.addAll(group.getPermissions());
                }
            }
        } else {
            // Add permissions from parents
            for (Integer parent : this.parents) {
                Group parentGroup = plugin.getPermissionManager().getGroup(parent);
                if (parentGroup != null)
                    output.addAll(parentGroup.getPermissions());
                else
                    plugin.debug("Could not use parent group " + parent + " to group " + this.id);
            }
        }
        // Add own permissions
        output.addAll(permissions);
        return output;
    }

    @Override
    public String getLadder() {
        return this.ladder;
    }

    @Override
    public int getRank() {
        return this.rank;
    }

    @Override
    public void setParents(List<Integer> parents) {
        this.parents = (parents != null ? parents : new ArrayList<Integer>());
    }

}
