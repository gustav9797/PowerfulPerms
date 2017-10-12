package com.github.gustav9797.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class AboutCommand extends SubCommand {

    public AboutCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp about");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.about")) {
            if (args != null && args.length >= 1 && args[0].equalsIgnoreCase("about")) {
                sendSender(invoker, sender, "PowerfulPerms version " + plugin.getVersion() + " by gustav9797");
                String spigotUser = "%%__USER__%%";
                String uniqueDownloadID = "%%__NONCE__%%";
                String copy = "https://www.SpigotMC.org/members/" + spigotUser;
                sendSender(invoker, sender, "This copy of the plugin is bound to " + copy + " and the Unique ID for this file is \"" + uniqueDownloadID + "\"");
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("about".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<>();
            output.add("about");
            return output;
        }
        return null;
    }

}
