package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class UserAddGroupCommand extends SubCommand {

    public UserAddGroupCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> addgroup <group> (server) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.addgroup") || (args != null && args.length >= 3 && hasPermission(invoker, sender, "powerfulperms.user.addgroup." + args[2]))) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("addgroup")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                String groupName = args[2];
                final boolean negated = groupName.startsWith("-");
                if (negated)
                    groupName = groupName.substring(1);
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }
                final int groupId = group.getId();

                UUID uuid = permissionManager.getConvertUUIDBase(playerName);
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
                } else {
                    String server = "";
                    Date expires = null;
                    if (args.length >= 4)
                        server = args[3];
                    if (args.length >= 5) {
                        expires = Utils.getDate(args[4]);
                        if (expires == null) {
                            sendSender(invoker, sender, "Invalid expiration format.");
                            return CommandResult.success;
                        }
                    }
                    Response response = permissionManager.addPlayerGroupBase(uuid, groupId, server, negated, expires);
                    sendSender(invoker, sender, response.getResponse());
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "addgroup".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("addgroup");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("addgroup")) {
            List<String> output = new ArrayList<>();
            for (Group group : permissionManager.getGroups().values()) {
                if (group.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                    output.add(group.getName());
            }
            return output;
        }
        return null;
    }
}
