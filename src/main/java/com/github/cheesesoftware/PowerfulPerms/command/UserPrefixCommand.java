package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class UserPrefixCommand extends SubCommand {

    public UserPrefixCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> prefix set/remove <prefix>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.prefix")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("prefix")) {
                if (args.length == 3 && !args[2].equalsIgnoreCase("remove")) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];

                ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
                Futures.addCallback(first, new FutureCallback<UUID>() {

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onSuccess(UUID result2) {
                        final UUID uuid = result2;
                        if (uuid == null) {
                            sendSender(invoker, sender, "Could not find player UUID.");
                            return;
                        } else {
                            if (args.length >= 4 && args[2].equalsIgnoreCase("set")) {
                                String prefix = "";
                                if (args[3].length() >= 1 && args[3].toCharArray()[0] == '"') {
                                    // Input is between quote marks.
                                    String result = "";
                                    result += args[3].substring(1) + " ";

                                    if (args.length >= 5) {
                                        for (int i = 4; i < args.length; i++) {
                                            result += args[i] + " ";
                                        }
                                    }

                                    if (result.toCharArray()[result.length() - 1] == ' ')
                                        result = result.substring(0, result.length() - 1);
                                    if (result.toCharArray()[result.length() - 1] == '"')
                                        result = result.substring(0, result.length() - 1);

                                    prefix = result;
                                } else
                                    prefix = args[3];

                                ListenableFuture<Response> second = permissionManager.setPlayerPrefix(uuid, prefix);
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

                            } else if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                                ListenableFuture<Response> second = permissionManager.setPlayerPrefix(uuid, "");
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
                            } else {
                                ListenableFuture<String> second = permissionManager.getPlayerOwnPrefix(uuid);
                                Futures.addCallback(second, new FutureCallback<String>() {

                                    @Override
                                    public void onFailure(Throwable t) {
                                        t.printStackTrace();
                                    }

                                    @Override
                                    public void onSuccess(String result) {
                                        sendSender(invoker, sender, "Prefix for player(non-inherited) " + playerName + ": \"" + (result != null ? result : "") + "\"");
                                    }
                                });
                            }
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
