package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.CachedGroup;
import com.github.cheesesoftware.PowerfulPerms.common.IPlugin;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionsPlayerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PowerfulPermission;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PermissionsPlayer extends PermissionsPlayerBase {
    private ProxiedPlayer player;

    public PermissionsPlayer(ProxiedPlayer p, HashMap<String, List<CachedGroup>> serverGroups, ArrayList<PowerfulPermission> permissions, String prefix, String suffix, IPlugin plugin) {
        super(serverGroups, permissions, prefix, suffix, plugin);
        this.player = p;
    }

    public PermissionsPlayer(ProxiedPlayer p, PermissionsPlayerBase base, IPlugin plugin) {
        super(base.getCachedGroups(), base.getPermissions(), base.getPrefix(), base.getSuffix(), plugin);
        this.player = p;
    }

    /**
     * Update this PermissionsPlayerBase with data from another one.
     */
    @Override
    public void update(PermissionsPlayerBase base) {
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
    public void setGroups(HashMap<String, List<CachedGroup>> serverGroups) {
        super.setGroups(serverGroups);
        this.updatePermissions();
    }

    /**
     * Internal function to update the permissions.
     */
    public void updatePermissions() {
        if (this.player.getServer() != null)
            this.updatePermissions(this.player.getServer().getInfo());
    }

    /**
     * Internal function to update the PermissionAttachment.
     */
    public void updatePermissions(ServerInfo serverInfo) {
        this.updateGroups(serverInfo.getName());
        this.realPermissions = super.calculatePermissions(serverInfo.getName(), null);
    }
}
