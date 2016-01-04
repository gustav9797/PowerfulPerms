package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface IPermissionsPlayer {

    public void update(PermissionsPlayerBase base);
    
    public HashMap<String, Group> getPrimaryGroups();
    
    public Group getPrimaryGroup(String server);

    public Group getPrimaryGroup();

    public HashMap<String, List<CachedGroup>> getCachedGroups();
    
    public List<CachedGroup> getCachedGroups(String server);
    
    public List<Group> getGroups(String server);
    
    public ArrayList<PowerfulPermission> getPermissions();
    
    public List<String> getPermissionsInEffect();

    public Boolean hasPermission(String permission);

    public boolean isPermissionSet(String permission);
    
    public String getPrefix(String server);
    
    public String getSuffix(String server);

    public String getPrefix();

    public String getSuffix();
    
    public String getOwnPrefix();
    
    public String getOwnSuffix();
}
