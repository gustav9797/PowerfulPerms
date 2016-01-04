package com.github.cheesesoftware.PowerfulPerms.Vault;

import com.github.cheesesoftware.PowerfulPerms.PermissionManager;
import com.github.cheesesoftware.PowerfulPerms.common.Group;
import com.github.cheesesoftware.PowerfulPerms.common.IPermissionsPlayer;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.ResponseRunnable;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Chat extends Chat {

    private PermissionManager permissionManager;

    public PowerfulPerms_Vault_Chat(Permission perms, PermissionManager permissionManager) {
        super(perms);
        this.permissionManager = permissionManager;
    }

    @Override
    public String getName() {
        return "PowerfulPerms";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPlayerPrefix(String world, String player) {
        IPermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
        if (p != null)
            return p.getPrefix();
        return null;
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        permissionManager.setPlayerPrefix(player, prefix, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        IPermissionsPlayer p = permissionManager.getPermissionsPlayer(player);
        if (p != null)
            return p.getSuffix();
        return null;
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        permissionManager.setPlayerSuffix(player, suffix, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public String getGroupPrefix(String world, String groupName) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null)
            return group.getPrefix(PermissionManagerBase.serverName);
        return null;
    }

    @Override
    public void setGroupPrefix(String world, String group, String prefix) {
        permissionManager.setGroupPrefix(group, prefix, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public String getGroupSuffix(String world, String groupName) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null)
            return group.getSuffix(PermissionManagerBase.serverName);
        return null;
    }

    @Override
    public void setGroupSuffix(String world, String group, String suffix) {
        permissionManager.setGroupSuffix(group, suffix, new ResponseRunnable() {

            @Override
            public void run() {

            }
        });
    }

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
    }

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        return defaultValue;
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        return defaultValue;
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
    }

}
