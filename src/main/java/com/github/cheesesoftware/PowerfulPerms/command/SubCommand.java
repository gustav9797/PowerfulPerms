package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

public abstract class SubCommand {
    protected List<SubCommand> subCommands = new ArrayList<SubCommand>();
    protected PowerfulPermsPlugin plugin;
    protected PermissionManager permissionManager;
    protected List<String> usage = new ArrayList<String>();

    public SubCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.permissionManager = permissionManager;
    }

    public abstract CommandResult execute(final ICommand invoker, final String sender, final String[] args);

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

    protected void sendSender(ICommand command, String sender, String message) {
        command.sendSender(sender, PermissionManagerBase.pluginPrefixShort + message);
    }

    protected void sendSender(ICommand command, String sender, List<String> message) {
        for (String m : message)
            command.sendSender(sender, PermissionManagerBase.pluginPrefixShort + m);
    }
}
