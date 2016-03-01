package com.github.cheesesoftware.PowerfulPerms.Bungee;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionCommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PermissionCommandExecutor extends Command implements ICommand {

    private PermissionManager permissionManager;

    public PermissionCommandExecutor(PermissionManager permissionManager) {
        super("powerfulperms", "powerfulperms.admin", "pp", "pop", "pow");
        this.permissionManager = permissionManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        PermissionCommand command = new PermissionCommand(permissionManager);
        command.onCommand(this, sender.getName(), args);
    }

    @Override
    public void sendSender(String sender, String reply) {
        CommandSender commandSender = null;
        if (sender.equalsIgnoreCase("console"))
            commandSender = ProxyServer.getInstance().getConsole();
        else
            commandSender = ProxyServer.getInstance().getPlayer(sender);

        if (commandSender != null) {
            TextComponent message = new TextComponent(reply);
            commandSender.sendMessage(message);
        }
    }

    @Override
    public String getVersion() {
        return com.github.cheesesoftware.PowerfulPerms.Bungee.PowerfulPerms.getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean hasPermission(String name, String permission) {
        if (name.equals("console"))
            return true;
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        if (player != null && (player.hasPermission("powerfulperms.admin") || player.hasPermission(permission)))
            return true;
        return false;
    }

}
