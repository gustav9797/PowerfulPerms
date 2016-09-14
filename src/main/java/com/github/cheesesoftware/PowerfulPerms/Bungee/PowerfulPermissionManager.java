package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.concurrent.TimeUnit;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerLoadedEvent;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PowerfulPermissionManager extends PermissionManagerBase implements Listener {

    private PowerfulPerms plugin;

    public PowerfulPermissionManager(Database database, PowerfulPerms plugin, String serverName) {
        super(database, plugin, serverName);
        this.plugin = plugin;
        loadGroups(true, true);
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
            e.registerIntent(plugin);
            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {

                @Override
                public void run() {
                    loadPlayer(e.getConnection().getUniqueId(), e.getConnection().getName(), true, true);
                    debug("LoginEvent uuid " + e.getConnection().getUniqueId().toString());
                    e.completeIntent(plugin);
                }
            }, 0, TimeUnit.MILLISECONDS);
        } else
            debug("LoginEvent player not allowed");
    }

    @EventHandler(priority = EventPriority.LOW)
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
    public void reloadPlayers() {
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (containsPermissionPlayer(p.getUniqueId())) {
                loadPlayer(p.getUniqueId(), p.getName(), false, false);
            } else {
                loadPlayer(p.getUniqueId(), p.getName(), true, true);
                loadCachedPlayer(p);
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