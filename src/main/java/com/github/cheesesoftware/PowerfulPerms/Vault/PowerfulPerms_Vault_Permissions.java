package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

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
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            List<Group> groups = p.getGroups(PermissionManagerBase.serverName);
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames.toArray(new String[groupNames.size()]);
        }
        return null;
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            List<Group> groups = p.getGroups();
            if (groups != null) {
                Iterator<Group> it = groups.iterator();
                if (it.hasNext())
                    return it.next().getName();
            }
        }
        return null;
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        permissionManager.addGroupPermission(group, permission, "", server, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "One of your plugins is using Vault in an undesirable way(groupHas). Please contact the developer of PowerfulPerms(gustav9797)");
        return false;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        permissionManager.removeGroupPermission(group, permission, "", server, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
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
        permissionManager.addPlayerPermission(plugin.getPlayerUUID(player), player, permission, world, server, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        permissionManager.addPlayerGroup(plugin.getPlayerUUID(player), group, server, false, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
    }

    @Override
    public boolean playerInGroup(String world, String player, String groupName) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            List<Group> groups = p.getGroups();
            for (Group group : groups) {
                if (group.getName().equalsIgnoreCase(groupName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        permissionManager.removePlayerPermission(plugin.getPlayerUUID(player), permission, world, server, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        permissionManager.removePlayerGroup(plugin.getPlayerUUID(player), group, server, false, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null) {
            Boolean has = p.hasPermission(permission);
            if (has != null)
                return has;
        }
        return false;
    }

}
