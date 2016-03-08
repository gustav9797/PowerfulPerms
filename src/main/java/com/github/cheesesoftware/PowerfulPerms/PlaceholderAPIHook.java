package com.github.cheesesoftware.PowerfulPerms;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

import me.clip.placeholderapi.external.EZPlaceholderHook;

public class PlaceholderAPIHook extends EZPlaceholderHook {

    PowerfulPermsPlugin plugin;

    public PlaceholderAPIHook(Plugin plugin, String placeholderName) {
        super(plugin, placeholderName);
        this.plugin = (PowerfulPermsPlugin) plugin;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null)
            return "";
        PermissionPlayer permissionPlayer = plugin.getPermissionManager().getPermissionPlayer(player.getUniqueId());
        if (permissionPlayer == null)
            return "";
        if (identifier.equals("prefix")) {
            String out = permissionPlayer.getPrefix();
            if (out != null)
                return out;
            return "";
        } else if (identifier.equals("suffix")) {
            String out = permissionPlayer.getSuffix();
            if (out != null)
                return out;
            return "";
        } else if (identifier.equals("ownprefix")) {
            String out = permissionPlayer.getOwnPrefix();
            if (out != null)
                return out;
            return "";
        } else if (identifier.equals("ownsuffix")) {
            String out = permissionPlayer.getOwnSuffix();
            if (out != null)
                return out;
            return "";
        } else if (identifier.equals("group")) {
            List<Group> groups = permissionPlayer.getGroups();
            if (groups != null && groups.size() > 0) {
                String out = groups.get(0).getName();
                if (out != null)
                    return out;
                return "";
            }
            return "";
        } else if (identifier.startsWith("prefix_")) {
            String ladder = identifier.replace("prefix_", "");
            String out = permissionPlayer.getPrefix(ladder);
            if (out != null)
                return out;
            return "";
        } else if (identifier.startsWith("suffix_")) {
            String ladder = identifier.replace("suffix_", "");
            String out = permissionPlayer.getSuffix(ladder);
            if (out != null)
                return out;
            return "";
        } else if (identifier.startsWith("group_")) {
            String ladder = identifier.replace("group_", "");
            List<Group> groups = permissionPlayer.getGroups();
            for (Group group : groups) {
                if (group.getLadder().equalsIgnoreCase(ladder)) {
                    String out = group.getName();
                    if (out != null)
                        return out;
                }
            }
            return "";
        }
        return null;
    }
}
