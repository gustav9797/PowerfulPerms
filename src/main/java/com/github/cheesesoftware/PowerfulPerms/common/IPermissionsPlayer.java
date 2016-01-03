package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface IPermissionsPlayer {

    public void update(PermissionsPlayerBase base);

    public Group getPrimaryGroup();

    public List<Group> getApplyingGroups(String server);

    public HashMap<String, List<Group>> getServerGroups();

    public String getRawServerGroups();

    public void setServerGroups(HashMap<String, List<Group>> serverGroups);
    
    public ArrayList<PowerfulPermission> getPermissions();

    public Boolean hasPermission(String permission);

    public boolean isPermissionSet(String permission);

    public String getPrefix();

    public String getSuffix();
}
