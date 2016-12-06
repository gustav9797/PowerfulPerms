package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionContainer;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

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
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(player);
        try {
            UUID uuid = first.get();
            ListenableFuture<String> second = permissionManager.getPlayerPrefix(uuid);
            return second.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setPlayerPrefix(String world, String player, String prefix) {
        permissionManager.setPlayerPrefix(plugin.getPlayerUUID(player), prefix);
    }

    @Override
    public String getPlayerSuffix(String world, String player) {
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(player);
        try {
            UUID uuid = first.get();
            ListenableFuture<String> second = permissionManager.getPlayerSuffix(uuid);
            return second.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public String getPlayerVaultVariable(String playerName, String node, String world) {
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
        try {
            UUID uuid = first.get();
            ListenableFuture<List<com.github.cheesesoftware.PowerfulPermsAPI.Permission>> second = permissionManager.getPlayerOwnPermissions(uuid);
            List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions = second.get();
            return getVaultVariable(permissions, node, world);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setPlayerVaultVariable(String playerName, String node, String value, String world) {
        ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
        try {
            UUID uuid = first.get();
            String var = "vault.variables." + node + "." + value;
            ListenableFuture<Response> second = permissionManager.addPlayerPermission(uuid, var, world, "", null);
            try {
                second.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
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
            ListenableFuture<Response> first = permissionManager.addGroupPermission(group.getId(), var, world, "", null);
            try {
                first.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public String getVaultVariable(List<com.github.cheesesoftware.PowerfulPermsAPI.Permission> permissions, String node, String world) {
        Iterator<com.github.cheesesoftware.PowerfulPermsAPI.Permission> it = permissions.iterator();
        while (it.hasNext()) {
            com.github.cheesesoftware.PowerfulPermsAPI.Permission p = it.next();
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
