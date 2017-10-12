package com.github.gustav9797.PowerfulPerms.Bungee;

import java.util.LinkedHashMap;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.PermissionPlayerBase;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PowerfulPermissionPlayer extends PermissionPlayerBase {
    private ProxiedPlayer player;

    public PowerfulPermissionPlayer(ProxiedPlayer p, LinkedHashMap<String, List<CachedGroup>> serverGroups, List<Permission> permissions, String prefix, String suffix, PowerfulPermsPlugin plugin,
            boolean isDefault) {
        super(serverGroups, permissions, prefix, suffix, plugin, isDefault);
        this.player = p;
    }

    public PowerfulPermissionPlayer(ProxiedPlayer p, PermissionPlayerBase base, PowerfulPermsPlugin plugin) {
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
     * Returns the player attached to this PermissionsPlayer.
     */
    public ProxiedPlayer getPlayer() {
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
     * Internal function to update the permissions.
     */
    public void updatePermissions() {
        if (this.player.getServer() != null)
            this.updatePermissions(this.player.getServer().getInfo());
        else
            this.updatePermissions(null);
    }

    /**
     * Internal function to update the PermissionAttachment.
     */
    public void updatePermissions(ServerInfo serverInfo) {
        String serverName = (serverInfo != null ? serverInfo.getName() : null);
        this.updateGroups(serverName);
        List<Permission> allPerms = PermissionPlayerBase.getAllPermissions(super.getGroups(), this, plugin);
        super.setRealPermissions(super.calculatePermissions(serverName, null, super.getGroups(), this, allPerms, plugin));
        super.setAllPermissions(allPerms);
    }
}
