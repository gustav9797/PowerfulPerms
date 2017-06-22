package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionContainer;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class PowerfulPerms_Vault_Chat extends Chat {

    private PowerfulPerms plugin;
    private PermissionManagerBase permissionManager;

    public PowerfulPerms_Vault_Chat(Permission perms, PowerfulPerms plugin) {
        super(perms);
        this.plugin = plugin;
        this.permissionManager = (PermissionManagerBase) plugin.getPermissionManager();
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
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            if (uuid == null)
                return null;
            try {
                return permissionManager.getPlayerPrefixBase(uuid, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PermissionPlayer p = permissionManager.getPermissionPlayer(player);
            if (p != null)
                return p.getPrefix();
        }
        return null;
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        UUID uuid = permissionManager.getConvertUUIDBase(player);
        if (uuid != null)
            permissionManager.setPlayerPrefixBase(uuid, prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(player);
            if (uuid == null)
                return null;
            try {
                return permissionManager.getPlayerSuffixBase(uuid, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PermissionPlayer p = permissionManager.getPermissionPlayer(player);
            if (p != null)
                return p.getSuffix();
        }
        return null;
    }

    @Override
    public void setPlayerSuffix(String world, String player, String suffix) {
        UUID uuid = permissionManager.getConvertUUIDBase(player);
        if (uuid != null)
            permissionManager.setPlayerSuffixBase(uuid, suffix);
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
            permissionManager.setGroupPrefixBase(groupId, prefix, "");
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
            permissionManager.setGroupSuffixBase(groupId, suffix, "");
        }
    }

    public String getPlayerVaultVariable(String playerName, String node, String world) {
        if (PowerfulPerms.vault_offline) {
            UUID uuid = permissionManager.getConvertUUIDBase(playerName);
            if (uuid == null)
                return null;
            try {
                List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions = permissionManager.getPlayerOwnPermissionsBase(uuid);
                return getVaultVariable(permissions, node, world);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PermissionPlayer p = permissionManager.getPermissionPlayer(playerName);
            if (p != null) {
                List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions = p.getPermissions();
                return getVaultVariable(permissions, node, world);
            }
        }
        return null;
    }

    public void setPlayerVaultVariable(String playerName, String node, String value, String world) {
        UUID uuid = permissionManager.getConvertUUIDBase(playerName);
        if (uuid == null)
            return;
        try {
            String var = "vault.variables." + node + "." + value;
            Response response = permissionManager.addPlayerPermissionBase(uuid, var, world, "", null);
            if (!response.succeeded())
                Bukkit.getLogger().warning("Could not set Vault player \"" + playerName + "\" variable \"" + node + "\" to \"" + value + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getGroupVaultVariable(String groupName, String node, String world) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            if (node.equalsIgnoreCase("rank"))
                return group.getRank() + "";
            return getVaultVariable(group.getOwnPermissions(), node, world);
        }
        return null;
    }

    public void setGroupVaultVariable(String groupName, String node, String value, String world) {
        Group group = permissionManager.getGroup(groupName);
        if (group != null) {
            String var = "vault.variables." + node + "." + value;
            Response response = permissionManager.addGroupPermissionBase(group.getId(), var, world, "", null);
            if (!response.succeeded())
                Bukkit.getLogger().warning("Could not set Vault group \"" + groupName + "\" variable \"" + node + "\" to \"" + value + "\"");
        }
    }

    public String getVaultVariable(List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions, String node, String world) {
        for (com.github.cheesesoftware.PowerfulPermsAPI.Permission p : permissions) {
            if (PermissionContainer.permissionApplies(p, PermissionManagerBase.serverName, world)) {
                String perm = p.getPermissionString();
                String begin = "vault.variables." + node + ".";
                if (perm.startsWith(begin) && perm.length() > begin.length()) {
                    return perm.substring(begin.length());
                }
            }
        }
        return null;
    }

    /**
     * Player
     */

    @Override
    public int getPlayerInfoInteger(String world, String player, String node, int defaultValue) {
        String var = getPlayerVaultVariable(player, node, world);
        try {
            int value = Integer.parseInt(var);
            return value;
        } catch (Exception e) {
        }
        return defaultValue;
    }

    @Override
    public void setPlayerInfoInteger(String world, String player, String node, int value) {
        setPlayerVaultVariable(player, node, value + "", world);
    }

    @Override
    public double getPlayerInfoDouble(String world, String player, String node, double defaultValue) {
        String var = getPlayerVaultVariable(player, node, world);
        try {
            double value = Double.parseDouble(var);
            return value;
        } catch (Exception e) {
        }
        return defaultValue;
    }

    @Override
    public void setPlayerInfoDouble(String world, String player, String node, double value) {
        setPlayerVaultVariable(player, node, value + "", world);
    }

    @Override
    public boolean getPlayerInfoBoolean(String world, String player, String node, boolean defaultValue) {
        String var = getPlayerVaultVariable(player, node, world);
        if (var != null) {
            try {
                boolean value = Boolean.parseBoolean(var);
                return value;
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    @Override
    public void setPlayerInfoBoolean(String world, String player, String node, boolean value) {
        setPlayerVaultVariable(player, node, value + "", world);
    }

    @Override
    public String getPlayerInfoString(String world, String player, String node, String defaultValue) {
        String var = getPlayerVaultVariable(player, node, world);
        if (var != null)
            return var;
        return defaultValue;
    }

    @Override
    public void setPlayerInfoString(String world, String player, String node, String value) {
        setPlayerVaultVariable(player, node, value + "", world);
    }

    /**
     * Group
     */

    @Override
    public int getGroupInfoInteger(String world, String group, String node, int defaultValue) {
        String var = getGroupVaultVariable(group, node, world);
        try {
            int value = Integer.parseInt(var);
            return value;
        } catch (Exception e) {
        }
        return defaultValue;
    }

    @Override
    public void setGroupInfoInteger(String world, String group, String node, int value) {
        setGroupVaultVariable(group, node, value + "", world);
    }

    @Override
    public double getGroupInfoDouble(String world, String group, String node, double defaultValue) {
        String var = getGroupVaultVariable(group, node, world);
        try {
            double value = Double.parseDouble(var);
            return value;
        } catch (Exception e) {
        }
        return defaultValue;
    }

    @Override
    public void setGroupInfoDouble(String world, String group, String node, double value) {
        setGroupVaultVariable(group, node, value + "", world);
    }

    @Override
    public boolean getGroupInfoBoolean(String world, String group, String node, boolean defaultValue) {
        String var = getGroupVaultVariable(group, node, world);
        if (var != null) {
            try {
                boolean value = Boolean.parseBoolean(var);
                return value;
            } catch (Exception e) {
            }
        }
        return defaultValue;
    }

    @Override
    public void setGroupInfoBoolean(String world, String group, String node, boolean value) {
        setGroupVaultVariable(group, node, value + "", world);
    }

    @Override
    public String getGroupInfoString(String world, String group, String node, String defaultValue) {
        String var = getGroupVaultVariable(group, node, world);
        if (var != null)
            return var;
        return defaultValue;
    }

    @Override
    public void setGroupInfoString(String world, String group, String node, String value) {
        setGroupVaultVariable(group, node, value + "", world);
    }

}
