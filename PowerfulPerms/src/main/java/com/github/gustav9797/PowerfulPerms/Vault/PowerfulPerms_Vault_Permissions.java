package com.github.gustav9797.PowerfulPerms.Vault;

import com.github.gustav9797.PowerfulPerms.PowerfulPerms;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPerms.common.PermissionPlayerBase;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Permissions extends Permission {

    private PowerfulPermsPlugin plugin;
    private PermissionManagerBase permissionManager;
    private PermissionManager _permissionManager;
    private String server;

    public PowerfulPerms_Vault_Permissions(PowerfulPerms plugin) {
        this.plugin = plugin;
        this.permissionManager = (PermissionManagerBase) plugin.getPermissionManager();
        this._permissionManager = plugin.getPermissionManager();
        this.server = PowerfulPerms.vaultIsLocal ? PermissionManagerBase.serverName : "";
    }

    @Override
    public String[] getGroups() {
        Map<Integer, Group> groups = permissionManager.getGroups();
        List<String> groupNames = new ArrayList<>();
        for (Group group : groups.values())
            groupNames.add(group.getName());
        return groupNames.toArray(new String[groupNames.size()]);
    }

    @Override
    public String getName() {
        return "PowerfulPerms";
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            try {
                LinkedHashMap<String, List<CachedGroup>> currentGroups = permissionManager.getPlayerCurrentGroupsBase(uuid);
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
                List<String> groupNames = new ArrayList<>();
                for (Group group : groups)
                    groupNames.add(group.getName());
                return groupNames.toArray(new String[groupNames.size()]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PermissionPlayer p = permissionManager.getPermissionPlayer(player);
            if (p != null) {
                List<Group> groups = p.getGroups();
                List<String> groupNames = new ArrayList<>();
                for (Group group : groups)
                    groupNames.add(group.getName());
                return groupNames.toArray(new String[groupNames.size()]);
            }
        }
        return null;
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            try {
                LinkedHashMap<String, List<CachedGroup>> currentGroups = permissionManager.getPlayerCurrentGroupsBase(uuid);
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
                Group group = PermissionPlayerBase.getPrimaryGroup(groups);
                if (group != null)
                    return group.getName();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PermissionPlayer p = permissionManager.getPermissionPlayer(player);
            if (p != null) {
                Group group = p.getPrimaryGroup();
                if (group != null)
                    return group.getName();
            }
        }
        return null;
    }

    @Override
    public boolean groupAdd(String world, String groupName, String permission) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            Response response = permissionManager.addGroupPermissionBase(groupId, permission, (world != null ? world : ""), server, null);
            return response.succeeded();
        }
        return false;
    }

    @Override
    public boolean groupHas(String world, String groupName, String permission) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            List<com.github.gustav9797.PowerfulPermsAPI.Permission> permissions = group.getPermissions();
            for (com.github.gustav9797.PowerfulPermsAPI.Permission p : permissions) {
                if (p.getPermissionString().equals(permission) && PermissionPlayerBase.permissionApplies(p, PermissionManagerBase.serverName, world) && !p.hasExpired()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean groupRemove(String world, String groupName, String permission) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            Response response = permissionManager.removeGroupPermissionBase(groupId, permission, (world != null ? world : ""), server, null);
            return response.succeeded();
        }
        return false;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        UUID uuid = permissionManager.getConvertUUIDBase(player);
        if (uuid == null)
            return false;
        Response response = permissionManager.addPlayerPermissionBase(uuid, permission, (world != null ? world : ""), server, null);
        return response.succeeded();
    }

    @Override
    public boolean playerAddGroup(String world, String player, String groupName) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            if (uuid == null)
                return false;
            Response response = permissionManager.addPlayerGroupBase(uuid, groupId, server, false, null);
            return response.succeeded();
        }
        return false;
    }

    @Override
    public boolean playerInGroup(String world, String player, String groupName) {
        String[] groups = this.getPlayerGroups(world, player);
        if (groups != null) {
            for (String group : groups) {
                if (group.equalsIgnoreCase(groupName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        UUID uuid = permissionManager.getConvertUUIDBase(player);
        if (uuid == null)
            return false;
        Response response = permissionManager.removePlayerPermissionBase(uuid, permission, (world != null ? world : ""), server, null);
        return response.succeeded();
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String groupName) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            if (uuid == null)
                return false;
            Response response = permissionManager.removePlayerGroupBase(uuid, groupId, server, false, null);
            return response.succeeded();
        }
        return false;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            try {
                ListenableFuture<Boolean> second = _permissionManager.playerHasPermission(uuid, permission, world, PermissionManagerBase.serverName);
                try {
                    Boolean has = second.get();
                    if (has == null)
                        return false;
                    return has;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Player p = Bukkit.getPlayer(player);
            if (p != null)
                return p.hasPermission(permission);
        }
        return false;
    }

}
