package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class BaseCommand extends SubCommand {

    public BaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);

        subCommands.add(new UserBaseCommand(plugin, permissionManager));
        subCommands.add(new ShowGroupsCommand(plugin, permissionManager));
        subCommands.add(new GroupBaseCommand(plugin, permissionManager));
        subCommands.add(new ReloadCommand(plugin, permissionManager));

        // usage = new ArrayList<String>();
        // String helpPrefix = "§b ";
        // TODO: Print command usage from commands.
        /*-usage.add(ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
        usage.add("/pp user <username>");
        usage.add("/pp user <username> promote/demote <ladder>");
        usage.add("/pp user <username> addgroup/removegroup <group> (server)");
        usage.add("/pp user <username> setrank <group>");
        usage.add("/pp user <username> add/remove <permission> (server) (world)");
        usage.add("/pp user <username> clearperms");
        usage.add("/pp user <username> prefix/suffix set/remove <prefix/suffix>");
        usage.add("/pp groups");
        usage.add("/pp group <group>");
        usage.add("/pp group <group> create (ladder) (rank)");
        usage.add("/pp group <group> delete/clearperms");
        usage.add("/pp group <group> add/remove <permission> (server) (world)");
        usage.add("/pp group <group> parents add/remove <parent>");
        usage.add("/pp group <group> prefix/suffix set/remove <prefix/suffix> (server)");
        usage.add("/pp group <group> setladder/setrank <ladder/rank>");
        usage.add("/pp haspermission <permission>");
        usage.add("/pp reload  |  /pp globalreload");
        usage.add("PowerfulPerms version " + plugin.getVersion() + " by gustav9797");*/
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        boolean hasSomePermission = false;
        for (SubCommand subCommand : subCommands) {
            CommandResult result = subCommand.execute(invoker, sender, args);
            if (result == CommandResult.success) {
                return CommandResult.success;
            } else if (result == CommandResult.showUsage) {
                sendSender(invoker, sender, subCommand.getUsage());
                return CommandResult.success;
            } else if (result == CommandResult.noMatch) {
                hasSomePermission = true;

            }
        }

        if (hasSomePermission) {
            sendSender(invoker, sender, getUsage());
            return CommandResult.success;
        } else {
            sendSender(invoker, sender, "You do not have permission.");
            return CommandResult.noPermission;
        }
    }

    @Override
    public List<String> getUsage() {
        List<String> usage = new ArrayList<String>();
        for (SubCommand subCommand : subCommands)
            usage.addAll(subCommand.getUsage());
        usage.add("This is usage from BaseCommand");
        return usage;
    }

}
