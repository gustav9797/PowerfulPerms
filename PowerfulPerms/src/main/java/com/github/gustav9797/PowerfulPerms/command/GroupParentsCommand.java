package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class GroupParentsCommand extends SubCommand {

    public GroupParentsCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> parents add/remove <parent>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.parents")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("parents")) {
                if (args.length == 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String groupName = args[0];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }
                int groupId = group.getId();

                if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    String parent = args[3];
                    final Group parentGroup = permissionManager.getGroup(parent);
                    if (parentGroup == null) {
                        sendSender(invoker, sender, "Parent group does not exist.");
                        return CommandResult.success;
                    }
                    int parentId = parentGroup.getId();
                    Response response = permissionManager.addGroupParentBase(groupId, parentId);
                    sendSender(invoker, sender, response.getResponse());
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                    String parent = args[3];
                    final Group parentGroup = permissionManager.getGroup(parent);
                    if (parentGroup == null) {
                        sendSender(invoker, sender, "Parent group does not exist.");
                        return CommandResult.success;
                    }
                    int parentId = parentGroup.getId();
                    Response response = permissionManager.removeGroupParentBase(groupId, parentId);
                    sendSender(invoker, sender, response.getResponse());
                } else {
                    // List parents
                    sendSender(invoker, sender, "Listing parents for group " + groupName + ":");

                    if (group.getParents() != null && group.getParents().size() > 0) {
                        for (Group g : group.getParents())
                            sendSender(invoker, sender, g.getName());
                    } else
                        sendSender(invoker, sender, "Group has no parents.");
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "parents".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("parents");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("parents")) {
            List<String> output = new ArrayList<>();
            if ("add".startsWith(args[1].toLowerCase()))
                output.add("add");
            if ("remove".startsWith(args[1].toLowerCase()))
                output.add("remove");
            return output;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("parents")) {
            List<String> output = new ArrayList<>();
            for (Group group : permissionManager.getGroups().values()) {
                if (group.getName().toLowerCase().startsWith(args[2].toLowerCase()))
                    output.add(group.getName());
            }
            return output;
        }
        return null;
    }
}
