package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.cheesesoftware.PowerfulPerms.common.ChatColor;
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
                Map<String, List<Group>> ladderGroups = new HashMap<String, List<Group>>();
                for (Group group : groups.values()) {
                    List<Group> temp = ladderGroups.get(group.getLadder());
                    if (temp == null)
                        temp = new ArrayList<Group>();
                    temp.add(group);
                    ladderGroups.put(group.getLadder(), temp);
                }

                String s = "";
                for (Entry<String, List<Group>> e : ladderGroups.entrySet()) {
                    s += "Ladder \"" + ChatColor.GREEN + e.getKey() + ChatColor.WHITE + "\":";
                    for (Group group : e.getValue()) {
                        s += group.getName() + ", ";
                    }
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
