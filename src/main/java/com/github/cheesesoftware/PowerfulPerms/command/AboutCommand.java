package com.github.cheesesoftware.PowerfulPerms.command;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class AboutCommand extends SubCommand {

    public AboutCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp about");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.about")) {
            if (args != null && args.length >= 1) {
                sendSender(invoker, sender, "PowerfulPerms version " + plugin.getVersion() + " by gustav9797");
                String spigotUser = "%%__USER__%%";
                String uniqueDownloadID = "%%__NONCE__%%";
                String copy = "https://www.SpigotMC.org/members/" + spigotUser;
                sendSender(invoker, sender, "This copy of the plugin is bound to " + copy + " and the Unique ID for this file is " + uniqueDownloadID);
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

}
