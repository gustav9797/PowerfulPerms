package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.PermissionsPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;
import com.github.cheesesoftware.PowerfulPerms.SQL;

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

    public PermissionManager(SQL sql, PowerfulPerms plugin) {
        super(sql, plugin);
        this.plugin = plugin;
        this.serverName = "bungeeproxy" + (new Random()).nextInt(5000) + (new Date()).getTime();

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

                                        if (server.equals(serverName))
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
                    tempPlugin.getLogger().warning(
                            PowerfulPerms.pluginPrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                    return;
                }
                pool.returnResource(jedis);
            }
        });

        loadGroups();
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
        }
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
            player.UpdatePermissions(e.getServer().getInfo());
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
                PermissionsPlayer permissionsPlayer = new PermissionsPlayer(player, base);
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

    /**
     * Returns the primary group of a player.
     */
    public Group getPlayerPrimaryGroup(String playerName) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null)
            return getPlayerPrimaryGroup(p);
        Iterator<Group> it = getPlayerGroups(playerName).get("").iterator();
        return it.next(); // First group is primary group.
    }

    /**
     * Get the full list of groups a player has, if player isn't online it will be loaded from the database.
     */
    @Override
    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null) {
            PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
            if (gp != null)
                return gp.getServerGroups();
        }
        // Player is not online, load from MySQL
        return super.getPlayerGroups(playerName);
    }

    /**
     * Gets a map containing all the permissions a player has, including derived permissions. If player is not online data will be loaded from DB and will not return world-specific or server-specific
     * permissions.
     * 
     * @param p
     *            The player to get permissions from.
     */
    @Override
    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {
        ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
        if (p != null) {
            PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
            if (gp != null)
                return gp.getPermissions();
        }

        // Load from DB
        return super.getPlayerPermissions(playerName);
    }

    /**
     * Does a proper permissions check on the specified player. Same as PermissionsPlayer.hasPermission(Sting permission)
     */
    /*
     * public boolean getPlayerHasPermission(ProxiedPlayer player, String permission) { IPermissionsPlayer permissionsPlayer = players.get(player.getUniqueId()); if (permissionsPlayer != null) {
     * boolean hasPermission = permissionsPlayer.hasPermission(permission); debug("Permission check of " + permission + " on player " + player.getName() + " is " + hasPermission); return
     * hasPermission; } return false; }
     */

}