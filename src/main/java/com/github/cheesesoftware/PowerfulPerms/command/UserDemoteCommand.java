package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class UserDemoteCommand extends SubCommand {

    public UserDemoteCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> demote <ladder>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.demote") || (args != null && args.length >= 3 && hasPermission(invoker, sender, "powerfulperms.user.demote." + args[2]))) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("demote")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String ladder = args[2];

                ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
                Futures.addCallback(first, new FutureCallback<UUID>() {

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onSuccess(UUID result) {
                        final UUID uuid = result;
                        if (uuid == null) {
                            sendSender(invoker, sender, "Could not find player UUID.");
                            return;
                        } else {
                            ListenableFuture<Response> second = permissionManager.demotePlayer(uuid, ladder);
                            Futures.addCallback(second, new FutureCallback<Response>() {

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
                    }
                });

                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }
}
