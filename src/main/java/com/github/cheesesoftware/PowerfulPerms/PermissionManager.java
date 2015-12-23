package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.PermissionsPlayer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class PermissionManager extends PermissionManagerBase implements Listener {

    private PowerfulPerms plugin;
    private PermissibleBaseInjector injector;

    public PermissionManager(SQL sql, PowerfulPerms plugin) {
        super(sql, plugin);
        this.plugin = plugin;
        this.serverName = Bukkit.getServerName();
        
        this.injector = new PermissibleBaseInjector();

        final Plugin tempPlugin = plugin;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    subscriber = (new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, final String msg) {
                            Bukkit.getScheduler().runTaskAsynchronously(tempPlugin, new Runnable() {
                                public void run() {
                                    // Reload player or groups depending on message
                                    String[] split = msg.split(" ");
                                    if (split.length == 2) {
                                        String first = split[0];
                                        String server = split[1];

                                        if (server.equals(Bukkit.getServerName()))
                                            return;
                                        if (first.equals("[groups]")) {
                                            loadGroups();
                                            Bukkit.getLogger().info(consolePrefix + "Reloaded all groups.");
                                        } else if (first.equals("[players]")) {
                                            loadGroups();
                                            Bukkit.getLogger().info(consolePrefix + "Reloaded all players. ");
                                        } else {
                                            Player player = Bukkit.getPlayer(first);
                                            if (player != null) {
                                                loadPlayer(player);
                                                Bukkit.getLogger().info(consolePrefix + "Reloaded player \"" + first + "\".");
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
                    Bukkit.getLogger().warning(
                            consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                    return;
                }
                pool.returnResource(jedis);
            }
        });

        loadGroups();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        debug("PlayerQuitEvent " + e.getPlayer().getName());
        if (players.containsKey(e.getPlayer().getUniqueId())) {
            players.remove(e.getPlayer().getUniqueId());
        }
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId()))
            cachedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent e) {
        debug("AsyncPlayerPreLoginEvent " + e.getName());
        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loadPlayer(e.getUniqueId(), e.getName(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent e) {
        debug("PlayerLoginEvent " + e.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        debug("PlayerJoinEvent " + e.getPlayer().getName());
        
        // Check again if
        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId())) {
            // Player is cached. Continue load it.
            continueLoadPlayer(e.getPlayer().getUniqueId());
        } else {
            debug("onPlayerJoin player isn't cached, loading directly");
            loadPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName(), true);
            this.continueLoadPlayer(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        IPermissionsPlayer gp = this.getPermissionsPlayer(e.getPlayer());
        e.setFormat(ChatColor.translateAlternateColorCodes('&', gp.getPrefix() + "%1$s" + gp.getSuffix() + "%2$s"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        debug("Player " + p.getName() + " changed world from " + event.getFrom().getName() + " to " + p.getWorld().getName());
        if (players.containsKey(p.getUniqueId())) {
            PermissionsPlayer permissionsPlayer = (PermissionsPlayer) players.get(p.getUniqueId());
            permissionsPlayer.UpdatePermissions();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        players.clear();
    }

    @Override
    public void reloadPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(p.getUniqueId())) {
                PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
                gp.clearPermissions();
                players.remove(p.getUniqueId());
            }
            loadPlayer(p);
        }
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(Player p) {
        return players.get(p.getUniqueId());
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     */
    public IPermissionsPlayer getPermissionsPlayer(String playerName) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null)
            return players.get(p.getUniqueId());
        return null;
    }

    private void loadPlayer(Player player) {
        loadPlayer(player.getUniqueId(), player.getName(), false);
    }

    private void continueLoadPlayer(UUID uuid) {
        PermissionsPlayerBase base = super.loadCachedPlayer(uuid);
        if (base != null) {
            Player p = Bukkit.getServer().getPlayer(uuid);
            if(p != null) {
                if (players.containsKey(p.getUniqueId())) {
                    PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
                    PermissionAttachment toRemove = gp.getPermissionAttachment();
                    if (toRemove != null)
                        toRemove.remove();
                    players.remove(p.getUniqueId());
                }
                
                PermissionAttachment pa = p.addAttachment(plugin);
                PermissionsPlayer permissionsPlayer = new PermissionsPlayer(p, pa, base);
                try {
                    injector.inject(p, new CustomPermissibleBase(permissionsPlayer));
                } catch (NoSuchFieldException e) {
                    Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "You're not using Spigot. Spigot must be used for permissions to work properly. 2");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "Could not inject permissible. Using default Bukkit permissions. 2");
                    e.printStackTrace();
                }
                players.put(uuid, permissionsPlayer);
            }
            else
                debug("continueLoadPlayer: Player is null");
        }
    }

    /**
     * Returns the primary group of an online player.
     */
    public Group getPlayerPrimaryGroup(Player p) {
        PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
        if (gp != null)
            return gp.getPrimaryGroup();
        return null;
    }

    /**
     * Returns the primary group of a player.
     */
    public Group getPlayerPrimaryGroup(String playerName) {
        Player p = Bukkit.getServer().getPlayer(playerName);
        if (p != null)
            return getPlayerPrimaryGroup(p);
        HashMap<String, List<Group>> g = getPlayerGroups(playerName);
        List<Group> primary = g.get("");
        if(primary != null) {
            Iterator<Group> it = primary.iterator();
            return it.next(); // First group is primary group.
        }
        return null;
    }

    /**
     * Get the full list of groups a player has, if player isn't online it will be loaded from the database.
     */
    @Override
    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
        Player p = Bukkit.getServer().getPlayer(playerName);
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
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            PermissionsPlayer gp = (PermissionsPlayer) players.get(p.getUniqueId());
            if (gp != null)
                return gp.getPermissions();
        }

        // Load from DB
        return super.getPlayerPermissions(playerName);
    }

    /**
     * Gets the prefix of a player. Non-inherited.
     * 
     * @param p
     *            The player to get prefix from.
     */
    public String getPlayerPrefix(String playerName) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            IPermissionsPlayer gp = this.getPermissionsPlayer(playerName);
            if(gp != null)
                return gp.getPrefix();
        }
        return super.getPlayerPrefix(playerName);
    }

    /**
     * Gets the suffix of a player. Non-inherited.
     * 
     * @param p
     *            The player to get suffix from.
     */
    public String getPlayerSuffix(String playerName) {
        Player p = Bukkit.getPlayer(playerName);
        if (p != null) {
            IPermissionsPlayer gp = this.getPermissionsPlayer(playerName);
            if(gp != null)
                return gp.getSuffix();
        }
        return super.getPlayerSuffix(playerName);
    }

}