package com.github.cheesesoftware.PowerfulPerms.common;

public class CachedGroup {
    private Group group;
    private boolean primary;
    private boolean negated;
    
    public CachedGroup(Group group, boolean primary, boolean negated) {
        this.group = group;
        this.primary = primary;
        this.negated = negated;
    }
    
    public Group getGroup() {
        return this.group;
    }
    
    public boolean isPrimary() {
        return this.primary;
    }
    
    public boolean isNegated() {
        return this.negated;
    }
}
