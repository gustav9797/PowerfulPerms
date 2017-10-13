package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class TestDatabaseCommand extends SubCommand {

    public TestDatabaseCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp test database");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.test.database")) {
            if (args != null && args.length >= 1 && args[0].equalsIgnoreCase("database")) {
                final PermissionManagerBase base = (PermissionManagerBase) permissionManager;
                plugin.runTaskAsynchronously(() -> {
                    boolean success = base.getDatabase().ping();
                    if (success)
                        sendSender(invoker, sender, "Your database connection is fine.");
                    else
                        sendSender(invoker, sender, "Could not open a connection to your database. Check the console for any exceptions.");
                });
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("database".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("database");
            return output;
        }
        return null;
    }

}
