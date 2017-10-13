package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import java.util.List;
import java.util.Queue;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class GroupCommand extends SubCommand {

    public GroupCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group>");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group")) {
            if (args != null && args.length >= 1) {
                int page = -1;
                if (args.length == 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        return CommandResult.noMatch;
                    }
                } else if (args.length > 2)
                    return CommandResult.noMatch;

                if (page <= 0)
                    page = 1;
                page--;

                Group group = permissionManager.getGroup(args[0]);
                if (group != null) {
                    // List group permissions
                    Queue<String> rows = new java.util.ArrayDeque<>();

                    rows.add(ChatColor.BLUE + "Listing permissions for group " + group.getName() + ".");
                    rows.add(ChatColor.GREEN + "Ladder" + ChatColor.WHITE + ": " + group.getLadder());
                    rows.add(ChatColor.GREEN + "Rank" + ChatColor.WHITE + ": " + group.getRank());
                    List<Permission> permissions = group.getOwnPermissions();
                    if (permissions.size() > 0) {
                        for (Permission e : permissions) {
                            boolean s = !e.getServer().isEmpty();
                            boolean w = !e.getWorld().isEmpty();
                            boolean p = s || w || e.willExpire();
                            rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + (p ? " (" : "")
                                    + (e.getServer().isEmpty() ? "" : "Server:" + ChatColor.RED + e.getServer() + ChatColor.WHITE)
                                    + (e.getWorld().isEmpty() ? "" : (s ? " " : "") + "World:" + ChatColor.RED + e.getWorld() + ChatColor.WHITE)
                                    + (e.willExpire() ? ((s || w ? " " : "") + ChatColor.YELLOW + Utils.getExpirationDateString(e.getExpirationDate())) : "") + ChatColor.WHITE + (p ? ")" : ""));
                        }
                    } else
                        rows.add("Group has no permissions.");

                    List<List<String>> list = Paginator.createList(rows, 19);
                    if (list.size() > 0) {
                        sendSender(invoker, sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());
                        if (page < list.size()) {
                            for (String s : list.get(page))
                                sendSender(invoker, sender, s);
                        } else
                            sendSender(invoker, sender, "Invalid page. Page too high. ");
                    }
                } else
                    sendSender(invoker, sender, "Group does not exist.");
                return CommandResult.success;

            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        return null;
    }

}
