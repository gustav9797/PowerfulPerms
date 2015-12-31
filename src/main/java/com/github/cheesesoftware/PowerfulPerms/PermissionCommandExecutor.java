package com.github.cheesesoftware.PowerfulPerms;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionCommand;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;

public class PermissionCommandExecutor implements ICommand, CommandExecutor {
    
    private PermissionManagerBase permissionManager;

    protected PermissionCommandExecutor(PermissionManagerBase permissionManager) {
        this.permissionManager = permissionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PermissionCommand cmd = new PermissionCommand(permissionManager);
        return cmd.onCommand(this, sender.getName(), args);
    }
    
    @Override
    public void sendSender(String sender, String reply) {
        CommandSender commandSender = null;
        if(sender.equalsIgnoreCase("console"))
            commandSender = Bukkit.getConsoleSender();
        else
            commandSender = Bukkit.getPlayer(sender);
        
        if(commandSender != null) {
            commandSender.sendMessage(reply);
        }
    }

    @Override
    public String getVersion() {
        return PowerfulPerms.getPlugin().getDescription().getVersion();
    }

}
