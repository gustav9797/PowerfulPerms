package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.List;

import com.github.cheesesoftware.PowerfulPermsAPI.Permission;

public class CachedPlayer {

    private String groups;
    private String prefix;
    private String suffix;
    private List<Permission> perms;

    public CachedPlayer(String groups, String prefix, String suffix, List<Permission> perms) {
        this.groups = groups;
        this.prefix = prefix;
        this.suffix = suffix;
        this.perms = perms;
    }

    public CachedPlayer() {

    }

    public String getGroups() {
        return groups;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public List<Permission> getPermissions() {
        return perms;
    }
}
