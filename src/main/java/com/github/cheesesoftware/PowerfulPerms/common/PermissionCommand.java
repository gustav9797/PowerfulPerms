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
                            if (args[2].equalsIgnoreCase("create")) {
                                permissionManager.createPlayer(playerName, uuid, response);
                            } else if (args[2].equalsIgnoreCase("clearperms")) {
                                permissionManager.removePlayerPermissions(uuid, response);
                            } else if (args[2].equalsIgnoreCase("promote") && args.length >= 4) {
                                permissionManager.promotePlayer(uuid, args[3], response);
                            } else if (args[2].equalsIgnoreCase("demote") && args.length >= 4) {
                                permissionManager.demotePlayer(uuid, args[3], response);
                            } else if (args[2].equalsIgnoreCase("setrank") && args.length >= 4) {
                                String group = args[3];
                                permissionManager.setPlayerRank(uuid, group, response);

                            } else if (args[2].equalsIgnoreCase("addgroup") && args.length >= 4) {
                                String group = args[3];
                                String server = "";
                                if (args.length >= 5)
                                    server = args[4];
                                boolean negated = group.startsWith("-");
                                if (negated)
                                    group = group.substring(1);
                                permissionManager.addPlayerGroup(uuid, group, server, negated, response);
                            } else if (args[2].equalsIgnoreCase("removegroup") && args.length >= 4) {
                                String group = args[3];
                                String server = "";
                                if (args.length >= 5)
                                    server = args[4];
                                boolean negated = group.startsWith("-");
                                if (negated)
                                    group = group.substring(1);
                                permissionManager.removePlayerGroup(uuid, group, server, negated, response);
                            } else if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                                String permission = args[3];
                                String world = "";
                                String server = "";
                                if (args.length >= 5)
                                    server = args[4];
                                if (args.length >= 6)
                                    world = args[5];
                                if (server.equalsIgnoreCase("all"))
                                    server = "";
                                if (world.equalsIgnoreCase("all"))
                                    world = "";
                                permissionManager.addPlayerPermission(uuid, playerName, permission, world, server, response);
                            } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                                String permission = args[3];
                                String world = "";
                                String server = "";
                                if (args.length >= 5)
                                    server = args[4];
                                if (args.length >= 6)
                                    world = args[5];
                                if (server.equalsIgnoreCase("all"))
                                    server = "";
                                if (world.equalsIgnoreCase("all"))
                                    world = "";
                                permissionManager.removePlayerPermission(uuid, permission, world, server, response);
                            } else if (args[2].equalsIgnoreCase("prefix")) {
                                if (args.length >= 5 && args[3].equalsIgnoreCase("set")) {
                                    String prefix = "";
                                    if (args[4].length() >= 1 && args[4].toCharArray()[0] == '"') {
                                        // Input is between quote marks.
                                        String result = "";
                                        result += args[4].substring(1) + " ";

                                        if (args.length >= 6) {
                                            for (int i = 5; i < args.length; i++) {
                                                result += args[i] + " ";
                                            }
                                        }

                                        if (result.toCharArray()[result.length() - 1] == ' ')
                                            result = result.substring(0, result.length() - 1);
                                        if (result.toCharArray()[result.length() - 1] == '"')
                                            result = result.substring(0, result.length() - 1);

                                        prefix = result;
                                    } else
                                        prefix = args[4];

                                    permissionManager.setPlayerPrefix(uuid, prefix, response);

                                } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                                    permissionManager.setPlayerPrefix(uuid, "", response);
                                } else
                                    permissionManager.getPlayerOwnPrefix(uuid, new ResultRunnable<String>() {

                                        @Override
                                        public void run() {
                                            sendSender(invoker, sender, "Prefix for player(non-inherited) " + playerName + ": \"" + (result != null ? result : "") + "\"");
                                        }
                                    });

                            } else if (args[2].equalsIgnoreCase("suffix")) {
                                if (args.length >= 5 && args[3].equalsIgnoreCase("set")) {
                                    String suffix = "";
                                    if (args[4].length() >= 1 && args[4].toCharArray()[0] == '"') {
                                        // Input is between quote marks.
                                        String result = "";
                                        result += args[4].substring(1) + " ";

                                        if (args.length >= 6) {
                                            for (int i = 5; i < args.length; i++) {
                                                result += args[i] + " ";
                                            }
                                        }

                                        if (result.toCharArray()[result.length() - 1] == ' ')
                                            result = result.substring(0, result.length() - 1);
                                        if (result.toCharArray()[result.length() - 1] == '"')
                                            result = result.substring(0, result.length() - 1);

                                        suffix = result;
                                    } else
                                        suffix = args[4];

                                    permissionManager.setPlayerSuffix(uuid, suffix, response);

                                } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                                    permissionManager.setPlayerSuffix(uuid, "", response);
                                } else
                                    permissionManager.getPlayerOwnSuffix(uuid, new ResultRunnable<String>() {

                                        @Override
                                        public void run() {
                                            sendSender(invoker, sender, "Suffix for player(non-inherited) " + playerName + ": \"" + (result != null ? result : "") + "\"");
                                        }
                                    });
                            } else {
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
                if (args[2].equalsIgnoreCase("clearperms")) {
                    permissionManager.removeGroupPermissions(groupName, response);
                } else if (args[2].equalsIgnoreCase("create")) {
                    String ladder = "default";
                    if (args.length >= 4)
                        ladder = args[3];
                    int rank = 100;
                    if (args.length >= 5) {
                        try {
                            rank = Integer.parseInt(args[4]);
                        } catch (NumberFormatException e) {
                            showCommandInfo(invoker, sender);
                            return true;
                        }
                    }
                    permissionManager.createGroup(groupName, ladder, rank, response);

                } else if (args[2].equalsIgnoreCase("delete")) {
                    permissionManager.deleteGroup(groupName, response);
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    if (args.length >= 6)
                        world = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    permissionManager.addGroupPermission(groupName, permission, world, server, response);
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    if (args.length >= 6)
                        world = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    permissionManager.removeGroupPermission(groupName, permission, world, server, response);
                } else if (args[2].equalsIgnoreCase("prefix")) {
                    String server = "";
                    if (args.length >= 5 && args[3].equalsIgnoreCase("set")) {
                        String prefix = "";
                        if (args.length >= 6 && args[4].toCharArray()[0] != '"')
                            server = args[5];
                        if (args[4].length() >= 1 && args[4].toCharArray()[0] == '"') {
                            // Input is between quote marks.
                            String result = "";
                            result += args[4].substring(1) + " ";

                            int lastArg = 4;
                            if (!result.endsWith("\" ")) {
                                if (args.length >= 6) {
                                    for (int i = 5; i < args.length; i++) {
                                        result += args[i] + " ";
                                        if (args[i].endsWith("\"")) {
                                            lastArg = i;
                                            break;
                                        }
                                    }
                                }
                            }

                            // If server is specified set server to argument after
                            if (args.length >= lastArg + 2) {
                                server = args[lastArg + 1];
                            }

                            // remove '" '
                            if (result.toCharArray()[result.length() - 1] == ' ')
                                result = result.substring(0, result.length() - 1);
                            if (result.toCharArray()[result.length() - 1] == '"')
                                result = result.substring(0, result.length() - 1);

                            prefix = result;
                        } else
                            prefix = args[4];

                        permissionManager.setGroupPrefix(groupName, prefix, server, response);
                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        permissionManager.setGroupPrefix(groupName, "", (args.length >= 5 ? args[4] : ""), response);
                    } else {
                        HashMap<String, String> prefix = permissionManager.getGroupServerPrefix(groupName);
                        if (prefix != null) {
                            String output = "";
                            Iterator<Entry<String, String>> it = prefix.entrySet().iterator();
                            while (it.hasNext()) {
                                Entry<String, String> entry = it.next();
                                output += ChatColor.WHITE + "\"" + entry.getValue() + "\":" + (entry.getKey().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : entry.getKey());
                                if (it.hasNext())
                                    output += ", ";
                            }
                            sendSender(invoker, sender, "Prefixes for group " + groupName + ": " + output);
                        } else
                            sendSender(invoker, sender, "Group does not exist.");
                    }
                } else if (args[2].equalsIgnoreCase("suffix")) {
                    String server = "";
                    if (args.length >= 5 && args[3].equalsIgnoreCase("set")) {
                        String suffix = "";
                        if (args.length >= 6 && args[4].toCharArray()[0] != '"')
                            server = args[5];
                        if (args[4].length() >= 1 && args[4].toCharArray()[0] == '"') {
                            // Input is between quote marks.
                            String result = "";
                            result += args[4].substring(1) + " ";

                            int lastArg = 4;
                            if (!result.endsWith("\" ")) {
                                if (args.length >= 6) {
                                    for (int i = 5; i < args.length; i++) {
                                        result += args[i] + " ";
                                        if (args[i].endsWith("\"")) {
                                            lastArg = i;
                                            break;
                                        }
                                    }
                                }
                            }

                            // If server is specified set server to argument after
                            if (args.length >= lastArg + 2) {
                                server = args[lastArg + 1];
                            }

                            if (result.toCharArray()[result.length() - 1] == ' ')
                                result = result.substring(0, result.length() - 1);
                            if (result.toCharArray()[result.length() - 1] == '"')
                                result = result.substring(0, result.length() - 1);

                            suffix = result;
                        } else
                            suffix = args[4];

                        permissionManager.setGroupSuffix(groupName, suffix, server, response);
                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        permissionManager.setGroupSuffix(groupName, "", (args.length >= 5 ? args[4] : ""), response);
                    } else {
                        HashMap<String, String> suffix = permissionManager.getGroupServerSuffix(groupName);
                        if (suffix != null) {
                            String output = "";
                            Iterator<Entry<String, String>> it = suffix.entrySet().iterator();
                            while (it.hasNext()) {
                                Entry<String, String> entry = it.next();
                                output += ChatColor.WHITE + "\"" + entry.getValue() + "\":" + (entry.getKey().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : entry.getKey());
                                if (it.hasNext())
                                    output += ", ";
                            }
                            sendSender(invoker, sender, "Suffixes for group " + groupName + ": " + output);
                        } else
                            sendSender(invoker, sender, "Group does not exist.");
                    }
                } else if (args[2].equalsIgnoreCase("parents")) {
                    if (args.length >= 5 && args[3].equalsIgnoreCase("add")) {
                        String parent = args[4];
                        permissionManager.addGroupParent(groupName, parent, response);
                    } else if (args.length >= 5 && args[3].equalsIgnoreCase("remove")) {
                        String parent = args[4];
                        permissionManager.removeGroupParent(groupName, parent, response);
                    } else {
                        // List parents
                        Group group = permissionManager.getGroup(groupName);
                        if (group != null) {
                            sendSender(invoker, sender, "Listing parents for group " + groupName + ":");

                            if (group.getParents() != null && group.getParents().size() > 0) {
                                for (Group g : group.getParents())
                                    sendSender(invoker, sender, g.getName());
                            } else
                                sendSender(invoker, sender, "Group has no parents.");
                        } else
                            sendSender(invoker, sender, "Group does not exist.");
                    }
                } else if (args[2].equalsIgnoreCase("setladder") && args.length >= 4) {
                    String ladder = args[3];
                    permissionManager.setGroupLadder(groupName, ladder, response);
                } else if (args[2].equalsIgnoreCase("setrank") && args.length >= 4) {
                    try {
                        int rank = Integer.parseInt(args[3]);
                        permissionManager.setGroupRank(groupName, rank, response);
                    } catch (NumberFormatException e) {
                        sendSender(invoker, sender, "Rank must be a number.");
                    }
                } else {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        showCommandInfo(invoker, sender);
                    }
                }
            }

        } else if (args.length >= 2 && args[0].equalsIgnoreCase("haspermission")) {
            String permission = args[1];
            PermissionPlayer p = permissionManager.getPermissionPlayer(sender);
            if (p != null) {
                Boolean has = p.hasPermission(permission);
                if (has != null) {
                    if (has)
                        sendSender(invoker, sender, "You have the permission \"" + permission + "\".");
                    else
                        sendSender(invoker, sender, "You do not have the permission \"" + permission + "\".");
                } else
                    sendSender(invoker, sender, "The permission \"" + permission + "\" is not set.");
            } else
                sendSender(invoker, sender, "Could not check permission. Make sure you are in-game.");
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
