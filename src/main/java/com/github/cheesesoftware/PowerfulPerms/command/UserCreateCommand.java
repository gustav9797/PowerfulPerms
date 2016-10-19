package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;
import com.google.common.util.concurrent.ListenableFuture;

public class UserCreateCommand extends SubCommand {

    public UserCreateCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> create");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.create")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("create")) {

                final String playerName = args[0];

                ListenableFuture<UUID> first = permissionManager.getConvertUUID(playerName);
                final UUID uuid = first.get();
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
                } else {
                    final ListenableFuture<Response> second = permissionManager.createPlayer(playerName, uuid);
                    sendSender(invoker, sender, second.get().getResponse());
                }

                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("create".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("create");
            return output;
        }
        return null;
    }
}
