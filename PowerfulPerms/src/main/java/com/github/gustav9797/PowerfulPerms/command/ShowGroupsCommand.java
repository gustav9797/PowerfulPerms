package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

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
                Map<String, List<Group>> ladderGroups = new HashMap<>();
                for (Group group : groups.values()) {
                    List<Group> temp = ladderGroups.get(group.getLadder());
                    if (temp == null)
                        temp = new ArrayList<>();
                    temp.add(group);
                    ladderGroups.put(group.getLadder(), temp);
                }

                sendSender(invoker, sender, ChatColor.BLUE + "Listing groups.");
                for (Entry<String, List<Group>> e : ladderGroups.entrySet()) {
                    String out = "On ladder \"" + ChatColor.GREEN + e.getKey() + ChatColor.WHITE + "\": ";
                    for (Group group : e.getValue()) {
                        out += group.getName() + ", ";
                    }
                    if (out.length() > 0 && ladderGroups.entrySet().size() > 0)
                        out = out.substring(0, out.length() - 2);
                    sendSender(invoker, sender, out);
                }

                return CommandResult.success;
            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if (args.length == 1 && "groups".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("groups");
            return output;
        }
        return null;
    }

}
