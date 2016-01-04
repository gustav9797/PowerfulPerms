package com.github.cheesesoftware.PowerfulPerms.common;

public class CachedGroupRaw {
    private int groupId;
    private boolean primary;
    private boolean negated;
    
    public CachedGroupRaw(int groupId, boolean primary, boolean negated) {
        this.groupId = groupId;
        this.primary = primary;
        this.negated = negated;
    }
    
    public int getGroupId() {
        return this.groupId;
    }
    
    public boolean isPrimary() {
        return this.primary;
    }
    
    public boolean isNegated() {
        return this.negated;
    }
}
