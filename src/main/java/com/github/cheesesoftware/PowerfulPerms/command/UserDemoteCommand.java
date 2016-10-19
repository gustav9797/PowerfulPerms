package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

public class UserDemoteCommand extends SubCommand {

    public UserDemoteCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> demote <ladder>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.demote") || (args != null && args.length >= 3 && hasPermission(invoker, sender, "powerfulperms.user.demote." + args[2]))) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("demote")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String ladder = args[2];

                ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
                final UUID uuid = first.get();
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
                } else {
                    ListenableFuture<Response> second = permissionManager.demotePlayer(uuid, ladder);
                    sendSender(invoker, sender, second.get().getResponse());
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "demote".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("demote");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("demote")) {
            List<String> output = new ArrayList<String>();
            for (Group group : permissionManager.getGroups().values()) {
                if (group.getLadder().toLowerCase().startsWith(args[1].toLowerCase()) && !output.contains(group.getLadder()))
                    output.add(group.getLadder());
            }
            return output;
        }
        return null;
    }
}
