package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class PermissionManagerMiddle extends PermissionManagerBase implements PermissionManager {

    protected ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    // protected ListeningExecutorService sameThreadService = MoreExecutors.sameThreadExecutor();

    public PermissionManagerMiddle(Database database, PowerfulPermsPlugin plugin, String serverName) {
        super(database, plugin, serverName);
    }

    @Override
    public ExecutorService getExecutor() {
        return service;
    }

    @Override
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerOwnGroups(UUID uuid) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> listenableFuture = service.submit(() -> getPlayerOwnGroupsBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> getPlayerCurrentGroups(UUID uuid) {
        ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> listenableFuture = service.submit(() -> getPlayerCurrentGroupsBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Group> getPlayerPrimaryGroup(UUID uuid) {
        ListenableFuture<Group> listenableFuture = service.submit(() -> getPlayerPrimaryGroupBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Boolean> isPlayerDefault(UUID uuid) {
        ListenableFuture<Boolean> listenableFuture = service.submit(() -> isPlayerDefaultBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<DBDocument> getPlayerData(UUID uuid) {
        ListenableFuture<DBDocument> listenableFuture = service.submit(() -> getPlayerDataBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<List<Permission>> getPlayerOwnPermissions(UUID uuid) {
        ListenableFuture<List<Permission>> listenableFuture = service.submit(() -> getPlayerOwnPermissionsBase(uuid));
        return listenableFuture;
    }

    // Overridden in PowerfulPermissionManager
    @Override
    public ListenableFuture<Boolean> playerHasPermission(UUID uuid, String permission, String world, String server) {
        return null;
    }

    @Override
    public ListenableFuture<String> getPlayerPrefix(UUID uuid, String ladder) {
        ListenableFuture<String> first = service.submit(() -> getPlayerPrefixBase(uuid, ladder));
        return first;
    }

    @Override
    public ListenableFuture<String> getPlayerPrefix(UUID uuid) {
        return getPlayerPrefix(uuid, null);
    }

    @Override
    public ListenableFuture<String> getPlayerSuffix(UUID uuid, String ladder) {
        ListenableFuture<String> listenableFuture = service.submit(() -> getPlayerSuffixBase(uuid, ladder));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<String> getPlayerSuffix(UUID uuid) {
        return getPlayerSuffix(uuid, null);
    }

    @Override
    public ListenableFuture<String> getPlayerOwnPrefix(UUID uuid) {
        ListenableFuture<String> listenableFuture = service.submit(() -> getPlayerOwnPrefixBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<String> getPlayerOwnSuffix(UUID uuid) {
        ListenableFuture<String> listenableFuture = service.submit(() -> getPlayerOwnSuffixBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<UUID> getConvertUUID(String playerName) {
        ListenableFuture<UUID> listenableFuture = service.submit(() -> getConvertUUIDBase(playerName));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> createPlayer(String name, UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> createPlayerBase(name, uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addPlayerPermission(UUID uuid, String permission) {
        return addPlayerPermission(uuid, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> addPlayerPermission(UUID uuid, String permission, String world, String server, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> addPlayerPermissionBase(uuid, permission, world, server, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerPermission(UUID uuid, String permission) {
        return removePlayerPermission(uuid, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> removePlayerPermission(UUID uuid, String permission, String world, String server, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removePlayerPermissionBase(uuid, permission, world, server, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerPermissions(UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removePlayerPermissionsBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerPrefix(UUID uuid, String prefix) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setPlayerPrefixBase(uuid, prefix));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerSuffix(UUID uuid, String suffix) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setPlayerSuffixBase(uuid, suffix));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId) {
        return removePlayerGroup(uuid, groupId, "", false, null);
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId, boolean negated) {
        return removePlayerGroup(uuid, groupId, "", negated, null);
    }

    @Override
    public ListenableFuture<Response> removePlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removePlayerGroupBase(uuid, groupId, server, negated, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId) {
        return addPlayerGroup(uuid, groupId, false);
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId, boolean negated) {
        return addPlayerGroup(uuid, groupId, "", negated, null);
    }

    @Override
    public ListenableFuture<Response> addPlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> addPlayerGroupBase(uuid, groupId, server, negated, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setPlayerRank(UUID uuid, int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setPlayerRankBase(uuid, groupId));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> promotePlayer(UUID uuid, String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> promotePlayerBase(uuid, ladder));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> demotePlayer(UUID uuid, String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> demotePlayerBase(uuid, ladder));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> deletePlayer(UUID uuid) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> deletePlayerBase(uuid));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> createGroup(String name, String ladder, int rank) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> createGroupBase(name, ladder, rank));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> deleteGroup(int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> deleteGroupBase(groupId));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addGroupPermission(int groupId, String permission) {
        return addGroupPermission(groupId, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> addGroupPermission(int groupId, String permission, String world, String server, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> addGroupPermissionBase(groupId, permission, world, server, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupPermission(int groupId, String permission) {
        return removeGroupPermission(groupId, permission, "", "", null);
    }

    @Override
    public ListenableFuture<Response> removeGroupPermission(int groupId, String permission, String world, String server, Date expires) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removeGroupPermissionBase(groupId, permission, world, server, expires));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupPermissions(int groupId) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removeGroupPermissionsBase(groupId));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> addGroupParent(int groupId, int parentGroupId) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> addGroupParentBase(groupId, parentGroupId));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> removeGroupParent(int groupId, int parentGroupId) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> removeGroupParentBase(groupId, parentGroupId));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupPrefix(int groupId, String prefix) {
        return setGroupPrefix(groupId, prefix, "");
    }

    @Override
    public ListenableFuture<Response> setGroupPrefix(int groupId, String prefix, String server) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setGroupPrefixBase(groupId, prefix, server));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupSuffix(int groupId, String suffix) {
        return setGroupSuffix(groupId, suffix, "");
    }

    @Override
    public ListenableFuture<Response> setGroupSuffix(int groupId, String suffix, String server) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setGroupSuffixBase(groupId, suffix, server));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupLadder(int groupId, String ladder) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setGroupLadderBase(groupId, ladder));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupRank(int groupId, int rank) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setGroupRankBase(groupId, rank));
        return listenableFuture;
    }

    @Override
    public ListenableFuture<Response> setGroupName(int groupId, String name) {
        ListenableFuture<Response> listenableFuture = service.submit(() -> setGroupNameBase(groupId, name));
        return listenableFuture;
    }

}
