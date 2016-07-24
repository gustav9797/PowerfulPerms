package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.Date;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserRemoveGroupCommand extends SubCommand {

    public UserRemoveGroupCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> removegroup <group> (server) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.removegroup")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("removegroup")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String groupName = args[2];
                final Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    sendSender(invoker, sender, "Group does not exist.");
                    return CommandResult.success;
                }
                final int groupId = group.getId();

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
                            String server = "";
                            Date expires = null;
                            if (args.length >= 4)
                                server = args[3];
                            if (args.length >= 5) {
                                expires = Utils.getDate(args[4]);
                                if (expires == null) {
                                    sendSender(invoker, sender, "Invalid expiration format.");
                                    return;
                                }
                            }
                            String group = groupName;
                            boolean negated = group.startsWith("-");
                            if (negated)
                                group = group.substring(1);
                            permissionManager.removePlayerGroup(uuid, groupId, server, negated, expires, response);
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
