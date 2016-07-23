package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

public class GroupRemovePermissionCommand extends SubCommand {

    public GroupRemovePermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> remove <permission> (server) (world)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.remove")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) {
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

                String permission = args[2];
                String world = "";
                String server = "";
                if (args.length >= 4)
                    server = args[3];
                if (args.length >= 5)
                    world = args[4];
                if (server.equalsIgnoreCase("all"))
                    server = "";
                if (world.equalsIgnoreCase("all"))
                    world = "";
                permissionManager.removeGroupPermission(groupId, permission, world, server, response);
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }
}
