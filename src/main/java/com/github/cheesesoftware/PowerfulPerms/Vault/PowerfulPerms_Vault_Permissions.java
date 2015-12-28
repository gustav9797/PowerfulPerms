package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.IPermissionsPlayer;
import com.github.cheesesoftware.PowerfulPerms.PermissionManager;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.common.PMR;

import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Permissions extends Permission {

    private PermissionManager permissionManager;

    public PowerfulPerms_Vault_Permissions(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    public String[] getGroups() {
        Collection<Group> groups = permissionManager.getGroups();
        List<String> groupNames = new ArrayList<String>();
        for (Group group : groups)
            groupNames.add(group.getName());
        return groupNames.toArray(new String[groupNames.size()]);
    }

    @Override
    public String getName() {
        return "PowerfulPerms";
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        IPermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
        if (p != null) {
            List<Group> groups = p.getApplyingGroups(Bukkit.getServerName());
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames.toArray(new String[groupNames.size()]);
        }
        return null;
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        /*Group group = permissionManager.getPlayerPrimaryGroup(player);
        if (group != null)
            return group.getName();*/
        return null;
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        //PMR result = permissionManager.addGroupPermission(group, permission, world, "");
        //return result.isSucceeded();
        return false;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        //return permissionManager.groupHasPermission(group, permission, Bukkit.getServerName(), world);
        Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "One of your plugins is using Vault in an undesirable way. Please contact the developer of PowerfulPerms(gustav9797)");
        return false;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        //PMR result = permissionManager.removeGroupPermission(group, permission, world, "");
        //return result.isSucceeded();
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
        //PMR result = permissionManager.addPlayerPermission(player, permission, world, "");
        //return result.isSucceeded();
        return false;
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        //PMR result = permissionManager.addPlayerGroup(player, group);
        //return result.isSucceeded();
        return false;
    }

    @Override
    public boolean playerInGroup(String world, String player, String groupName) {
        IPermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
        if (p != null) {
            List<Group> groups = p.getApplyingGroups(Bukkit.getServerName());
            for (Group group : groups) {
                if (group.getName().equalsIgnoreCase(groupName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        //PMR result = permissionManager.removePlayerPermission(player, permission, world, "");
        //return result.isSucceeded();
        return false;
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        //PMR result = permissionManager.removePlayerGroup(player, group);
        //return result.isSucceeded();
        return false;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "One of your plugins is using Vault in an undesirable way. Please contact the developer of PowerfulPerms(gustav9797)");
        return false;
    }

}
