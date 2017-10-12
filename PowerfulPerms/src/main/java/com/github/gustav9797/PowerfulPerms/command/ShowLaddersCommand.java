package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class ShowLaddersCommand extends SubCommand {

    public ShowLaddersCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp ladders");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.ladders")) {
            if (args != null && args.length == 1 && args[0].equalsIgnoreCase("ladders")) {

                List<String> ladders = new ArrayList<>();
                Map<Integer, Group> groups = permissionManager.getGroups();
                String s = "";
                for (Group group : groups.values()) {
                    if (!ladders.contains(group.getLadder())) {
                        s += group.getLadder() + ", ";
                        ladders.add(group.getLadder());
                    }
                }
                if (s.length() > 0 && ladders.size() > 0) {
                    s = s.substring(0, s.length() - 2);
                }
                sendSender(invoker, sender, "Ladders: " + s);
                return CommandResult.success;
            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("ladders".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("ladders");
            return output;
        }
        return null;
    }

}
