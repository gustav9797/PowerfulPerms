package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionContainer;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerMiddle;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerLoadedEvent;
import com.google.common.util.concurrent.ListenableFuture;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PowerfulPermissionManager extends PermissionManagerMiddle implements Listener {

    private PowerfulPerms bungeePlugin;

    public PowerfulPermissionManager(Database database, PowerfulPerms plugin, String serverName) {
        super(database, plugin, serverName);
        this.bungeePlugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        debug("PlayerQuitEvent " + e.getPlayer().getName());
        if (containsPermissionPlayer(e.getPlayer().getUniqueId()))
            removePermissionPlayer(e.getPlayer().getUniqueId());
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId()))
            cachedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(final LoginEvent e) {
        debug("LoginEvent " + e.getConnection().getName());

        if (!e.isCancelled()) {
            e.registerIntent(bungeePlugin);
            bungeePlugin.getProxy().getScheduler().schedule(bungeePlugin, new Runnable() {

                @Override
                public void run() {
                    loadPlayer(e.getConnection().getUniqueId(), e.getConnection().getName(), true, true);
                    debug("LoginEvent uuid " + e.getConnection().getUniqueId().toString());
                    e.completeIntent(bungeePlugin);
                }
            }, 0, TimeUnit.MILLISECONDS);
        } else
            debug("LoginEvent player not allowed");
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void onPostLogin(final PostLoginEvent e) {
        debug("player server name: \"" + e.getPlayer().getServer() + "\"");
        debug("PostLoginEvent " + e.getPlayer().getName() + " uuid " + e.getPlayer().getUniqueId().toString());
        // Check again if
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId())) {
            // Player is cached. Continue load it.
            loadCachedPlayer(e.getPlayer());
        } else
            debug("onPlayerJoin player isn't cached");
        debug("PostLoginEvent finish");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnected(final ServerConnectedEvent e) {
        debug("serverconnected event " + e.getServer().getInfo().getName());
        if (containsPermissionPlayer(e.getPlayer().getUniqueId())) {
            PowerfulPermissionPlayer player = (PowerfulPermissionPlayer) getPermissionPlayer(e.getPlayer().getUniqueId());
            player.updatePermissions(e.getServer().getInfo());
        }
    }

    @Override
    public ListenableFuture<Boolean> playerHasPermission(UUID uuid, String permission, String world, String server) {
        ListenableFuture<Boolean> first = service.submit(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                PermissionPlayer player = getPermissionPlayer(uuid);
                if (player != null) {
                    if ((world == null || world.isEmpty() || world.equalsIgnoreCase("all")) && (server == null || server.isEmpty() || server.equalsIgnoreCase("all")))
                        return player.hasPermission(permission);
                    PermissionContainer permissionContainer = new PermissionContainer(player.getPermissions());
                    permissionContainer.setRealPermissions(PermissionPlayerBase.calculatePermissions(server, world, player.getGroups(), permissionContainer, plugin));
                    return permissionContainer.hasPermission(permission);
                }
                LinkedHashMap<String, List<CachedGroup>> currentGroups = getPlayerCurrentGroupsBase(uuid);
                List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(server, currentGroups);
                List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
                PermissionContainer permissionContainer = new PermissionContainer(getPlayerOwnPermissions(uuid).get());
                permissionContainer.setRealPermissions(PermissionPlayerBase.calculatePermissions(server, world, groups, permissionContainer, plugin));
                // Player own permissions have been added. Permissions from player groups have been added. In relation to world and server.
                return permissionContainer.hasPermission(permission);
            }
        });
        return first;
    }

    @Override
    public void reloadPlayers() {
        if (bungeePlugin != null) {
            for (ProxiedPlayer p : bungeePlugin.getProxy().getPlayers()) {
                if (containsPermissionPlayer(p.getUniqueId())) {
                    loadPlayer(p.getUniqueId(), p.getName(), false, false);
                } else {
                    loadPlayer(p.getUniqueId(), p.getName(), true, true);
                    loadCachedPlayer(p);
                }
            }
        }
    }

    /**
     * Continues loading a previously cached player.
     */
    private void loadCachedPlayer(ProxiedPlayer player) {
        playersLock.lock();
        try {
            if (players.containsKey(player.getUniqueId())) {
                players.remove(player.getUniqueId());
            }
        } finally {
            playersLock.unlock();
        }

        PermissionPlayerBase base = super.loadCachedPlayer(player.getUniqueId());
        if (base != null && player != null) {
            PowerfulPermissionPlayer permissionPlayer = new PowerfulPermissionPlayer(player, base, plugin);
            putPermissionPlayer(player.getUniqueId(), permissionPlayer);
            checkPlayerTimedGroupsAndPermissions(player.getUniqueId(), permissionPlayer);
            eventHandler.fireEvent(new PlayerLoadedEvent(player.getUniqueId(), player.getName()));
        } else
            debug("loadCachedPlayer: ProxiedPlayer or PermissionPlayerBase is null");
        debug("loadCachedPlayer finish");
    }

}