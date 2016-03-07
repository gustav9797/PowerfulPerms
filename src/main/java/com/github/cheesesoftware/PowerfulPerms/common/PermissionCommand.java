package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Pair;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionPlayer;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class PermissionCommand {

    private PermissionManager permissionManager;

    public PermissionCommand(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public boolean onCommand(final ICommand invoker, final String sender, final String[] args) {
        final ResponseRunnable response = new ResponseRunnable() {
            @Override
            public void run() {
                sendSender(invoker, sender, response);
            }
        };

        if (args.length >= 1 && args[0].equalsIgnoreCase("user") && args.length >= 2) {
            final String playerName = args[1];
            permissionManager.getConvertUUID(playerName, new ResultRunnable<UUID>() {

                @Override
                public void run() {
                    final UUID uuid = result;
                    if (uuid == null) {
                        response.setResponse(false, "Could not find player UUID.");
                        permissionManager.getScheduler().runSync(response, response.isSameThread());
                    } else {

                        int page = -1;
                        if (args.length >= 3) {
                            {
                                try {
                                    page = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    showCommandInfo(invoker, sender);
                                }
                            }
                        }
                        if (page != -1 || args.length == 2) {
                            if (page == -1)
                                page = 1;
                            page--;
                            if (page < 0)
                                sendSender(invoker, sender, "Invalid page. Page negative.");

                        }
                    }

                }
            });

            // /////////////////////////////////////////////////////////////////////////////////////////////////
            // /////////////////////////GROUP COMMAND BEGIN/////////////////////////////////////////
            // ///////////////////////////////////////////////////////////////////////
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("group") && args.length >= 2) {
            String groupName = args[1];
            int page = -1;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("prefix")) {
                    
                } else if (args[2].equalsIgnoreCase("suffix")) {
                    
                } else if (args[2].equalsIgnoreCase("parents")) {
                    
                } else if (args[2].equalsIgnoreCase("setladder") && args.length >= 4) {
                    
                } else if (args[2].equalsIgnoreCase("setrank") && args.length >= 4) {

                } else {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        showCommandInfo(invoker, sender);
                    }
                }
            }

        } else if (args.length >= 2 && args[0].equalsIgnoreCase("haspermission")) {
            
        } else
            showCommandInfo(invoker, sender);
        return true;
    }

    private void sendSender(ICommand command, String sender, String message) {
        command.sendSender(sender, PermissionManagerBase.pluginPrefixShort + message);
    }

    private void showCommandInfo(ICommand command, String sender) {
        String helpPrefix = "§b ";
        command.sendSender(sender, ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
        command.sendSender(sender, helpPrefix + "/pp user <username>");
        command.sendSender(sender, helpPrefix + "/pp user <username> promote/demote <ladder>");
        command.sendSender(sender, helpPrefix + "/pp user <username> addgroup/removegroup <group> (server)");
        command.sendSender(sender, helpPrefix + "/pp user <username> setrank <group>");
        command.sendSender(sender, helpPrefix + "/pp user <username> add/remove <permission> (server) (world)");
        command.sendSender(sender, helpPrefix + "/pp user <username> clearperms");
        command.sendSender(sender, helpPrefix + "/pp user <username> prefix/suffix set/remove <prefix/suffix>");
        command.sendSender(sender, helpPrefix + "/pp groups");
        command.sendSender(sender, helpPrefix + "/pp group <group>");
        command.sendSender(sender, helpPrefix + "/pp group <group> create (ladder) (rank)");
        command.sendSender(sender, helpPrefix + "/pp group <group> delete/clearperms");
        command.sendSender(sender, helpPrefix + "/pp group <group> add/remove <permission> (server) (world)");
        command.sendSender(sender, helpPrefix + "/pp group <group> parents add/remove <parent>");
        command.sendSender(sender, helpPrefix + "/pp group <group> prefix/suffix set/remove <prefix/suffix> (server)");
        command.sendSender(sender, helpPrefix + "/pp group <group> setladder/setrank <ladder/rank>");
        command.sendSender(sender, helpPrefix + "/pp haspermission <permission>");
        command.sendSender(sender, helpPrefix + "/pp reload  |  /pp globalreload");
        command.sendSender(sender, helpPrefix + "PowerfulPerms version " + "asfasofhjasfhj" + " by gustav9797");
    }

}
