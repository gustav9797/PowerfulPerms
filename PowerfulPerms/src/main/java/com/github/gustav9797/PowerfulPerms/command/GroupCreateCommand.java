package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class GroupCreateCommand extends SubCommand {

    public GroupCreateCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> create (ladder) (rank)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.create")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("create")) {

                final String groupName = args[0];

                String ladder = "default";
                if (args.length >= 3)
                    ladder = args[2];
                int rank = 100;
                if (args.length >= 4) {
                    try {
                        rank = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sendSender(invoker, sender, getUsage());
                        return CommandResult.success;
                    }
                }

                Response response = permissionManager.createGroupBase(groupName, ladder, rank);
                sendSender(invoker, sender, response.getResponse());
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "create".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("create");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
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
