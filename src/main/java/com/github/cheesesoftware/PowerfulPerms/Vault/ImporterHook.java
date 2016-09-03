/*package com.github.cheesesoftware.PowerfulPerms.Vault;

import org.bukkit.OfflinePlayer;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class ImporterHook implements eu.taigacraft.importer.permissions.PermissionsImporter {

    PermissionManager permissionManager;

    public void hook(PowerfulPermsPlugin plugin) {
        permissionManager = plugin.getPermissionManager();
        eu.taigacraft.importer.permissions.PermissionsImporter.register("PowerfulPerms", this);
    }

    @Override
    public String getPrefix(OfflinePlayer player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            return p.getPrefix();
        }
        return null;
    }

    @Override
    public String getPrefix(OfflinePlayer player, String worldname) {
        return getPrefix(player);
    }

    @Override
    public String getPrefix(OfflinePlayer player, String worldname, String ladder) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            return p.getPrefix(ladder);
        }
        return null;
    }

    @Override
    public String getRank(OfflinePlayer player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            Group group = p.getGroup();
            if (group != null)
                return group.getName();
        }
        return null;
    }

    @Override
    public String getSuffix(OfflinePlayer player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            return p.getSuffix();
        }
        return null;
    }

    @Override
    public String getSuffix(OfflinePlayer player, String worldname) {
        return getSuffix(player);
    }

    @Override
    public String getSuffix(OfflinePlayer player, String worldname, String ladder) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            return p.getSuffix(ladder);
        }
        return null;
    }

    @Override
    public Boolean hasPermission(OfflinePlayer player, String permission) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            return p.hasPermission(permission);
        }
        return null;
    }

    @Override
    public boolean hasPermission(OfflinePlayer player, String permission, String worldname) {
        return hasPermission(player, permission);
    }
}
*/