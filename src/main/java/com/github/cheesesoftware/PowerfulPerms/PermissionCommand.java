package com.github.cheesesoftware.PowerfulPerms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.ChatPaginator;

public class PermissionCommand implements CommandExecutor {

    private PermissionManagerBase permissionManager;

    public PermissionCommand(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("user") && args.length >= 2) {
            String playerName = args[1];
            int page = -1;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("clearperms")) {
                    PMR result = permissionManager.removePlayerPermissions(playerName);
                    sendSender(sender, result.getResponse());
                } else if (args[2].equalsIgnoreCase("setprimarygroup") && args.length >= 4) {
                    String group = args[3];
                    PMR result = permissionManager.setPlayerPrimaryGroup(playerName, group);
                    sendSender(sender, result.getResponse());
                } else if (args[2].equalsIgnoreCase("addgroup") && args.length >= 4) {
                    String group = args[3];
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    PMR result = permissionManager.addPlayerGroup(playerName, group, server);
                    sendSender(sender, result.getResponse());
                } else if (args[2].equalsIgnoreCase("removegroup") && args.length >= 4) {
                    String group = args[3];
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    PMR result = permissionManager.removePlayerGroup(playerName, group, server);
                    sendSender(sender, result.getResponse());
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        world = args[4];
                    if (args.length >= 6)
                        server = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    PMR result = permissionManager.addPlayerPermission(playerName, permission, world, server);
                    sendSender(sender, result.getResponse());
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        world = args[4];
                    if (args.length >= 6)
                        server = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    PMR result = permissionManager.removePlayerPermission(playerName, permission, world, server);
                    sendSender(sender, result.getResponse());
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

                        PMR result = permissionManager.setPlayerPrefix(playerName, prefix);
                        sendSender(sender, result.getResponse());

                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        PMR result = permissionManager.setPlayerPrefix(playerName, "");
                        sendSender(sender, result.getResponse());
                    } else
                        sendSender(sender, "Prefix for player(non-inherited) " + playerName + ": " + permissionManager.getPlayerPrefix(playerName));
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

                        PMR result = permissionManager.setPlayerSuffix(playerName, suffix);
                        sendSender(sender, result.getResponse());

                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        PMR result = permissionManager.setPlayerSuffix(playerName, "");
                        sendSender(sender, result.getResponse());
                    } else
                        sendSender(sender, "Suffix for player(non-inherited) " + playerName + ": " + permissionManager.getPlayerSuffix(playerName));
                } else {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        showCommandInfo(sender);
                    }
                }
            }
            if (page != -1 || args.length == 2) {
                if (page == -1)
                    page = 1;
                page--;
                if (page < 0)
                    sendSender(sender, "Invalid page. Page negative.");
                // List player permissions
                Queue<String> rows = new java.util.ArrayDeque<String>();
                rows.add(ChatColor.BLUE + "Listing permissions for player " + playerName + ".");
                ResultSet result = permissionManager.getPlayerData(playerName);
                String tempUUID = "empty";
                try {
                    if (result != null)
                        tempUUID = result.getString("uuid");
                    rows.add(ChatColor.GREEN + "UUID" + ChatColor.WHITE + ": " + tempUUID);
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                HashMap<String, List<Group>> groups = permissionManager.getPlayerGroups(playerName);
                Group primary = permissionManager.getPlayerPrimaryGroup(playerName);
                rows.add(ChatColor.GREEN + "Primary Group" + ChatColor.WHITE + ": " + (primary != null ? primary.getName() : "Player has no group."));

                String otherGroups = ChatColor.GREEN + "Groups" + ChatColor.WHITE + ": ";
                if (groups.size() > 0) {
                    Iterator<Entry<String, List<Group>>> it = groups.entrySet().iterator();
                    while (it.hasNext()) {
                        Entry<String, List<Group>> current = it.next();
                        Iterator<Group> itt = current.getValue().iterator();
                        while (itt.hasNext()) {
                            Group group = itt.next();
                            if (group != null) {
                                otherGroups += ChatColor.WHITE + group.getName() + ":" + ChatColor.RED + (current.getKey() == null || current.getKey().isEmpty() ? "ALL" : current.getKey());
                                if (it.hasNext() || itt.hasNext())
                                    otherGroups += ", ";
                            } else
                                Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "Group was null iterating groups in page");
                        }
                    }
                }
                rows.add(otherGroups);

                ArrayList<PowerfulPermission> playerPerms = permissionManager.getPlayerPermissions(playerName);
                if (playerPerms.size() > 0)
                    for (PowerfulPermission e : playerPerms) {
                        rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + " (Server:" + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer())
                                + " World:" + (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
                    }
                else
                    rows.add("Player has no permissions.");

                List<List<String>> list = createList(rows, 19);
                sendSender(sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());
                if (page < list.size()) {
                    for (String s : list.get(page))
                        sendSender(sender, s);
                } else
                    sendSender(sender, "Invalid page. Page too high. ");
            }

            // /////////////////////////////////////////////////////////////////////////////////////////////////
            // /////////////////////////GROUP COMMAND BEGIN/////////////////////////////////////////
            // ///////////////////////////////////////////////////////////////////////
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("group") && args.length >= 2) {
            String groupName = args[1];
            int page = -1;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("clearperms")) {
                    PMR result = permissionManager.removeGroupPermissions(groupName);
                    sendSender(sender, result.getResponse());
                } else if (args[2].equalsIgnoreCase("create")) {
                    PMR result = permissionManager.createGroup(groupName);
                    sendSender(sender, result.getResponse());
                } else if (args[2].equalsIgnoreCase("delete")) {
                    PMR result = permissionManager.deleteGroup(groupName);
                    sendSender(sender, result.getResponse());
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("add")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        world = args[4];
                    if (args.length >= 6)
                        server = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    PMR result = permissionManager.addGroupPermission(groupName, permission, world, server);
                    sendSender(sender, result.getResponse());
                } else if (args.length >= 4 && args[2].equalsIgnoreCase("remove")) {
                    String permission = args[3];
                    String world = "";
                    String server = "";
                    if (args.length >= 5)
                        world = args[4];
                    if (args.length >= 6)
                        server = args[5];
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";
                    PMR result = permissionManager.removeGroupPermission(groupName, permission, world, server);
                    sendSender(sender, result.getResponse());
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

                        PMR result = permissionManager.setGroupPrefix(groupName, prefix);
                        sendSender(sender, result.getResponse());
                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        PMR result = permissionManager.setGroupPrefix(groupName, "");
                        sendSender(sender, result.getResponse());
                    } else {
                        String prefix = permissionManager.getGroupPrefix(groupName);
                        sendSender(sender, "Prefix for group " + groupName + ": " + (prefix.equals("") ? ChatColor.RED + "none" : prefix));
                    }
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

                        PMR result = permissionManager.setGroupSuffix(groupName, suffix);
                        sendSender(sender, result.getResponse());
                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        PMR result = permissionManager.setGroupSuffix(groupName, "");
                        sendSender(sender, result.getResponse());
                    } else {
                        String suffix = permissionManager.getGroupSuffix(groupName);
                        sendSender(sender, "Suffix for group " + groupName + ": " + (suffix.equals("") ? ChatColor.RED + "none" : suffix));
                    }
                } else if (args[2].equalsIgnoreCase("parents")) {
                    if (args.length >= 5 && args[3].equalsIgnoreCase("add")) {
                        String parent = args[4];
                        PMR result = permissionManager.addGroupParent(groupName, parent);
                        sendSender(sender, result.getResponse());
                    } else if (args.length >= 5 && args[3].equalsIgnoreCase("remove")) {
                        String parent = args[4];
                        PMR result = permissionManager.removeGroupParent(groupName, parent);
                        sendSender(sender, result.getResponse());
                    } else {
                        // List parents
                        Group group = permissionManager.getGroup(groupName);
                        if (group != null) {
                            sendSender(sender, "Listing parents for group " + groupName + ":");

                            if (group.getParents() != null && group.getParents().size() > 0) {
                                for (Group g : group.getParents())
                                    sendSender(sender, g.getName());
                            } else
                                sendSender(sender, "Group has no parents.");
                        } else
                            sendSender(sender, "Group doesn't exist.");
                    }
                } else {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        showCommandInfo(sender);
                    }
                }
            }
            if (page != -1 || args.length == 2) {
                if (page == -1)
                    page = 1;
                page--;
                if (page < 0)
                    sendSender(sender, "Invalid page. Page negative.");
                // List group permissions
                Queue<String> rows = new java.util.ArrayDeque<String>();
                Group group = permissionManager.getGroup(groupName);
                if (group != null) {
                    rows.add("Listing permissions for group " + groupName + ":");
                    ArrayList<PowerfulPermission> permissions = group.getPermissions();
                    if (permissions.size() > 0) {
                        for (PowerfulPermission e : permissions)
                            rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + " (Server:"
                                    + (e.getServer() == null || e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
                                    + (e.getServer() == null || e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
                    } else
                        rows.add("Group has no permissions.");

                } else
                    sendSender(sender, "Group doesn't exist.");

                List<List<String>> list = createList(rows, 19);
                sendSender(sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());
                if (page < list.size()) {
                    for (String s : list.get(page))
                        sendSender(sender, s);
                } else
                    sendSender(sender, "Invalid page. Page too high. ");
            }
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("groups")) {
            Collection<Group> groups = permissionManager.getGroups();
            String s = "";
            for (Group group : groups) {
                s += group.getName() + ", ";
            }
            if (s.length() > 0 && groups.size() > 0) {
                s = s.substring(0, s.length() - 2);
            }
            sendSender(sender, "Groups: " + s);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            permissionManager.reloadGroups();
            permissionManager.reloadPlayers();
            sendSender(sender, "Groups and players have been reloaded.");
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("globalreload")) {
            permissionManager.reloadGroups();
            permissionManager.reloadPlayers();

            permissionManager.notifyReloadGroups();
            permissionManager.notifyReloadPlayers();
            sendSender(sender, "Groups and players have been reloaded globally.");
        } else
            showCommandInfo(sender);
        return true;
    }

    private List<List<String>> createList(Queue<String> input, int rowsPerPage) {
        int rowWidth = ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH;
        List<List<String>> list = new ArrayList<List<String>>();
        while (input.size() > 0) {
            List<String> page = new ArrayList<String>();
            for (int j = 0; j < rowsPerPage; j++) {
                if (input.size() > 0) {
                    String row = input.remove();
                    page.add(row);
                    if (row.length() > rowWidth)
                        j++;

                }
            }
            list.add(page);
        }
        return list;
    }

    private void sendSender(CommandSender sender, String message) {
        sender.sendMessage(PowerfulPerms.pluginPrefixShort + message);
    }

    private void showCommandInfo(CommandSender sender) {
        String helpPrefix = "§b ";
        sender.sendMessage(ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
        sender.sendMessage(helpPrefix + "/pp user <username>");
        sender.sendMessage(helpPrefix + "/pp user <username> setprimarygroup <group>");
        sender.sendMessage(helpPrefix + "/pp user <username> addgroup <group> (server)");
        sender.sendMessage(helpPrefix + "/pp user <username> removegroup <group> (server)");
        sender.sendMessage(helpPrefix + "/pp user <username> add/remove <permission> (world) (server)");
        sender.sendMessage(helpPrefix + "/pp user <username> clearperms");
        sender.sendMessage(helpPrefix + "/pp user <username> prefix set/remove <prefix>");
        sender.sendMessage(helpPrefix + "/pp user <username> suffix set/remove <suffix>");
        sender.sendMessage(helpPrefix + "/pp groups");
        sender.sendMessage(helpPrefix + "/pp group <group>");
        sender.sendMessage(helpPrefix + "/pp group <group> create/delete/clearperms");
        sender.sendMessage(helpPrefix + "/pp group <group> add/remove <permission> (world) (server)");
        sender.sendMessage(helpPrefix + "/pp group <group> parents add/remove <parent>");
        sender.sendMessage(helpPrefix + "/pp group <group> prefix set/remove <prefix>");
        sender.sendMessage(helpPrefix + "/pp group <group> suffix set/remove <suffix>");
        sender.sendMessage(helpPrefix + "/pp reload  |  /pp globalreload");
        sender.sendMessage(helpPrefix + "PowerfulPerms version " + PowerfulPerms.getPlugin().getDescription().getVersion() + " by gustav9797");
    }

}
