package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class TestBaseCommand extends SubCommand {

    public TestBaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new TestRedisCommand(plugin, permissionManager));
        subCommands.add(new TestDatabaseCommand(plugin, permissionManager));
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("test")) {

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
