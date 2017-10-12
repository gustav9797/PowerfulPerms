package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class GroupSetRankCommand extends SubCommand {

    public GroupSetRankCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> setrank <rank>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.setrank")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("setrank")) {
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

                try {
                    int rank = Integer.parseInt(args[2]);
                    Response response = permissionManager.setGroupRankBase(groupId, rank);
                    sendSender(invoker, sender, response.getResponse());
                } catch (NumberFormatException e) {
                    sendSender(invoker, sender, "Rank must be a number.");
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("setrank".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("setrank");
            return output;
        }
        return null;
    }
}
