package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ChatColor;
import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionContainer;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class GroupHasPermissionCommand extends SubCommand {

    public GroupHasPermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group> haspermission <permission> (server) (world)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group.haspermission")
                || (args != null && args.length >= 3 && hasPermission(invoker, sender, "powerfulperms.group.haspermission." + args[2]))) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("haspermission")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String groupName = args[0];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }

                final String permission = args[2];
                String server = "";
                if (args.length >= 4)
                    server = args[3];
                String world = "";
                if (args.length >= 5)
                    world = args[4];

                List<Permission> permissions = group.getPermissions();
                List<String> realPermissions = new ArrayList<String>();
                for (Permission p : permissions) {
                    if (PermissionContainer.permissionApplies(p, server, world))
                        realPermissions.add(p.getPermissionString());
                }
                PermissionContainer permissionContainer = new PermissionContainer(permissions);
                permissionContainer.setRealPermissions(realPermissions);

                Boolean has = permissionContainer.hasPermission(permission);
                if (has != null) {
                    if (has)
                        sendSender(invoker, sender, "The group has the permission \"" + permission + "\".");
                    else
                        sendSender(invoker, sender, "The group" + ChatColor.RED + " does not " + ChatColor.WHITE + "have the permission \"" + permission + "\".");
                } else
                    sendSender(invoker, sender, "The permission \"" + permission + "\" is not set.");
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("haspermission".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("haspermission");
            return output;
        }
        return null;
    }
}
