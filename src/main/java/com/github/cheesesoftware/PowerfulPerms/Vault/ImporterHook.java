package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

import eu.taigacraft.importer.permissions.PermissionsImporter;

public class ImporterHook implements eu.taigacraft.importer.permissions.PermissionsImporter {

    PermissionManager permissionManager;

    public void hook(PowerfulPermsPlugin plugin) {
        permissionManager = plugin.getPermissionManager();
        PermissionsImporter.register("PowerfulPerms", this);
    }

    // Prefix

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

    // Suffix

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
    public String getRank(OfflinePlayer player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            Group group = p.getPrimaryGroup();
            if (group != null)
                return group.getName();
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
    public Boolean hasPermission(OfflinePlayer player, String permission, String worldname) {
        return hasPermission(player, permission);
    }

    @Override
    public List<String> getRanks(OfflinePlayer player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player.getUniqueId());
        if (p != null) {
            List<Group> groups = p.getGroups(PermissionManagerBase.serverName);
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames;
        }
        return null;
    }

    @Override
    public void load(OfflinePlayer player) {

    }

    @Override
    public void unload(OfflinePlayer player) {

    }
}