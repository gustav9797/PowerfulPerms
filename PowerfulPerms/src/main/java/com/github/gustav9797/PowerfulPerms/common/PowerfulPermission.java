package com.github.gustav9797.PowerfulPerms.common;

import java.util.Date;

import com.github.gustav9797.PowerfulPermsAPI.Permission;

public class PowerfulPermission implements Permission {
    private int id;
    private String permission;
    private String world = "";
    private String server = "";
    private Date expires = null;
    private int expireTaskId = -1;

    public PowerfulPermission(int id, String permission) {
        this.id = id;
        this.permission = permission;
    }

    public PowerfulPermission(int id, String permission, String world, String server, Date expires) {
        this.id = id;
        this.permission = permission;

        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("ALL"))
            this.world = world;
        if (server != null && !server.isEmpty() && !server.equalsIgnoreCase("ALL"))
            this.server = server;
        this.expires = expires;
    }

    @Override
    public int getId() {
        return this.id;
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

    @Override
    public boolean willExpire() {
        return expires != null;
    }

    @Override
    public boolean hasExpired() {
        return willExpire() && getExpirationDate().before(new Date());
    }

    public int getExpireTaskId() {
        return expireTaskId;
    }

    public void setExpireTaskId(int taskId) {
        this.expireTaskId = taskId;
    }
}
