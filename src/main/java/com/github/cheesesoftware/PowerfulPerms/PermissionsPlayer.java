package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PermissionsPlayer extends PermissionsPlayerBase {
    private Player player;

    public PermissionsPlayer(Player p, HashMap<String, List<Group>> serverGroups, ArrayList<PowerfulPermission> permissions, String prefix, String suffix) {
        super(serverGroups, permissions, prefix, suffix);
        this.player = p;
        this.UpdatePermissions();
    }
    
    public PermissionsPlayer(Player p, PermissionsPlayerBase base) {
        super(base.getServerGroups(), base.getPermissions(), base.getPrefix(), base.getSuffix());
        this.player = p;
        this.UpdatePermissions();
    }
    
    /**
     * Update this PermissionsPlayerBase with data from another one.
     */
    @Override
    public void update(PermissionsPlayerBase base) {
        super.update(base);
        this.UpdatePermissions();
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
    public void setServerGroups(HashMap<String, List<Group>> serverGroups) {
        super.setServerGroups(serverGroups);
        this.UpdatePermissions();
    }
    
    /**
     * Internal function to update the permissions of this PermissionPlayer.
     * Run for example when the player has changed world.
     */
    public void UpdatePermissions() {
        //Map<String, Boolean> destination = reflectMap(pa);
        //destination.clear();
        this.realPermissions = super.calculatePermissions(Bukkit.getServer().getServerName(), player.getWorld().getName());
        //destination.putAll(this.realPermissions);
        //player.recalculatePermissions();
    }

    /*private Field pField;

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> reflectMap(PermissionAttachment attachment) {
        try {
            if (pField == null) {
                pField = PermissionAttachment.class.getDeclaredField("permissions");
                pField.setAccessible(true);
            }
            return (Map<String, Boolean>) pField.get(attachment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/

}
