package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

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
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("group")) {

            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, args.length - 1);

            boolean hasSomePermission = false;
            for (SubCommand subCommand : subCommands) {
                CommandResult result = subCommand.execute(invoker, sender, newArgs);
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
    public List<String> getUsage() {
        List<String> usage = new ArrayList<String>();
        for (SubCommand subCommand : subCommands)
            usage.addAll(subCommand.getUsage());
        return usage;
    }

}
