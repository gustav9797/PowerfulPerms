package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class ReloadCommand extends SubCommand {

    public ReloadCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp reload  |  /pp globalreload");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.reload")) {
            if (args != null && args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    permissionManager.reloadGroups();
                    sendSender(invoker, sender, "Groups and players have been reloaded.");
                    return CommandResult.success;
                } else if (args[0].equalsIgnoreCase("globalreload")) {
                    permissionManager.reloadGroups();
                    permissionManager.notifyReloadGroups();
                    sendSender(invoker, sender, "Groups and players have been reloaded globally.");
                    return CommandResult.success;
                }
            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

}
