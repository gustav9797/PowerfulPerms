package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class UserBaseCommand extends SubCommand {

    public UserBaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new UserCommand(plugin, permissionManager));
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, null)) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("user")) {

                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);

                boolean hasSomePermission = false;
                for (SubCommand subCommand : subCommands) {
                    CommandResult result = subCommand.execute(invoker, sender, newArgs);
                    if (result == CommandResult.success) {
                        return CommandResult.success;
                    } else if (result == CommandResult.showUsage) {
                        sendSender(invoker, sender, subCommand.getUsage());
                        return CommandResult.success;
                    } else if (result == CommandResult.noMatch) {
                        hasSomePermission = true;

                    }
                }

                if (hasSomePermission)
                    return CommandResult.showUsage;
                else
                    return CommandResult.noPermission;
            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

    @Override
    public List<String> getUsage() {
        List<String> usage = new ArrayList<String>();
        for (SubCommand subCommand : subCommands)
            usage.addAll(subCommand.getUsage());
        usage.add("This is from UserBaseCommand");
        return usage;
    }

}
