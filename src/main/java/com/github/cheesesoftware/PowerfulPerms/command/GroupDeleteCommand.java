package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class GroupDeleteCommand extends SubCommand {

    public GroupDeleteCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> delete");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.delete")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("delete")) {

                final String groupName = args[0];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }
                int groupId = group.getId();

                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    sendSender(invoker, sender, "You are about to delete a group. Confirm with \"/pp group " + groupName + " delete confirm\"");
                    return CommandResult.success;
                } else {
                    ListenableFuture<Response> first = permissionManager.deleteGroup(groupId);
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
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }
}
