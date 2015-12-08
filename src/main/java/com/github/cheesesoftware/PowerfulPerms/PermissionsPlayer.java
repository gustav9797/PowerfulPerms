package com.github.cheesesoftware.PowerfulPerms;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class PermissionsPlayer extends PermissionsPlayerBase {
    private Player player;
    private PermissionAttachment pa;

    public PermissionsPlayer(Player p, HashMap<String, List<Group>> serverGroups, ArrayList<PowerfulPermission> permissions, PermissionAttachment pa, String prefix, String suffix) {
        super(serverGroups, permissions, prefix, suffix);
        this.player = p;
        this.pa = pa;
        this.UpdatePermissions();
    }
    
    public PermissionsPlayer(Player p, PermissionAttachment pa, PermissionsPlayerBase base) {
        super(base.getServerGroups(), base.getPermissions(), base.getPrefix(), base.getSuffix());
        this.player = p;
        this.pa = pa;
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
     * Returns the PermissionAttachment used with Bukkit.
     */
    public PermissionAttachment getPermissionAttachment() {
        return this.pa;
    }

    /**
     * Clears the player-specific permissions of this player. Changes won't save for now.
     */
    @Override
    public void clearPermissions() {
        super.clearPermissions();
        this.UpdatePermissions();
    }

    /**
     * Internal function to update the PermissionAttachment.
     */
    public void UpdatePermissions() {
        Map<String, Boolean> destination = reflectMap(pa);
        destination.clear();
        this.realPermissions = super.calculatePermissions(Bukkit.getServer().getServerName(), player.getWorld().getName());
        destination.putAll(this.realPermissions);
        player.recalculatePermissions();
    }

    private Field pField;

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
    }

}
