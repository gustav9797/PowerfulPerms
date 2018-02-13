package com.github.gustav9797.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import eu.taigacraft.importer.ImporterCallback;
import eu.taigacraft.importer.permissions.PermissionsImporter;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPerms.common.PermissionPlayerBase;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.util.concurrent.ListenableFuture;

public class ImporterHook implements PermissionsImporter {
    
    private PowerfulPermsPlugin plugin;
    private PermissionManager permissionManager;

    public void hook(PowerfulPermsPlugin plugin) {
        this.plugin = plugin;
        permissionManager = plugin.getPermissionManager();
        PermissionsImporter.register(Bukkit.getPluginManager().getPlugin("PowerfulPerms"), this);
    }


    @Override
    public void getRank(OfflinePlayer offlinePlayer, ImporterCallback<String> importerCallback) {
        ListenableFuture<Group> second = permissionManager.getPlayerPrimaryGroup(offlinePlayer.getUniqueId());
        try {
            Group group = second.get();
            if (group != null)
                importerCallback.call(group.getName());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getRanks(OfflinePlayer offlinePlayer, ImporterCallback<List<String>> importerCallback) {
        try {
            ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = permissionManager.getPlayerCurrentGroups(offlinePlayer.getUniqueId());
            LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
            List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
            List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
            List<String> groupNames = new ArrayList<>();
            for (Group group : groups)
                groupNames.add(group.getName());
            importerCallback.call(groupNames);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(new ArrayList<>());
        }
    }

    @Override
    public void getPrefix(OfflinePlayer offlinePlayer, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerPrefix(offlinePlayer.getUniqueId());
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getPrefix(OfflinePlayer offlinePlayer, String worldname, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerPrefix(offlinePlayer.getUniqueId());
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getPrefix(OfflinePlayer offlinePlayer, String worldname, String ladder, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerPrefix(offlinePlayer.getUniqueId(), ladder);
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getSuffix(OfflinePlayer offlinePlayer, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerSuffix(offlinePlayer.getUniqueId());
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getSuffix(OfflinePlayer offlinePlayer, String worldname, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerSuffix(offlinePlayer.getUniqueId());
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void getSuffix(OfflinePlayer offlinePlayer, String worldname, String ladder, ImporterCallback<String> importerCallback) {
        ListenableFuture<String> second = permissionManager.getPlayerPrefix(offlinePlayer.getUniqueId(), ladder);
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void hasPermission(OfflinePlayer offlinePlayer, String permission, ImporterCallback<Boolean> importerCallback) {
        ListenableFuture<Boolean> second = permissionManager.playerHasPermission(offlinePlayer.getUniqueId(), permission, "", "");
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }

    @Override
    public void hasPermission(OfflinePlayer offlinePlayer, String permission, String worldname, ImporterCallback<Boolean> importerCallback) {
        ListenableFuture<Boolean> second = permissionManager.playerHasPermission(offlinePlayer.getUniqueId(), permission, worldname, "");
        try {
            importerCallback.call(second.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            importerCallback.call(null);
        }
    }
}
