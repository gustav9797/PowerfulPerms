package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

public class GroupSetLadderCommand extends SubCommand {

    public GroupSetLadderCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> setladder <ladder>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.setladder")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("setladder")) {
                if (args.length == 2) {
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

                String ladder = args[2];
                ListenableFuture<Response> first = permissionManager.setGroupLadder(groupId, ladder);
                sendSender(invoker, sender, first.get().getResponse());
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public Iterable<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "setladder".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("setladder");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setladder")) {
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
