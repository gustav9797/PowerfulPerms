package com.github.gustav9797.PowerfulPerms.common;

import java.util.LinkedHashMap;
import java.util.List;

import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Permission;

public class CachedPlayer {

    private LinkedHashMap<String, List<CachedGroup>> groups;
    private String prefix;
    private String suffix;
    private List<Permission> perms;

    public CachedPlayer(LinkedHashMap<String, List<CachedGroup>> groups, String prefix, String suffix, List<Permission> perms) {
        this.groups = groups;
        this.prefix = prefix;
        this.suffix = suffix;
        this.perms = perms;
    }

    public CachedPlayer() {

    }

    public LinkedHashMap<String, List<CachedGroup>> getGroups() {
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
