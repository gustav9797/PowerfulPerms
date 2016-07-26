package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.Map.Entry;

import com.github.cheesesoftware.PowerfulPerms.common.ChatColor;
import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Pair;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;

public class UserCommand extends SubCommand {

    public UserCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user")) {
            if (args != null && args.length >= 1) {
                int page = -1;
                if (args.length == 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        return CommandResult.noMatch;
                    }
                } else if (args.length > 2)
                    return CommandResult.noMatch;

                if (page <= 0)
                    page = 1;
                page--;
                final int pageCopy = page;

                final String playerName = args[0];

                final ResponseRunnable response = new ResponseRunnable() {
                    @Override
                    public void run() {
                        sendSender(invoker, sender, response);
                    }
                };

                permissionManager.getConvertUUID(playerName, new ResultRunnable<UUID>() {

                    @Override
                    public void run() {
                        final UUID uuid = result;
                        if (uuid == null) {
                            response.setResponse(false, "Could not find player UUID.");
                            permissionManager.getScheduler().runSync(response, response.isSameThread());
                        } else {
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

                                            permissionManager.getPlayerCurrentGroups(uuid, new ResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {

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
                                                                Group group = plugin.getPermissionManager().getGroup(currentGroup.getGroupId());
                                                                String ladder = (group != null ? group.getLadder() : "NULL");

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
                                                                Group group = plugin.getPermissionManager().getGroup(cachedGroup.getSecond().getGroupId());
                                                                if (group != null) {
                                                                    otherGroups += (cachedGroup.getSecond().isNegated() ? (ChatColor.RED + "-") : "") + ChatColor.WHITE + group.getName()
                                                                            + (cachedGroup.getFirst() == null || cachedGroup.getFirst().isEmpty() ? ""
                                                                                    : ChatColor.WHITE + ":" + ChatColor.RED + cachedGroup.getFirst())
                                                                            + (cachedGroup.getSecond().willExpire() ? ChatColor.WHITE + ":" + ChatColor.YELLOW
                                                                                    + Utils.getExpirationDateString(cachedGroup.getSecond().getExpirationDate()) : "");
                                                                    otherGroups += ", ";
                                                                }
                                                            }
                                                            if (otherGroups.endsWith(", "))
                                                                otherGroups = otherGroups.substring(0, otherGroups.length() - 2);

                                                            rows.add(otherGroups);
                                                        }
                                                    } else
                                                        rows.add("Player has no groups.");

                                                    permissionManager.getPlayerOwnPermissions(uuid, new ResultRunnable<List<Permission>>() {

                                                        @Override
                                                        public void run() {
                                                            List<Permission> playerPerms = result;
                                                            if (playerPerms != null && playerPerms.size() > 0)
                                                                for (Permission e : playerPerms) {
                                                                    boolean s = !e.getServer().isEmpty();
                                                                    boolean w = !e.getWorld().isEmpty();
                                                                    boolean p = s || w || e.willExpire();
                                                                    rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + (p ? " (" : "")
                                                                            + (e.getServer().isEmpty() ? "" : "Server:" + ChatColor.RED + e.getServer() + ChatColor.WHITE)
                                                                            + (e.getWorld().isEmpty() ? "" : (s ? " " : "") + "World:" + ChatColor.RED + e.getWorld() + ChatColor.WHITE)
                                                                            + (e.willExpire() ? ((s || w ? " " : "") + ChatColor.YELLOW + Utils.getExpirationDateString(e.getExpirationDate())) : "")
                                                                            + ChatColor.WHITE + (p ? ")" : ""));
                                                                }
                                                            else
                                                                rows.add("Player has no permissions.");

                                                            List<List<String>> list = Paginator.createList(rows, 19);
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
                });
                return CommandResult.success;

            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }
}
