package com.github.cheesesoftware.PowerfulPerms.common;

public class PowerfulPermission {
    private String permission;
    private String world = "";
    private String server = "";

    public PowerfulPermission(String permission) {
        this.permission = permission;
    }

    public PowerfulPermission(String permission, String world, String server) {
        this.permission = permission;

        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("ALL"))
            this.world = world;
        if (server != null && !server.isEmpty() && !server.equalsIgnoreCase("ALL"))
            this.server = server;
    }

    public String getPermissionString() {
        return this.permission;
    }

    public String getWorld() {
        return this.world;
    }

    public String getServer() {
        return this.server;
    }
}
