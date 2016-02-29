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
                            final int pageCopy = page;
                            // List player permissions
                            final Queue<String> rows = new java.util.ArrayDeque<String>();
                            rows.add(ChatColor.BLUE + "Listing permissions for player " + playerName + ".");

                            permissionManager.getPlayerData(uuid, new ResultRunnable<DBDocument>() {

                                @Override
                                public void run() {
                                    String tempUUID = "empty";
                                    DBDocument row = result;
                                    if (result != null)
                                        tempUUID = row.getString("uuid");
                                    rows.add(ChatColor.GREEN + "UUID" + ChatColor.WHITE + ": " + tempUUID);

                                    permissionManager.isPlayerDefault(uuid, new ResultRunnable<Boolean>() {

                                        @Override
                                        public void run() {
                                            if (result)
                                                rows.add("This player has no groups and is using [default] groups.");

                                            permissionManager.getPlayerCurrentGroups(uuid, new ResultRunnable<Map<String, List<CachedGroup>>>() {

                                                @Override
                                                public void run() {

                                                    Map<String, List<CachedGroup>> groups = result;
                                                    if (groups == null)
                                                        groups = new LinkedHashMap<String, List<CachedGroup>>();

                                                    // Store by ladder instead of server
                                                    Map<String, List<Pair<String, CachedGroup>>> ladderGroups = new LinkedHashMap<String, List<Pair<String, CachedGroup>>>();
                                                    Iterator<Entry<String, List<CachedGroup>>> it = groups.entrySet().iterator();
                                                    while (it.hasNext()) {
                                                        Entry<String, List<CachedGroup>> currentGroups = it.next();
                                                        if (currentGroups != null) {
                                                            Iterator<CachedGroup> it2 = currentGroups.getValue().iterator();
                                                            while (it2.hasNext()) {
                                                                CachedGroup currentGroup = it2.next();
                                                                String ladder = currentGroup.getGroup().getLadder();

                                                                List<Pair<String, CachedGroup>> out = ladderGroups.get(ladder);
                                                                if (out == null)
                                                                    out = new ArrayList<Pair<String, CachedGroup>>();

                                                                out.add(new Pair<String, CachedGroup>(currentGroups.getKey(), currentGroup));
                                                                ladderGroups.put(ladder, out);
                                                            }
                                                        }
                                                    }

                                                    // List groups
                                                    // String otherGroups = ChatColor.GREEN + "Groups" + ChatColor.WHITE + ": ";
                                                    if (groups != null && groups.size() > 0) {
                                                        Iterator<Entry<String, List<Pair<String, CachedGroup>>>> it3 = ladderGroups.entrySet().iterator();
                                                        while (it3.hasNext()) {
                                                            Entry<String, List<Pair<String, CachedGroup>>> current = it3.next();
                                                            Iterator<Pair<String, CachedGroup>> it4 = current.getValue().iterator();
                                                            String otherGroups = ChatColor.GREEN + "On ladder " + ChatColor.WHITE + "\"" + current.getKey() + "\": ";
                                                            while (it4.hasNext()) {
                                                                Pair<String, CachedGroup> cachedGroup = it4.next();
                                                                Group group = cachedGroup.getSecond().getGroup();
                                                                if (group != null) {
                                                                    otherGroups += (cachedGroup.getSecond().isNegated() ? (ChatColor.RED + "-") : "") + ChatColor.WHITE + group.getName() + ":"
                                                                            + ChatColor.RED + (cachedGroup.getFirst() == null || cachedGroup.getFirst().isEmpty() ? "ALL" : cachedGroup.getFirst());
                                                                    otherGroups += ", ";
                                                                }
                                                            }
                                                            if (otherGroups.endsWith(", "))
                                                                otherGroups = otherGroups.substring(0, otherGroups.length() - 2);

                                                            rows.add(otherGroups);
                                                        }
                                                    } else
                                                        rows.add("Player has no groups.");

                                                    // List groups
                                                    /*-String otherGroups = ChatColor.GREEN + "Groups" + ChatColor.WHITE + ": ";
                                                    if (groups != null && groups.size() > 0) {
                                                        Iterator<Entry<String, List<CachedGroup>>> it = groups.entrySet().iterator();
                                                        while (it.hasNext()) {
                                                            Entry<String, List<CachedGroup>> current = it.next();
                                                            Iterator<CachedGroup> itt = current.getValue().iterator();
                                                            while (itt.hasNext()) {
                                                                CachedGroup cachedGroup = itt.next();
                                                                Group group = cachedGroup.getGroup();
                                                                if (group != null) {
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
                                                    rows.add(otherGroups);*/

                                                    permissionManager.getPlayerOwnPermissions(uuid, new ResultRunnable<List<Permission>>() {

                                                        @Override
                                                        public void run() {
                                                            List<Permission> playerPerms = result;
                                                            if (playerPerms != null && playerPerms.size() > 0)
                                                                for (Permission e : playerPerms) {
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
                            });
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
                    rows.add(ChatColor.BLUE + "Listing permissions for group " + groupName + ".");
                    rows.add(ChatColor.GREEN + "Ladder" + ChatColor.WHITE + ": " + group.getLadder());
                    rows.add(ChatColor.GREEN + "Rank" + ChatColor.WHITE + ": " + group.getRank());
                    List<Permission> permissions = group.getOwnPermissions();
                    if (permissions.size() > 0) {
                        for (Permission e : permissions)
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
            Map<Integer, Group> groups = permissionManager.getGroups();
            String s = "";
            for (Group group : groups.values()) {
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
        command.sendSender(sender, helpPrefix + "PowerfulPerms version " + command.getVersion() + " by gustav9797");
    }

}
