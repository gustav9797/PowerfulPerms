package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.github.cheesesoftware.PowerfulPerms.common.Group;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionsPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PermissionManager extends PermissionManagerBase implements Listener {

    private PowerfulPerms plugin;

    public PermissionManager(Database database, PowerfulPerms plugin, String serverName) {
        super(database, plugin, serverName);
        this.plugin = plugin;

        if (redis) {
            final String srvName = serverName;

            final Plugin tempPlugin = plugin;
            plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
                @SuppressWarnings("deprecation")
                public void run() {
                    Jedis jedis = null;
                    try {
                        jedis = pool.getResource();
                        subscriber = (new JedisPubSub() {
                            @Override
                            public void onMessage(String channel, final String msg) {
                                tempPlugin.getProxy().getScheduler().runAsync(tempPlugin, new Runnable() {
                                    public void run() {
                                        // Reload player or groups depending on message
                                        String[] split = msg.split(" ");
                                        if (split.length == 2) {
                                            String first = split[0];

                                            String server = split[1];

                                            if (server.equals(srvName))
                                                return;

                                            if (first.equals("[groups]")) {
                                                loadGroups();
                                                tempPlugin.getLogger().info(consolePrefix + "Reloaded all groups.");
                                            } else if (first.equals("[players]")) {
                                                loadGroups();
                                                tempPlugin.getLogger().info(consolePrefix + "Reloaded all players. ");
                                            } else {
                                                ProxiedPlayer player = tempPlugin.getProxy().getPlayer(first);
                                                if (player != null) {
                                                    loadPlayer(player);
                                                    tempPlugin.getLogger().info(consolePrefix + "Reloaded player \"" + first + "\".");
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        });
                        jedis.subscribe(subscriber, "PowerfulPerms");
                    } catch (Exception e) {
                        pool.returnBrokenResource(jedis);
                        tempPlugin.getLogger().warning(redisMessage);
                        return;
                    }
                    pool.returnResource(jedis);
                }
            });
        }

        loadGroups(true, true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        debug("PlayerQuitEvent " + e.getPlayer().getName());
        if (players.containsKey(e.getPlayer().getUniqueId()))
            players.remove(e.getPlayer().getUniqueId());
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
                    loadPlayer(e.getConnection().getUniqueId(), e.getConnection().getName(), true);
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
            continueLoadPlayer(e.getPlayer());
        } else
            debug("onPlayerJoin player isn't cached");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onServerConnected(final ServerConnectedEvent e) {
        debug("serverconnected event " + e.getServer().getInfo().getName());
        if (players.containsKey(e.getPlayer().getUniqueId())) {
            PermissionsPlayer player = (PermissionsPlayer) players.get(e.getPlayer().getUniqueId());
            player.updatePermissions(e.getServer().getInfo());
        }
    }

    /**
     * Loads online player data from database, removes old data.
     */
    private void loadPlayer(ProxiedPlayer player) {
        loadPlayer(player.getUniqueId(), player.getName(), false);
    }

    /**
     * Continues loading a previously cached player.
     */
    private void continueLoadPlayer(ProxiedPlayer player) {
        PermissionsPlayerBase base = super.loadCachedPlayer(player.getUniqueId());
        if (base != null) {
            if (player != null) {
                PermissionsPlayer permissionsPlayer = new PermissionsPlayer(player, base, plugin);
                players.put(player.getUniqueId(), permissionsPlayer);
            } else
                debug("continueLoadPlayer: ProxiedPlayer is null");
        }
    }

    /**
     * Returns the primary group of an online player.
     */
    public Group getPlayerPrimaryGroup(ProxiedPlayer p) {
        PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
        if (gp != null)
            return gp.getPrimaryGroup();
        return null;
    }

}