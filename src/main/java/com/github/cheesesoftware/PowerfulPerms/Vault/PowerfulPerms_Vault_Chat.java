package com.github.cheesesoftware.PowerfulPerms.Vault;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Chat extends Chat {

    private PowerfulPermsPlugin plugin;
    private PermissionManager permissionManager;

    public PowerfulPerms_Vault_Chat(Permission perms, PowerfulPermsPlugin plugin) {
        super(perms);
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
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
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null)
            return p.getPrefix();
        return null;
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        permissionManager.setPlayerPrefix(plugin.getPlayerUUID(player), prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        PermissionPlayer p = permissionManager.getPermissionPlayer(player);
        if (p != null)
            return p.getSuffix();
        return null;
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        permissionManager.setPlayerSuffix(plugin.getPlayerUUID(player), suffix);
    }

    @Override
    public String getGroupPrefix(String world, String groupName) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null)
            return group.getPrefix(PermissionManagerBase.serverName);
        return null;
    }

    @Override
    public void setGroupPrefix(String world, String groupName, String prefix) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            permissionManager.setGroupPrefix(groupId, prefix);
        }
    }

    @Override
    public String getGroupSuffix(String world, String groupName) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null)
            return group.getSuffix(PermissionManagerBase.serverName);
        return null;
    }

    @Override
    public void setGroupSuffix(String world, String groupName, String suffix) {
        final Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            int groupId = group.getId();
            permissionManager.setGroupSuffix(groupId, suffix);
        }
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
