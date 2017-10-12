package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class UserBaseCommand extends SubCommand {

    public UserBaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new UserCommand(plugin, permissionManager));
        subCommands.add(new UserPromoteCommand(plugin, permissionManager));
        subCommands.add(new UserDemoteCommand(plugin, permissionManager));
        subCommands.add(new UserSetRankCommand(plugin, permissionManager));
        subCommands.add(new UserAddGroupCommand(plugin, permissionManager));
        subCommands.add(new UserRemoveGroupCommand(plugin, permissionManager));
        subCommands.add(new UserAddPermissionCommand(plugin, permissionManager));
        subCommands.add(new UserRemovePermissionCommand(plugin, permissionManager));
        subCommands.add(new UserClearPermissionsCommand(plugin, permissionManager));
        subCommands.add(new UserDeleteCommand(plugin, permissionManager));
        subCommands.add(new UserCreateCommand(plugin, permissionManager));
        subCommands.add(new UserPrefixCommand(plugin, permissionManager));
        subCommands.add(new UserSuffixCommand(plugin, permissionManager));
        subCommands.add(new UserHasPermissionCommand(plugin, permissionManager));
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) throws InterruptedException, ExecutionException {
        if (args.length >= 1 && args[0].equalsIgnoreCase("user")) {

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
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        List<String> output = new ArrayList<>();
        if (args.length == 1 && "user".startsWith(args[0].toLowerCase())) {
            // Tabcomplete "user"
            output.add("user");
            return output;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("user")) {
            // Tabcomplete "user <name>"
            for (String name : plugin.getOnlinePlayers().values()) {
                if (name.toLowerCase().startsWith(args[1]))
                    output.add(name);
            }
            return output;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("user")) {
            // Tabcomplete "user <name> <something>"
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
