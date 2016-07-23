package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;

public class PowerfulGroup implements Group {

    private int id;
    private String name;
    private List<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private List<Integer> parents;
    private String ladder;
    private int rank;

    private PermissionManager permissionManager;

    private HashMap<String, String> serverPrefix = new HashMap<String, String>();
    private HashMap<String, String> serverSuffix = new HashMap<String, String>();

    public PowerfulGroup(int id, String name, List<PowerfulPermission> permissions, HashMap<String, String> prefixes, HashMap<String, String> suffixes, String ladder, int rank,
            PermissionManager permissionManager) {
        this.id = id;
        this.name = name;
        this.permissions = (permissions != null ? permissions : new ArrayList<PowerfulPermission>());
        this.serverPrefix = (prefixes != null ? prefixes : new HashMap<String, String>());
        this.serverSuffix = (suffixes != null ? suffixes : new HashMap<String, String>());
        this.ladder = ladder;
        this.rank = rank;
        this.permissionManager = permissionManager;
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
            out.add(permissionManager.getGroup(groupId));
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
        ArrayList<Permission> temp = new ArrayList<Permission>();
        for (Integer parent : this.parents) {
            Group parentGroup = permissionManager.getGroup(parent);
            temp.addAll(parentGroup.getPermissions());
        }
        temp.addAll(permissions);
        return temp;
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
