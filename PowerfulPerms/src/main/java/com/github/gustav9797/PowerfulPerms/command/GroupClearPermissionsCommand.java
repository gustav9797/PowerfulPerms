package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.gustav9797.PowerfulPermsAPI.Response;

public class GroupClearPermissionsCommand extends SubCommand {

    public GroupClearPermissionsCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> clearperms");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.clearperms")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("clearperms")) {

                final String groupName = args[0];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }
                int groupId = group.getId();

                Response response = permissionManager.removeGroupPermissionsBase(groupId);
                sendSender(invoker, sender, response.getResponse());
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("clearperms".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("clearperms");
            return output;
        }
        return null;
    }
}
