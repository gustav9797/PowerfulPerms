package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.Map;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class ShowGroupsCommand extends SubCommand {

    public ShowGroupsCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp groups");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.groups")) {
            if (args != null && args.length == 1 && args[0].equalsIgnoreCase("groups")) {
                Map<Integer, Group> groups = permissionManager.getGroups();
                String s = "";
                for (Group group : groups.values()) {
                    s += group.getName() + ", ";
                }
                if (s.length() > 0 && groups.size() > 0) {
                    s = s.substring(0, s.length() - 2);
                }
                sendSender(invoker, sender, "Groups: " + s);
                return CommandResult.success;
            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

}
