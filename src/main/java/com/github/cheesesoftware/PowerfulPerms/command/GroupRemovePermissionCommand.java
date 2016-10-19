package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

public class GroupRemovePermissionCommand extends SubCommand {

    public GroupRemovePermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> remove <permission> (server) (world) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
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

                String permission = args[2];
                String world = "";
                String server = "";
                Date expires = null;
                if (args.length >= 4)
                    server = args[3];
                if (args.length >= 5)
                    world = args[4];
                if (args.length >= 6) {
                    expires = Utils.getDate(args[5]);
                    if (expires == null) {
                        sendSender(invoker, sender, "Invalid expiration format.");
                        return CommandResult.success;
                    }
                }
                if (server.equalsIgnoreCase("all"))
                    server = "";
                if (world.equalsIgnoreCase("all"))
                    world = "";

                ListenableFuture<Response> first = permissionManager.removeGroupPermission(groupId, permission, world, server, expires);
                sendSender(invoker, sender, first.get().getResponse());
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("remove".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("remove");
            return output;
        }
        // TODO: suggest permission + world + server that the group has
        return null;
    }
}
