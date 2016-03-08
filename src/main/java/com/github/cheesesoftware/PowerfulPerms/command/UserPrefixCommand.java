package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserPrefixCommand extends SubCommand {

    public UserPrefixCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> prefix set/remove <prefix>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.prefix")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("prefix")) {
                if (args.length == 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];

                final ResponseRunnable response = new ResponseRunnable() {
                    @Override
                    public void run() {
                        sendSender(invoker, sender, response);
                    }
                };

                permissionManager.getConvertUUID(playerName, new ResultRunnable<UUID>() {

                    @Override
                    public void run() {
                        final UUID uuid = result;
                        if (uuid == null) {
                            response.setResponse(false, "Could not find player UUID.");
                            permissionManager.getScheduler().runSync(response, response.isSameThread());
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

                                permissionManager.setPlayerPrefix(uuid, prefix, response);

                            } else if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                                permissionManager.setPlayerPrefix(uuid, "", response);
                            } else {
                                permissionManager.getPlayerOwnPrefix(uuid, new ResultRunnable<String>() {

                                    @Override
                                    public void run() {
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
