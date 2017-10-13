package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import com.github.gustav9797.PowerfulPerms.database.DBResult;
import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.DBDocument;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class GroupMembersCommand extends SubCommand {

    public GroupMembersCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> members");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.members") || (args != null && args.length >= 2 && hasPermission(invoker, sender, "powerfulperms.group.members." + args[0]))) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("members")) {

                final String groupName = args[0];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }

                int page = -1;
                if (args.length == 3) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        return CommandResult.noMatch;
                    }
                } else if (args.length > 3)
                    return CommandResult.noMatch;

                if (page <= 0)
                    page = 1;
                page--;

                final List<String> rows = new ArrayList<>();
                rows.add(ChatColor.BLUE + "Listing members in group " + groupName + ".");
                int rowsPerPage = 18;
                DBResult result = permissionManager.getDatabase().getPlayersInGroup(group.getId(), rowsPerPage, page * rowsPerPage);
                while (result.hasNext()) {
                    DBDocument row = result.next();
                    rows.add(row.getString("name"));
                }
                if (result.getRows() == 0)
                    rows.add("Group has no members or page number too high.");

                sendSender(invoker, sender, ChatColor.BLUE + "Page " + (page + 1));

                for (String s : rows)
                    sendSender(invoker, sender, s);

                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "members".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("members");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("members")) {
            List<String> output = new ArrayList<>();
            for (Group group : permissionManager.getGroups().values()) {
                if (group.getLadder().toLowerCase().startsWith(args[1].toLowerCase()) && !output.contains(group.getLadder()))
                    output.add(group.getLadder());
            }
            return output;
        }
        return null;
    }
}
