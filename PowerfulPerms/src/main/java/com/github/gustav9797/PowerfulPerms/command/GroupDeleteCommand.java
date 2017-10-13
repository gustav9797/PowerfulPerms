package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class GroupDeleteCommand extends SubCommand {

    public GroupDeleteCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> delete");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
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
                    Response response = permissionManager.deleteGroupBase(groupId);
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
        if ("delete".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("delete");
            return output;
        }
        return null;
    }
}
