package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class UserPrefixCommand extends SubCommand {

    public UserPrefixCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> prefix set/remove <prefix>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.prefix")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("prefix")) {
                if (args.length == 3 && !args[2].equalsIgnoreCase("remove")) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];

                UUID uuid = permissionManager.getConvertUUIDBase(playerName);
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
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

                        Response response = permissionManager.setPlayerPrefixBase(uuid, prefix);
                        sendSender(invoker, sender, response.getResponse());

                    } else if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                        Response response = permissionManager.setPlayerPrefixBase(uuid, "");
                        sendSender(invoker, sender, response.getResponse());
                    } else {
                        String ownPrefix = permissionManager.getPlayerOwnPrefixBase(uuid);
                        sendSender(invoker, sender, "Prefix for player(non-inherited) " + playerName + ": \"" + (ownPrefix != null ? ownPrefix : "") + "\"");
                    }
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "prefix".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("prefix");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("prefix")) {
            List<String> output = new ArrayList<>();
            if ("set".startsWith(args[1].toLowerCase()))
                output.add("set");
            if ("remove".startsWith(args[1].toLowerCase()))
                output.add("remove");
            return output;
        }
        return null;
    }
}
