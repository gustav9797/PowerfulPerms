package com.github.cheesesoftware.PowerfulPerms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermissionPlayer;
import com.github.cheesesoftware.PowerfulPerms.common.ChatColor;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionContainer;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerMiddle;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.database.Database;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PlayerLoadedEvent;
import com.google.common.util.concurrent.ListenableFuture;

public class PowerfulPermissionManager extends PermissionManagerMiddle implements Listener {

    private PermissibleBaseInjector injector;

    public PowerfulPermissionManager(Database database, PowerfulPerms plugin, String serverName) {
        super(database, plugin, serverName);
        this.injector = new PermissibleBaseInjector();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        debug("PlayerQuitEvent " + e.getPlayer().getName());

        removePermissionPlayer(e.getPlayer().getUniqueId());
        cachedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent e) {
        debug("AsyncPlayerPreLoginEvent " + e.getName());

        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loadPlayer(e.getUniqueId(), e.getName(), true, true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(final PlayerLoginEvent e) {
        debug("PlayerLoginEvent " + e.getPlayer().getName());
        debug("Player world " + e.getPlayer().getWorld().getName());

        if (cachedPlayers.containsKey(e.getPlayer().getUniqueId())) {
            // Player is cached. Continue load it.s
            loadCachedPlayer(e.getPlayer());
        } else {
            // Player is not cached, Load directly on Bukkit main thread.
            debug("onPlayerLogin player isn't cached, loading directly");
            loadPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName(), true, true);

            if (e.getResult() == PlayerLoginEvent.Result.ALLOWED)
                loadCachedPlayer(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent e) {
        debug("PlayerLoginEvent Monitor " + e.getPlayer().getName());

        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            debug("onPlayerLoginMonitor player not allowed, removing cached");
            cachedPlayers.remove(e.getPlayer().getUniqueId());
            removePermissionPlayer(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        debug("PlayerJoinEvent " + e.getPlayer().getName());
        Player p = e.getPlayer();

        if (!containsPermissionPlayer(p.getUniqueId())) {
            // Player is not cached, Load directly on Bukkit main thread.
            debug("onPlayerJoin player isn't loaded, loading directly");
            loadPlayer(e.getPlayer().getUniqueId(), e.getPlayer().getName(), true, true);
            loadCachedPlayer(e.getPlayer());
        }

        if (containsPermissionPlayer(p.getUniqueId())) {
            PowerfulPermissionPlayer permissionsPlayer = (PowerfulPermissionPlayer) getPermissionPlayer(p.getUniqueId());
            permissionsPlayer.updatePermissions();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (!PowerfulPerms.disableChatFormat) {
            PermissionPlayer gp = this.getPermissionPlayer(e.getPlayer().getUniqueId());
            if (PowerfulPerms.useChatFormat && PowerfulPerms.placeholderAPIEnabled) {
                String output = me.clip.placeholderapi.PlaceholderAPI.setBracketPlaceholders(e.getPlayer(), PowerfulPerms.chatFormat);
                e.setFormat(ChatColor.translateAlternateColorCodes('&', output.replace("{message}", "%2$s")));
            } else
                e.setFormat(ChatColor.translateAlternateColorCodes('&', gp.getPrefix() + "%1$s" + gp.getSuffix() + "%2$s"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        debug("Player " + p.getName() + " changed world from " + e.getFrom().getName() + " to " + p.getWorld().getName());
        if (containsPermissionPlayer(p.getUniqueId())) {
            PowerfulPermissionPlayer permissionPlayer = (PowerfulPermissionPlayer) getPermissionPlayer(p.getUniqueId());
            permissionPlayer.updatePermissions();
        }
    }

    @Override
    public ListenableFuture<Boolean> playerHasPermission(UUID uuid, String permission, String world, String server) {
        ListenableFuture<Boolean> first = service.submit(() -> {
            PermissionPlayer player = getPermissionPlayer(uuid);
            if (player != null) {
                if ((world == null || world.isEmpty() || world.equalsIgnoreCase("all")) && (server == null || server.isEmpty() || server.equalsIgnoreCase("all")))
                    return player.hasPermission(permission);
                PermissionContainer permissionContainer = new PermissionContainer(player.getPermissions());
                List<String> perms = PermissionPlayerBase.calculatePermissions(server, world, player.getGroups(), permissionContainer, plugin);
                List<String> realPerms = PowerfulPermissionPlayer.calculateRealPermissions(perms, plugin);
                permissionContainer.setRealPermissions(realPerms);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                permissionContainer.setTemporaryPrePermissions(CustomPermissibleBase.getTemporaryPrePermissions((offlinePlayer != null ? offlinePlayer.isOp() : false)));
                return permissionContainer.hasPermission(permission);
            }

            LinkedHashMap<String, List<CachedGroup>> currentGroups = getPlayerCurrentGroupsBase(uuid);
            List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(server, currentGroups);
            List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups, plugin);
            PermissionContainer permissionContainer = new PermissionContainer(getPlayerOwnPermissionsBase(uuid));
            List<String> perms = PermissionPlayerBase.calculatePermissions(server, world, groups, permissionContainer, plugin);
            // Player own permissions have been added. Permissions from player groups have been added. In relation to world and server.

            List<String> realPerms = PowerfulPermissionPlayer.calculateRealPermissions(perms, plugin);
            permissionContainer.setRealPermissions(realPerms);
            // Child permissions have been added.

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            permissionContainer.setTemporaryPrePermissions(CustomPermissibleBase.getTemporaryPrePermissions((offlinePlayer != null ? offlinePlayer.isOp() : false)));
            // Default Bukkit permissions have been added.

            return permissionContainer.hasPermission(permission);
        });
        return first;
    }

    @Override
    public void reloadPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (containsPermissionPlayer(p.getUniqueId())) {
                loadPlayer(p.getUniqueId(), p.getName(), false, false);
            } else {
                loadPlayer(p.getUniqueId(), p.getName(), true, true);
                loadCachedPlayer(p);
            }
        }
    }

    private void loadCachedPlayer(Player p) {
        debug("loadCachedPlayer begin");

        playersLock.lock();
        try {
            if (players.containsKey(p.getUniqueId())) {
                players.remove(p.getUniqueId());
            }
        } finally {
            playersLock.unlock();
        }

        PermissionPlayerBase base = super.loadCachedPlayer(p.getUniqueId());
        if (base != null) {
            removePermissionPlayer(p.getUniqueId());

            PowerfulPermissionPlayer permissionsPlayer = new PowerfulPermissionPlayer(p, base, plugin);
            try {
                injector.inject(p, new CustomPermissibleBase(permissionsPlayer));
            } catch (NoSuchFieldException e) {
                Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "You're not using Spigot. Spigot must be used for permissions to work properly.");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Could not inject permissible. Using default Bukkit permissions.");
                e.printStackTrace();
            }
            putPermissionPlayer(p.getUniqueId(), permissionsPlayer);
            checkPlayerTimedGroupsAndPermissions(p.getUniqueId(), permissionsPlayer);
            eventHandler.fireEvent(new PlayerLoadedEvent(p.getUniqueId(), p.getName()));
        }
        debug("loadCachedPlayer end");
    }

}