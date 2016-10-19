package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.github.cheesesoftware.PowerfulPerms.command.BaseCommand;
import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;

public class PermissionCommandExecutor implements ICommand, CommandExecutor, TabCompleter {

    private BaseCommand cmd;

    protected PermissionCommandExecutor(PermissionManager permissionManager) {
        cmd = new BaseCommand(PowerfulPerms.getPlugin(), permissionManager);
    }

    public BaseCommand getBaseCommand() {
        return this.cmd;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        cmd.execute(this, sender.getName(), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("powerfulperms.admin") || sender.hasPermission("powerfulperms.tabcomplete")) {
            args = BaseCommand.resolveArgs(args);
            Iterable<String> output = this.cmd.tabComplete(this, sender.getName(), args);
            if (output != null)
                return (List<String>) output;
        }
        return null;
    }

    @Override
    public boolean hasPermission(String name, String permission) {
        if (name.equalsIgnoreCase("console") || name.equalsIgnoreCase("rcon"))
            return true;
        Player player = Bukkit.getPlayerExact(name);
        if (player != null && player.hasPermission(permission))
            return true;
        return false;
    }

}
