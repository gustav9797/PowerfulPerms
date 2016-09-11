package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.Date;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class GroupAddPermissionCommand extends SubCommand {

    public GroupAddPermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> add <permission> (server) (world) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.add")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("add")) {
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
                ListenableFuture<Response> first = permissionManager.addGroupPermission(groupId, permission, world, server, expires);
                Futures.addCallback(first, new FutureCallback<Response>() {

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onSuccess(Response result) {
                        sendSender(invoker, sender, result.getResponse());
                    }
                });
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

}
