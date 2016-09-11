package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

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
                
                ListenableFuture<Response> first = permissionManager.createGroup(groupName, ladder, rank);
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
