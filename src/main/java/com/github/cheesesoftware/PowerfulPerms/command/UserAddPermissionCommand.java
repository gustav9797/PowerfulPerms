package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserAddPermissionCommand extends SubCommand {

    public UserAddPermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> add <permission> (server) (world)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.add")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("add")) {
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
                            if (args.length >= 4)
                                server = args[3];
                            if (args.length >= 5)
                                world = args[4];
                            if (server.equalsIgnoreCase("all"))
                                server = "";
                            if (world.equalsIgnoreCase("all"))
                                world = "";
                            parsePermission(permissionManager, uuid, playerName, permission, world, server, response);
                        }
                    }
                });
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    private static void parsePermission(PermissionManager permissionManager, UUID uuid, String playerName, String permission, String world, String server, ResponseRunnable response) {
        int beginIndex = -1;
        int endIndex = -1;

        char[] chars = permission.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            if (beginIndex == -1 && chars[i] == '{')
                beginIndex = i;
            if (endIndex == -1 && beginIndex != -1 && chars[i] == '}') {
                endIndex = i;
                // Found sequence
                String sequence = permission.substring(beginIndex + 1, endIndex);
                String[] sequenceList = sequence.split(",");
                for (String s : sequenceList) {
                    StringBuilder builder = new StringBuilder(permission);
                    builder.replace(beginIndex, endIndex + 1, s);
                    parsePermission(permissionManager, uuid, playerName, builder.toString(), world, server, response);
                }
            }

        }

        if (beginIndex == -1 && endIndex == -1) {
            // Didn't find any more sequence
            permissionManager.addPlayerPermission(uuid, playerName, permission, world, server, response);
        }
    }
}
