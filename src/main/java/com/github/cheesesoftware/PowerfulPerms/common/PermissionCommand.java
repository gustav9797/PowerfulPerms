package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;

import com.github.cheesesoftware.PowerfulPerms.database.DBDocument;

public class PermissionCommand {

    private PermissionManagerBase permissionManager;

    public PermissionCommand(PermissionManagerBase permissionManager) {
        this.permissionManager = permissionManager;
    }

    public boolean onCommand(final ICommand invoker, final String sender, String[] args) {
        ResponseRunnable response = new ResponseRunnable() {
            @Override
            public void run() {
                sendSender(invoker, sender, response);
            }
        };

        if (args.length >= 1 && args[0].equalsIgnoreCase("user") && args.length >= 2) {
            final String playerName = args[1];
            int page = -1;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("clearperms")) {
                    permissionManager.removePlayerPermissions(playerName, response);
                } else if ((args[2].equalsIgnoreCase("addprimary") || args[2].equalsIgnoreCase("setprimary") || args[2].equalsIgnoreCase("setprimarygroup")) && args.length >= 4) {
                    String group = args[3];
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    permissionManager.setPlayerPrimaryGroup(playerName, group, server, response);
                } else if (args[2].equalsIgnoreCase("removeprimary") || args[2].equalsIgnoreCase("removeprimarygroup")) {
                    String server = "";
                    if (args.length >= 4)
                        server = args[3];

                    permissionManager.setPlayerPrimaryGroup(playerName, "", server, response);
                } else if (args[2].equalsIgnoreCase("addgroup") && args.length >= 4) {
                    String group = args[3];
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    boolean negated = group.startsWith("-");
                    if (negated)
                        group = group.substring(1);
                    permissionManager.addPlayerGroup(playerName, group, server, negated, response);
                } else if (args[2].equalsIgnoreCase("removegroup") && args.length >= 4) {
                    String group = args[3];
                    String server = "";
                    if (args.length >= 5)
                        server = args[4];
                    boolean negated = group.startsWith("-");
                    if (negated)
                        group = group.substring(1);
                    permissionManager.removePlayerGroup(playerName, group, server, negated, response);
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
                    permissionManager.addPlayerPermission(playerName, permission, world, server, response);
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
                    permissionManager.removePlayerPermission(playerName, permission, world, server, response);
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

                        permissionManager.setPlayerPrefix(playerName, prefix, response);

                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        permissionManager.setPlayerPrefix(playerName, "", response);
                    } else
                        permissionManager.getPlayerOwnPrefix(playerName, new ResultRunnable() {

                            @Override
                            public void run() {
                                sendSender(invoker, sender, "Prefix for player(non-inherited) " + playerName + ": \"" + (result != null ? (String) result : "") + "\"");
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

                        permissionManager.setPlayerSuffix(playerName, suffix, response);

                    } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                        permissionManager.setPlayerSuffix(playerName, "", response);
                    } else
                        permissionManager.getPlayerOwnSuffix(playerName, new ResultRunnable() {

                            @Override
                            public void run() {
                                sendSender(invoker, sender, "Suffix for player(non-inherited) " + playerName + ": \"" + (result != null ? (String) result : "") + "\"");
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
                final int pageCopy = page;
                // List player permissions
                final Queue<String> rows = new java.util.ArrayDeque<String>();
                rows.add(ChatColor.BLUE + "Listing permissions for player " + playerName + ".");

                permissionManager.getPlayerData(playerName, new ResultRunnable() {

                    @Override
                    public void run() {
                        String tempUUID = "empty";
                        DBDocument row = null;
                        if (result != null) {
                            row = (DBDocument) result;
                            tempUUID = row.getString("uuid");
                        }
                        rows.add(ChatColor.GREEN + "UUID" + ChatColor.WHITE + ": " + tempUUID);

                        IPermissionsPlayer p = permissionManager.getPermissionsPlayer(playerName);
                        if (p != null) {
                            Group pri = p.getPrimaryGroup();
                            if (pri != null)
                                rows.add(ChatColor.GREEN + "Current Primary Group" + ChatColor.WHITE + ": " + pri.getName());
                            else
                                rows.add(ChatColor.RED + "Player has no current primary group." + ChatColor.WHITE);
                        }

                        permissionManager.getPlayerGroups(playerName, new ResultRunnable() {

                            @Override
                            public void run() {
                                HashMap<String, List<CachedGroup>> groups = (HashMap<String, List<CachedGroup>>) result;
                                boolean has = false;
                                String primaryGroups = ChatColor.GREEN + "Primary Groups" + ChatColor.WHITE + ": ";
                                if (groups != null && groups.size() > 0) {
                                    Iterator<Entry<String, List<CachedGroup>>> it = groups.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Entry<String, List<CachedGroup>> current = it.next();
                                        Iterator<CachedGroup> itt = current.getValue().iterator();
                                        while (itt.hasNext()) {
                                            CachedGroup cachedGroup = itt.next();
                                            Group group = cachedGroup.getGroup();
                                            if (group != null && cachedGroup.isPrimary()) {
                                                primaryGroups += ChatColor.WHITE + group.getName() + ":" + ChatColor.RED
                                                        + (current.getKey() == null || current.getKey().isEmpty() ? "ALL" : current.getKey());
                                                primaryGroups += ", ";
                                                has = true;
                                            }
                                        }
                                    }
                                }
                                if (!has)
                                    primaryGroups += "Player has no primary groups.";
                                if (primaryGroups.endsWith(", "))
                                    primaryGroups = primaryGroups.substring(0, primaryGroups.length() - 2);
                                rows.add(primaryGroups);

                                String otherGroups = ChatColor.GREEN + "Groups" + ChatColor.WHITE + ": ";
                                if (groups != null && groups.size() > 0) {
                                    Iterator<Entry<String, List<CachedGroup>>> it = groups.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Entry<String, List<CachedGroup>> current = it.next();
                                        Iterator<CachedGroup> itt = current.getValue().iterator();
                                        while (itt.hasNext()) {
                                            CachedGroup cachedGroup = itt.next();
                                            Group group = cachedGroup.getGroup();
                                            if (group != null && !cachedGroup.isPrimary()) {
                                                otherGroups += (cachedGroup.isNegated() ? (ChatColor.RED + "-") : "") + ChatColor.WHITE + group.getName() + ":" + ChatColor.RED
                                                        + (current.getKey() == null || current.getKey().isEmpty() ? "ALL" : current.getKey());
                                                otherGroups += ", ";
                                            }
                                        }
                                    }
                                } else
                                    otherGroups += "Player has no groups.";
                                if (otherGroups.endsWith(", "))
                                    otherGroups = otherGroups.substring(0, otherGroups.length() - 2);
                                rows.add(otherGroups);

                                permissionManager.getPlayerPermissions(playerName, new ResultRunnable() {

                                    @Override
                                    public void run() {
                                        ArrayList<PowerfulPermission> playerPerms = (ArrayList<PowerfulPermission>) result;
                                        if (playerPerms != null && playerPerms.size() > 0)
                                            for (PowerfulPermission e : playerPerms) {
                                                rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + " (Server:"
                                                        + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
                                                        + (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
                                            }
                                        else
                                            rows.add("Player has no permissions.");

                                        List<List<String>> list = createList(rows, 19);
                                        sendSender(invoker, sender, ChatColor.BLUE + "Page " + (pageCopy + 1) + " of " + list.size());

                                        if (pageCopy < list.size()) {
                                            for (String s : list.get(pageCopy))
                                                sendSender(invoker, sender, s);
                                        } else
                                            sendSender(invoker, sender, "Invalid page. Page too high. ");
                                    }
                                });

                            }
                        });

                    }
                });
            }

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
                    permissionManager.createGroup(groupName, response);
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
                        String output = "";
                        Iterator<Entry<String, String>> it = prefix.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, String> entry = it.next();
                            output += ChatColor.WHITE + "\"" + entry.getValue() + "\":" + (entry.getKey().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : entry.getKey());
                            if (it.hasNext())
                                output += ", ";
                        }
                        sendSender(invoker, sender, "Prefixes for group " + groupName + ": " + output);
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
                        String output = "";
                        Iterator<Entry<String, String>> it = suffix.entrySet().iterator();
                        while (it.hasNext()) {
                            Entry<String, String> entry = it.next();
                            output += ChatColor.WHITE + "\"" + entry.getValue() + "\":" + (entry.getKey().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : entry.getKey());
                            if (it.hasNext())
                                output += ", ";
                        }
                        sendSender(invoker, sender, "Suffixes for group " + groupName + ": " + output);
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
                            sendSender(invoker, sender, "Group doesn't exist.");
                    }
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
                // List group permissions
                Queue<String> rows = new java.util.ArrayDeque<String>();
                Group group = permissionManager.getGroup(groupName);
                if (group != null) {
                    rows.add("Listing permissions for group " + groupName + ":");
                    ArrayList<PowerfulPermission> permissions = group.getOwnPermissions();
                    if (permissions.size() > 0) {
                        for (PowerfulPermission e : permissions)
                            rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + " (Server:"
                                    + (e.getServer() == null || e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
                                    + (e.getServer() == null || e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
                    } else
                        rows.add("Group has no permissions.");

                } else
                    sendSender(invoker, sender, "Group doesn't exist.");

                List<List<String>> list = createList(rows, 19);
                if (list.size() > 0) {
                    sendSender(invoker, sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());
                    if (page < list.size()) {
                        for (String s : list.get(page))
                            sendSender(invoker, sender, s);
                    } else
                        sendSender(invoker, sender, "Invalid page. Page too high. ");
                }
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
            sendSender(invoker, sender, "Groups: " + s);
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            permissionManager.reloadGroups();
            sendSender(invoker, sender, "Groups and players have been reloaded.");
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("globalreload")) {
            permissionManager.reloadGroups();

            permissionManager.notifyReloadGroups();
            permissionManager.notifyReloadPlayers();
            sendSender(invoker, sender, "Groups and players have been reloaded globally.");
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("haspermission")) {
            String permission = args[1];
            IPermissionsPlayer p = permissionManager.getPermissionsPlayer(sender);
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

    private List<List<String>> createList(Queue<String> input, int rowsPerPage) {
        int rowWidth = 55;
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

    private void sendSender(ICommand command, String sender, String message) {
        command.sendSender(sender, PermissionManagerBase.pluginPrefixShort + message);
    }

    private void showCommandInfo(ICommand command, String sender) {
        String helpPrefix = "§b ";
        command.sendSender(sender, ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
        command.sendSender(sender, helpPrefix + "/pp user <username>");
        command.sendSender(sender, helpPrefix + "/pp user <username> setprimary <group> (server)");
        command.sendSender(sender, helpPrefix + "/pp user <username> removeprimary (server)");
        command.sendSender(sender, helpPrefix + "/pp user <username> addgroup/removegroup <group> (server)");
        command.sendSender(sender, helpPrefix + "/pp user <username> add/remove <permission> (server) (world)");
        command.sendSender(sender, helpPrefix + "/pp user <username> clearperms");
        command.sendSender(sender, helpPrefix + "/pp user <username> prefix/suffix set/remove <prefix/suffix>");
        command.sendSender(sender, helpPrefix + "/pp groups");
        command.sendSender(sender, helpPrefix + "/pp group <group>");
        command.sendSender(sender, helpPrefix + "/pp group <group> create/delete/clearperms");
        command.sendSender(sender, helpPrefix + "/pp group <group> add/remove <permission> (server) (world)");
        command.sendSender(sender, helpPrefix + "/pp group <group> parents add/remove <parent>");
        command.sendSender(sender, helpPrefix + "/pp group <group> prefix/suffix set/remove <prefix/suffix> (server)");
        command.sendSender(sender, helpPrefix + "/pp haspermission <permission>");
        command.sendSender(sender, helpPrefix + "/pp reload  |  /pp globalreload");
        command.sendSender(sender, helpPrefix + "PowerfulPerms version " + command.getVersion() + " by gustav9797");
    }

}
