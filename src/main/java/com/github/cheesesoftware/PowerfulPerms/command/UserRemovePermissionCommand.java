package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.Date;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserRemovePermissionCommand extends SubCommand {

    public UserRemovePermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> remove <permission> (server) (world)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.remove")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String permission = args[2];

                final ResponseRunnable response = new ResponseRunnable() {
                    @Override
                    public void run() {
                        sendSender(invoker, sender, response);
                    }
                };

                permissionManager.getConvertUUID(playerName, new ResultRunnable<UUID>() {

                    @Override
                    public void run() {
                        final UUID uuid = result;
                        if (uuid == null) {
                            response.setResponse(false, "Could not find player UUID.");
                            permissionManager.getScheduler().runSync(response, response.isSameThread());
                        } else {
                            String world = "";
                            String server = "";
                            Date expires = null;
                            if (args.length >= 4)
                                server = args[3];
                            if (args.length >= 5)
                                world = args[4];
                            if (args.length >= 6) {
                                expires = Utils.getDate(args[5]);
                                if (expires == null) {
                                    sendSender(invoker, sender, "Invalid expiration format.");
                                    return;
                                }
                            }
                            if (server.equalsIgnoreCase("all"))
                                server = "";
                            if (world.equalsIgnoreCase("all"))
                                world = "";
                            permissionManager.removePlayerPermission(uuid, permission, world, server, expires, response);
                        }
                    }
                });
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }
}
