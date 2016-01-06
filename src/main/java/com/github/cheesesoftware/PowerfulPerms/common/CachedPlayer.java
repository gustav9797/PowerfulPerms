package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.List;

public class CachedPlayer {

    private String groups;
    private String prefix;
    private String suffix;
    private List<PowerfulPermission> perms;

    public CachedPlayer(String groups, String prefix, String suffix, List<PowerfulPermission> perms) {
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

    public List<PowerfulPermission> getPermissions() {
        return perms;
    }
}
