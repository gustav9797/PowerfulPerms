package com.github.cheesesoftware.PowerfulPerms.common;

public class CachedGroupRaw {
    private int groupId;
    private boolean primary;
    private boolean secondary;
    private boolean negated;

    public CachedGroupRaw(int groupId, boolean primary, boolean secondary, boolean negated) {
        this.groupId = groupId;
        this.primary = primary;
        this.secondary = secondary;
        this.negated = negated;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    public boolean isSecondary() {
        return this.secondary;
    }

    public boolean isNegated() {
        return this.negated;
    }
}
