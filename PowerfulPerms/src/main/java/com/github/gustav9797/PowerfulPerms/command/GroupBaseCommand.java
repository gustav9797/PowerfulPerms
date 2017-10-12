package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class GroupBaseCommand extends SubCommand {

    public GroupBaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new GroupCommand(plugin, permissionManager));
        subCommands.add(new GroupCreateCommand(plugin, permissionManager));
        subCommands.add(new GroupDeleteCommand(plugin, permissionManager));
        subCommands.add(new GroupClearPermissionsCommand(plugin, permissionManager));
        subCommands.add(new GroupAddPermissionCommand(plugin, permissionManager));
        subCommands.add(new GroupRemovePermissionCommand(plugin, permissionManager));
        subCommands.add(new GroupParentsCommand(plugin, permissionManager));
        subCommands.add(new GroupPrefixCommand(plugin, permissionManager));
        subCommands.add(new GroupSuffixCommand(plugin, permissionManager));
        subCommands.add(new GroupSetLadderCommand(plugin, permissionManager));
        subCommands.add(new GroupSetRankCommand(plugin, permissionManager));
        subCommands.add(new GroupRenameCommand(plugin, permissionManager));
        subCommands.add(new GroupHasPermissionCommand(plugin, permissionManager));
        subCommands.add(new GroupMembersCommand(plugin, permissionManager));
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) throws InterruptedException, ExecutionException {
        if (args.length >= 1 && args[0].equalsIgnoreCase("group")) {

            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);

            boolean hasSomePermission = false;
            for (SubCommand subCommand : subCommands) {
                CommandResult result;
                result = subCommand.execute(invoker, sender, newArgs);
                if (result == CommandResult.success) {
                    return CommandResult.success;
                } else if (result == CommandResult.noMatch) {
                    hasSomePermission = true;
                }
            }

            if (hasSomePermission) {
                sendSender(invoker, sender, getUsage());
                return CommandResult.success;
            } else
                return CommandResult.noPermission;
        } else if (!hasPermission(invoker, sender, "powerfulperms.admin"))
            return CommandResult.noPermission;
        else
            return CommandResult.noMatch;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        List<String> output = new ArrayList<>();
        if (args.length == 1 && "group".startsWith(args[0].toLowerCase())) {
            // Tabcomplete "group"
            output.add("group");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("group")) {
            // Tabcomplete "group <name>"
            for (Group group : permissionManager.getGroups().values()) {
                if (group.getName().toLowerCase().startsWith(args[1]))
                    output.add(group.getName());
            }
            return output;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("group")) {
            // Tabcomplete "group <name> <something>"
            String[] newArgs = new String[args.length - 2];
            System.arraycopy(args, 2, newArgs, 0, args.length - 2);
            for (SubCommand subCommand : subCommands) {
                List<String> out = subCommand.tabComplete(invoker, sender, newArgs);
                if (out != null)
                    output.addAll(out);
            }
            return output;
        }
        return null;
    }

    @Override
    public List<String> getUsage() {
        List<String> usage = new ArrayList<>();
        for (SubCommand subCommand : subCommands)
            usage.addAll(subCommand.getUsage());
        return usage;
    }

}
