package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PermissionPlayer;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class HasPermissionCommand extends SubCommand {

    public HasPermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp haspermission <permission>");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.haspermission")) {
            if (args != null && args.length >= 1 && args[0].equalsIgnoreCase("haspermission")) {
                if (args.length < 2) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                String permission = args[1];
                PermissionPlayer p = permissionManager.getPermissionPlayer(sender);
                if (p != null) {
                    Boolean has = p.hasPermission(permission);
                    if (has != null) {
                        if (has)
                            sendSender(invoker, sender, "You have the permission \"" + permission + "\".");
                        else
                            sendSender(invoker, sender, "You do not have the permission \"" + permission + "\".");
                    } else
                        sendSender(invoker, sender, "The permission \"" + permission + "\" is not set.");
                } else
                    sendSender(invoker, sender, "Could not check permission. Make sure you are in-game.");
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("haspermission".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("haspermission");
            return output;
        }
        return null;
    }

}
