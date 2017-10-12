package com.github.gustav9797.PowerfulPerms;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPerms.common.PermissionPlayerBase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class PowerfulPermissionPlayer extends PermissionPlayerBase {
    private Player player;

    public PowerfulPermissionPlayer(Player p, LinkedHashMap<String, List<CachedGroup>> serverGroups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin,
            boolean isDefault) {
        super(serverGroups, permissions, prefix, suffix, plugin, isDefault);
        this.player = p;
        this.updatePermissions();
    }

    public PowerfulPermissionPlayer(Player p, PermissionPlayerBase base, PowerfulPermsPlugin plugin) {
        super(base.getCachedGroups(), base.getPermissions(), base.getOwnPrefix(), base.getOwnSuffix(), plugin, base.isDefault());
        this.player = p;
        this.updatePermissions();
    }

    /**
     * Update this PermissionsPlayerBase with data from another one.
     */
    @Override
    public void update(PermissionPlayerBase base) {
        super.update(base);
        this.updatePermissions();
    }

    /**
     * Returns the player attached to this PermissionPlayer.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Sets the player's groups as seen in getServerGroups() Changes won't save for now.
     */
    @Override
    public void setGroups(LinkedHashMap<String, List<CachedGroup>> serverGroups) {
        super.setGroups(serverGroups);
        this.updatePermissions();
    }

    /**
     * Internal function to update the permissions of this PermissionPlayer. Run for example when the player has changed world.
     */
    public void updatePermissions() {
        this.updateGroups(PermissionManagerBase.serverName);

        List<Permission> allPerms = PermissionPlayerBase.getAllPermissions(super.getGroups(), this, plugin);
        List<String> perms = PermissionPlayerBase.calculatePermissions(PermissionManagerBase.serverName, player.getWorld().getName(), super.getGroups(), this, allPerms, plugin);
        List<String> realPerms = calculateRealPermissions(perms, plugin);
        super.setRealPermissions(realPerms);
        super.setAllPermissions(allPerms);
    }

    public static List<String> calculateRealPermissions(List<String> perms, PowerfulPermsPlugin plugin) {
        List<String> realPerms = new ArrayList<>();
        for (String permString : perms) {
            boolean invert = false;
            if (permString.startsWith("-")) {
                realPerms.add(permString);
                invert = true;
                if (permString.length() > 1)
                    permString = permString.substring(1);
            } else
                realPerms.add(permString);
            org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission(permString);
            if (perm != null)
                realPerms.addAll(calculateChildPermissions(perm.getChildren(), invert, plugin));
        }
        return realPerms;
    }

    private static List<String> calculateChildPermissions(Map<String, Boolean> children, boolean invert, PowerfulPermsPlugin plugin) {
        Set<String> keys = children.keySet();
        if (keys.size() > 0) {
            List<String> perms = new ArrayList<>();

            for (String name : keys) {
                org.bukkit.permissions.Permission perm = Bukkit.getServer().getPluginManager().getPermission(name);
                boolean value = children.get(name) ^ invert;
                String lname = name.toLowerCase();

                if (value == true)
                    perms.add(lname);
                else if (value == false)
                    perms.add("-" + lname);

                plugin.debug("added perm " + lname + " value " + value);

                if (perm != null) {
                    perms.addAll(calculateChildPermissions(perm.getChildren(), !value, plugin));
                }
            }

            return perms;
        }
        return new ArrayList<>();
    }

}
