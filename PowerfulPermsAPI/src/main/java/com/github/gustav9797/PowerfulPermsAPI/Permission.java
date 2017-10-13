package com.github.gustav9797.PowerfulPermsAPI;

import java.util.Date;

public interface Permission {

    /**
     * Returns the ID of this permission as it is stored in the database.
     */
    public int getId();

    /**
     * Returns the permission string.
     */
    public String getPermissionString();

    /**
     * Returns the name of the world the permission applies to.
     */
    public String getWorld();

    /**
     * Returns the name of the server the permission applies to. If empty or "all", applies to all servers.
     */
    public String getServer();

    /**
     * Returns the date when this permission expires. If no expiration date, it is null.
     */
    public Date getExpirationDate();

    /*
     * Returns true if this is a timed permission.
     */
    public boolean willExpire();

    /*
     * Returns true if this is a timed permission and has expired.
     */
    public boolean hasExpired();

}