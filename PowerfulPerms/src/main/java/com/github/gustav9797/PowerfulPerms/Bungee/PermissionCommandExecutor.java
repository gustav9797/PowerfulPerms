package com.github.gustav9797.PowerfulPerms.Bungee;

import com.github.gustav9797.PowerfulPerms.command.BaseCommand;
import java.util.List;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PermissionCommandExecutor extends Command implements ICommand {

    private BaseCommand cmd;

    public PermissionCommandExecutor(PermissionManager permissionManager) {
        super("powerfulperms", null, "powerfulpermsbungee", "ppbungee", "popbungee", "powbungee");
        cmd = new BaseCommand(PowerfulPerms.getPlugin(), permissionManager);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        cmd.execute(this, sender.getName(), args);
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (sender.hasPermission("powerfulperms.admin") || sender.hasPermission("powerfulperms.tabcomplete")) {
            args = BaseCommand.resolveArgs(args);
            return this.cmd.tabComplete(this, sender.getName(), args);
        }
        return null;
    }

    @Override
    public boolean hasPermission(String name, String permission) {
        if (name.equalsIgnoreCase("console") && ProxyServer.getInstance().getPlayer("console") == null)
            return true;
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        if (player != null && player.hasPermission(permission))
            return true;
        return false;
    }

}
