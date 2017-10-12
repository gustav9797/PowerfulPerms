package com.github.gustav9797.PowerfulPermsAPI;

import java.util.LinkedHashMap;
import java.util.List;

public interface PermissionPlayer {

    public LinkedHashMap<String, List<CachedGroup>> getCachedGroups();

    public List<CachedGroup> getCachedGroups(String server);

    public List<Group> getGroups(String server);

    public List<Group> getGroups();

    public List<Permission> getPermissions();

    public List<String> getPermissionsInEffect();

    public List<Permission> getAllPermissions();

    public Boolean hasPermission(String permission);

    public boolean isPermissionSet(String permission);

    public Group getGroup(String ladder);

    public Group getPrimaryGroup();

    public String getPrefix(String ladder);

    public String getSuffix(String ladder);

    public String getPrefix();

    public String getSuffix();

    public String getOwnPrefix();

    public String getOwnSuffix();

    public boolean isDefault();

}
