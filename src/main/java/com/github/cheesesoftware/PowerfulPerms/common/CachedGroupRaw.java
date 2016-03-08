package com.github.cheesesoftware.PowerfulPerms.common;

public class CachedGroupRaw {
    private int groupId;
    private boolean negated;

    public CachedGroupRaw(int groupId, boolean negated) {
        this.groupId = groupId;
        this.negated = negated;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public boolean isNegated() {
        return this.negated;
    }
}
