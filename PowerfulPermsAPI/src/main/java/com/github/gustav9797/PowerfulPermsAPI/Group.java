package com.github.gustav9797.PowerfulPermsAPI;

import java.util.HashMap;
import java.util.List;

public interface Group {

    public int getId();

    public String getName();

    public List<Group> getParents();

    public String getPrefix(String server);

    public String getSuffix(String server);

    public HashMap<String, String> getPrefixes();

    public HashMap<String, String> getSuffixes();

    public List<Permission> getOwnPermissions();

    public List<Permission> getPermissions();

    public String getLadder();

    public int getRank();

    public void setParents(List<Integer> parents);

}