package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class PowerfulPermissionPlayer extends PermissionPlayerBase {
    private Player player;

    public PowerfulPermissionPlayer(Player p, HashMap<String, List<CachedGroup>> serverGroups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin) {
        super(serverGroups, permissions, prefix, suffix, plugin);
        this.player = p;
        this.updatePermissions();
    }

    public PowerfulPermissionPlayer(Player p, PermissionPlayerBase base, PowerfulPermsPlugin plugin) {
        super(base.getCachedGroups(), base.getPermissions(), base.getOwnPrefix(), base.getOwnSuffix(), plugin);
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
     * Returns the player attached to this PermissionsPlayer.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Sets the player's groups as seen in getServerGroups() Changes won't save for now.
     */
    @Override
    public void setGroups(HashMap<String, List<CachedGroup>> serverGroups) {
        super.setGroups(serverGroups);
        this.updatePermissions();
    }

    /**
     * Internal function to update the permissions of this PermissionPlayer. Run for example when the player has changed world.
     */
    public void updatePermissions() {
        this.updateGroups(PermissionManagerBase.serverName);
        
        List<String> perms = super.calculatePermissions(PermissionManagerBase.serverName, player.getWorld().getName());
        List<String> realPerms = new ArrayList<String>();
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
                realPerms.addAll(calculateChildPermissions(perm.getChildren(), invert));
        }

        this.realPermissions = realPerms;
    }

    private List<String> calculateChildPermissions(Map<String, Boolean> children, boolean invert) {
        Set<String> keys = children.keySet();
        if (keys.size() > 0) {
            List<String> perms = new ArrayList<String>();

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
                    perms.addAll(calculateChildPermissions(perm.getChildren(), !value));
                }
            }

            return perms;
        }
        return new ArrayList<String>();
    }

}
