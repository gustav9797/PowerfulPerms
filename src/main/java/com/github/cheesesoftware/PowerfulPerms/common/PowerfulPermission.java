package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.Date;

import com.github.cheesesoftware.PowerfulPermsAPI.Permission;

public class PowerfulPermission implements Permission {
    private String permission;
    private String world = "";
    private String server = "";
    private Date expires = null;

    public PowerfulPermission(String permission) {
        this.permission = permission;
    }

    public PowerfulPermission(String permission, String world, String server, Date expires) {
        this.permission = permission;

        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("ALL"))
            this.world = world;
        if (server != null && !server.isEmpty() && !server.equalsIgnoreCase("ALL"))
            this.server = server;
        this.expires = expires;
    }

    @Override
    public String getPermissionString() {
        return this.permission;
    }

    @Override
    public String getWorld() {
        return this.world;
    }

    @Override
    public String getServer() {
        return this.server;
    }

    @Override
    public Date getExpirationDate() {
        return expires;
    }
}
