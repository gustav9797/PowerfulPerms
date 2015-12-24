package com.github.cheesesoftware.PowerfulPerms;

import org.bukkit.permissions.PermissibleBase;

public class CustomPermissibleBase extends PermissibleBase {

    private PermissionsPlayer permissionsPlayer;

    public CustomPermissibleBase(PermissionsPlayer permissionsPlayer) {
        super(permissionsPlayer.getPlayer());
        this.permissionsPlayer = permissionsPlayer;
    }

    @Override
    public boolean isPermissionSet(String permission) {
        if (permission != null) {
            permission = permission.toLowerCase();
            return permissionsPlayer.isPermissionSet(permission);
        }
        return false;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        
        permission = permission.toLowerCase();

        // OP negation is bad. Derive negated from Guest -> never able to get that again
        /*if (permissionsPlayer.getPlayer() != null && permissionsPlayer.getPlayer().isOp()) {
            if (permissionsPlayer.isPermissionNegated(permission))
                return false;
            return true;
        }*/

        return permissionsPlayer.hasPermission(permission) || (permissionsPlayer.getPlayer() != null && permissionsPlayer.getPlayer().isOp()) ;
    }
}
