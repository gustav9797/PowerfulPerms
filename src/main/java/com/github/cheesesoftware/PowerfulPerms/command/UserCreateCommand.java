package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserCreateCommand extends SubCommand {

    public UserCreateCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> create");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.create")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("create")) {

                final String playerName = args[0];

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
                            permissionManager.createPlayer(playerName, uuid, response);
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
