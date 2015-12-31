package com.github.cheesesoftware.PowerfulPerms.Bungee;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionCommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class PermissionCommandExecutor extends Command implements ICommand {
    
    private PermissionManagerBase permissionManager;
    
    public PermissionCommandExecutor(PermissionManagerBase permissionManager) {
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
        if(sender.equalsIgnoreCase("console"))
            commandSender = ProxyServer.getInstance().getConsole();
        else
            commandSender = ProxyServer.getInstance().getPlayer(sender);
        
        if(commandSender != null) {
            TextComponent message = new TextComponent(reply);
            commandSender.sendMessage(message);
        }
    }

    @Override
    public String getVersion() {
        return com.github.cheesesoftware.PowerfulPerms.Bungee.PowerfulPerms.getPlugin().getDescription().getVersion();
    }

}
