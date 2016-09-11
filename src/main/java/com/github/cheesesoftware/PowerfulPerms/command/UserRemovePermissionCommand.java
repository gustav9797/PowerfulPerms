package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.Date;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class UserRemovePermissionCommand extends SubCommand {

    public UserRemovePermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> remove <permission> (server) (world) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.remove")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String permission = args[2];

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
                                    return;
                                }
                            }
                            if (server.equalsIgnoreCase("all"))
                                server = "";
                            if (world.equalsIgnoreCase("all"))
                                world = "";

                            ListenableFuture<Response> second = permissionManager.removePlayerPermission(uuid, permission, world, server, expires);
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
