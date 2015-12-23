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
        if(permission != null) {
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

        String name = permission.toLowerCase();
        Boolean has = permissionsPlayer.hasPermission(name) || super.isOp();
        return has;
    }
}
