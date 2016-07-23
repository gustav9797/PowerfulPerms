package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

public class GroupParentsCommand extends SubCommand {

    public GroupParentsCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> parents add/remove <parent>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
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

                final ResponseRunnable response = new ResponseRunnable() {
                    @Override
                    public void run() {
                        sendSender(invoker, sender, response);
                    }
                };

                if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    String parent = args[3];
                    final Group parentGroup = permissionManager.getGroup(parent);
                    if (parentGroup == null) {
                        sendSender(invoker, sender, "Parent group does not exist.");
                        return CommandResult.success;
                    }
                    int parentId = parentGroup.getId();
                    permissionManager.addGroupParent(groupId, parentId, response);
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                    String parent = args[3];
                    final Group parentGroup = permissionManager.getGroup(parent);
                    if (parentGroup == null) {
                        sendSender(invoker, sender, "Parent group does not exist.");
                        return CommandResult.success;
                    }
                    int parentId = parentGroup.getId();
                    permissionManager.removeGroupParent(groupId, parentId, response);
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
}
