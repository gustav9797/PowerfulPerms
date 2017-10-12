package com.github.gustav9797.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;

import com.github.gustav9797.PowerfulPermsAPI.Permission;

public class PermissionContainer {

    protected List<Permission> ownPermissions = new ArrayList<>();
    protected List<String> realPermissions = new ArrayList<>();
    protected List<Permission> allPermissions = new ArrayList<>();
    protected List<String> temporaryPrePermissions = new ArrayList<>();
    protected List<String> temporaryPostPermissions = new ArrayList<>();

    protected ReentrantLock asyncPermLock = new ReentrantLock();

    public PermissionContainer(List<Permission> ownPermissions) {
        this.ownPermissions = ownPermissions;
    }

    protected Boolean preHasPermission(String permission) {
        Boolean has = null;

        asyncPermLock.lock();
        try {
            if (temporaryPrePermissions != null) {
                ListIterator<String> it = temporaryPrePermissions.listIterator(temporaryPrePermissions.size());
                while (it.hasPrevious()) {
                    Boolean check = internalPermissionCheck(permission, it.previous());
                    if (check != null) {
                        has = check;
                        break;
                    }
                }
            }

            ListIterator<String> it = realPermissions.listIterator(realPermissions.size());
            while (it.hasPrevious()) {
                Boolean check = internalPermissionCheck(permission, it.previous());
                if (check != null) {
                    has = check;
                    break;
                }
            }

            if (temporaryPostPermissions != null) {
                it = temporaryPostPermissions.listIterator(temporaryPostPermissions.size());
                while (it.hasPrevious()) {
                    Boolean check = internalPermissionCheck(permission, it.previous());
                    if (check != null) {
                        has = check;
                        break;
                    }
                }
            }
        } finally {
            asyncPermLock.unlock();
        }

        return has;
    }

    protected Boolean internalPermissionCheck(String toCheck, String ownPermission) {
        Boolean has = null;
        if (ownPermission.equalsIgnoreCase(toCheck)) {
            has = true;
        } else if (ownPermission.equalsIgnoreCase("-" + toCheck)) {
            has = false;
        } else if (ownPermission.endsWith("*")) {

            boolean ownNegated = ownPermission.startsWith("-");
            int ownOffset = (ownNegated ? 1 : 0);
            int i = 0;
            for (; i + ownOffset < ownPermission.length() && i < toCheck.length();) {
                if (ownPermission.charAt(i + ownOffset) == toCheck.charAt(i)) {
                    // plugin.debug(ownPermission.charAt(i + ownOffset) + " " + toCheck.charAt(i) + " " + i + " + 1");
                    ++i;
                } else
                    break;
            }

            // plugin.debug("ownPermission " + ownPermission);
            // plugin.debug("toCheck " + toCheck);
            // plugin.debug("ownPermission " + ownPermission.length());
            // plugin.debug("toCheck " + toCheck.length());
            // plugin.debug("i " + i);

            if (ownPermission.charAt(i + ownOffset) == '*') {
                has = !ownNegated;
                // plugin.debug("wildcard perm check: has = " + has + " ownPermission = " + ownPermission);
            }
        }
        return has;
    }

    /**
     * Checks if permission applies for server and world.
     */
    public static boolean permissionApplies(Permission e, String playerServer, String playerWorld) {
        boolean isSameServer = false;
        boolean isSameWorld = false;

        if (e.getServer().isEmpty() || e.getServer().equalsIgnoreCase("ALL") || playerServer == null || e.getServer().equalsIgnoreCase(playerServer))
            isSameServer = true;

        if (e.getWorld().isEmpty() || e.getWorld().equalsIgnoreCase("ALL") || playerWorld == null || e.getWorld().equalsIgnoreCase(playerWorld))
            isSameWorld = true;

        if (isSameServer && isSameWorld)
            return true;
        return false;
    }

    /**
     * Returns all permissions for this player.
     */
    public List<Permission> getPermissions() {
        return new ArrayList<>(this.ownPermissions);
    }

    /**
     * Returns all permissions in effect for this player.
     */
    public List<String> getPermissionsInEffect() {
        asyncPermLock.lock();
        try {
            return new ArrayList<>(this.realPermissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    /**
     * Returns all permissions for this player.
     */
    public List<Permission> getAllPermissions() {
        asyncPermLock.lock();
        try {
            return new ArrayList<>(this.allPermissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    public boolean isPermissionSet(String permission) {
        return preHasPermission(permission) != null;
    }

    /**
     * Check if this player has the specified permission.
     */
    public Boolean hasPermission(String permission) {
        return preHasPermission(permission);
    }

    public void setRealPermissions(List<String> permissions) {
        asyncPermLock.lock();
        try {
            this.realPermissions = new ArrayList<>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    public void setAllPermissions(List<Permission> permissions) {
        asyncPermLock.lock();
        try {
            this.allPermissions = new ArrayList<>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    public void setTemporaryPrePermissions(List<String> permissions) {
        asyncPermLock.lock();
        try {
            this.temporaryPrePermissions = new ArrayList<>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
    }

    public void setTemporaryPostPermissions(List<String> permissions) {
        asyncPermLock.lock();
        try {
            this.temporaryPostPermissions = new ArrayList<>(permissions);
        } finally {
            asyncPermLock.unlock();
        }
    }
}
