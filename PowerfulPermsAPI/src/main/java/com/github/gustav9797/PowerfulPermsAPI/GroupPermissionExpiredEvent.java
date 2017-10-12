package com.github.gustav9797.PowerfulPermsAPI;

public class GroupPermissionExpiredEvent extends Event {
    private final Group group;
    private final Permission permission;

    public GroupPermissionExpiredEvent(Group group, Permission permission) {
        this.group = group;
        this.permission = permission;
    }

    public Group getGroup() {
        return this.group;
    }

    public Permission getPermission() {
        return this.permission;
    }
}
