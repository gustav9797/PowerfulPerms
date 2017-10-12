package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public abstract class SubCommand {
    protected List<SubCommand> subCommands = new ArrayList<>();
    protected PowerfulPermsPlugin plugin;
    protected PermissionManagerBase permissionManager;
    protected PermissionManager _permissionManager;
    protected List<String> usage = new ArrayList<>();

    public SubCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.permissionManager = (PermissionManagerBase) permissionManager;
        this._permissionManager = permissionManager;
    }

    public abstract CommandResult execute(final ICommand invoker, final String sender, final String[] args) throws InterruptedException, ExecutionException;

    public abstract List<String> tabComplete(final ICommand invoker, final String sender, final String[] args);

    public List<String> getUsage() {
        return usage;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    protected boolean hasBasicPerms(ICommand invoker, String sender, String permission) {
        if (invoker.hasPermission(sender, "powerfulperms.admin")) {
            return true;
        } else if (permission != null) {
            if (invoker.hasPermission(sender, permission))
                return true;
        }
        return false;
    }

    protected boolean hasPermission(ICommand invoker, String sender, String permission) {
        if (invoker.hasPermission(sender, permission))
            return true;
        return false;
    }

    protected void sendSender(ICommand command, String sender, String message) {
        plugin.sendPlayerMessage(sender, message);
    }

    protected void sendSender(ICommand command, String sender, List<String> message) {
        for (String m : message)
            plugin.sendPlayerMessage(sender, m);
    }
}
