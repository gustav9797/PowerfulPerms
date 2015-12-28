package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.HashMap;

import com.github.cheesesoftware.PowerfulPerms.PermissionManagerBase;

public class GroupLoader {

    private int total = 0;
    private int current = 0;
    private PermissionManagerBase p;
    
    public HashMap<Integer, String> tempParents;
    
    public GroupLoader(int total, PermissionManagerBase p) {
        this.total = 0;
        this.p = p;
    }
    
    public void add() {
        current++;
        if(current >= total)
            p.continueLoadGroups(this);
    }
}
