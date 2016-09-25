package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Permissions extends Permission {

    private PowerfulPermsPlugin plugin;
    private PermissionManager permissionManager;
    private String server;

    public PowerfulPerms_Vault_Permissions(PowerfulPermsPlugin plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.server = PowerfulPerms.vaultIsLocal ? PermissionManagerBase.serverName : "";
    }

    @Override
    public String[] getGroups() {
        Map<Integer, Group> groups = permissionManager.getGroups();
        List<String> groupNames = new ArrayList<String>();
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
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(player);
        try {
            UUID uuid = first.get();
            ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = permissionManager.getPlayerCurrentGroups(uuid);
            LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
            List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
            List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames.toArray(new String[groupNames.size()]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(player);
        try {
            UUID uuid = first.get();
            ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = permissionManager.getPlayerCurrentGroups(uuid);
            LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
            List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
            List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
            Group group = PermissionPlayerBase.getPrimaryGroup(groups);
            if (group != null)
                return group.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean groupAdd(String world, String groupName, String permission) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            ListenableFuture<Response> first = permissionManager.addGroupPermission(groupId, permission, (world != null ? world : ""), server, null);
            try {
                Response response = first.get();
                return response.succeeded();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean groupHas(String world, String groupName, String permission) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions = group.getPermissions();
            for (com.github.cheesesoftware.PowerfulPermsAPI.Permission p : permissions) {
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
            ListenableFuture<Response> first = permissionManager.removeGroupPermission(groupId, permission, (world != null ? world : ""), server, null);
            try {
                Response response = first.get();
                return response.succeeded();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
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
        ListenableFuture<Response> first = permissionManager.addPlayerPermission(plugin.getPlayerUUID(player), permission, (world != null ? world : ""), server, null);
        try {
            Response response = first.get();
            return response.succeeded();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean playerAddGroup(String world, String player, String groupName) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            ListenableFuture<Response> first = permissionManager.addPlayerGroup(plugin.getPlayerUUID(player), groupId, server, false, null);
            try {
                Response response = first.get();
                return response.succeeded();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
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
        ListenableFuture<Response> first = permissionManager.removePlayerPermission(plugin.getPlayerUUID(player), permission, (world != null ? world : ""), server, null);
        try {
            Response response = first.get();
            return response.succeeded();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String groupName) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            ListenableFuture<Response> first = permissionManager.removePlayerGroup(plugin.getPlayerUUID(player), groupId, server, false, null);
            try {
                Response response = first.get();
                return response.succeeded();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(player);
        try {
            ListenableFuture<Boolean> second = permissionManager.playerHasPermission(first.get(), permission, world, PermissionManagerBase.serverName);
            try {
                Boolean has = second.get();
                if (has == null)
                    return false;
                return has;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
