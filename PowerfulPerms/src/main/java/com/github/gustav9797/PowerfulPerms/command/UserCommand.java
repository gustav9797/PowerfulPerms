package com.github.gustav9797.PowerfulPerms.command;

import com.github.gustav9797.PowerfulPerms.common.ChatColor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.Map.Entry;

import com.github.gustav9797.PowerfulPerms.common.ICommand;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.DBDocument;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.Pair;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;

public class UserCommand extends SubCommand {

    public UserCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user>");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, String[] args) throws InterruptedException, ExecutionException {
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

                final String playerName = args[0];

                UUID uuid = permissionManager.getConvertUUIDBase(playerName);
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
                } else {
                    // List player permissions
                    final Queue<String> rows = new java.util.ArrayDeque<>();
                    rows.add(ChatColor.BLUE + "Listing permissions for player " + playerName + ".");

                    String tempUUID = "empty";
                    DBDocument row;
                    row = permissionManager.getPlayerDataBase(uuid);
                    if (row != null)
                        tempUUID = row.getString("uuid");
                    rows.add(ChatColor.GREEN + "UUID" + ChatColor.WHITE + ": " + tempUUID);

                    if (permissionManager.isPlayerDefaultBase(uuid))
                        rows.add("This player has no groups and is using [default] groups.");

                    Map<String, List<CachedGroup>> groups = permissionManager.getPlayerCurrentGroupsBase(uuid);
                    if (groups == null)
                        groups = new LinkedHashMap<>();

                    // Store by ladder instead of server
                    Map<String, List<Pair<String, CachedGroup>>> ladderGroups = new LinkedHashMap<>();
                    for (Entry<String, List<CachedGroup>> currentGroups : groups.entrySet()) {
                        if (currentGroups != null) {
                            for (CachedGroup currentGroup : currentGroups.getValue()) {
                                Group group = plugin.getPermissionManager().getGroup(currentGroup.getGroupId());
                                String ladder = (group != null ? group.getLadder() : "NULL");

                                List<Pair<String, CachedGroup>> out = ladderGroups.get(ladder);
                                if (out == null)
                                    out = new ArrayList<>();

                                out.add(new Pair<>(currentGroups.getKey(), currentGroup));
                                ladderGroups.put(ladder, out);
                            }
                        }
                    }

                    // List groups
                    // String otherGroups = ChatColor.GREEN + "Groups" + ChatColor.WHITE + ": ";
                    if (groups != null && groups.size() > 0) {
                        for (Entry<String, List<Pair<String, CachedGroup>>> current : ladderGroups.entrySet()) {
                            Iterator<Pair<String, CachedGroup>> it4 = current.getValue().iterator();
                            String otherGroups = ChatColor.GREEN + "On ladder " + ChatColor.WHITE + "\"" + current.getKey() + "\": ";
                            while (it4.hasNext()) {
                                Pair<String, CachedGroup> cachedGroup = it4.next();
                                Group group = plugin.getPermissionManager().getGroup(cachedGroup.getSecond().getGroupId());
                                if (group != null) {
                                    otherGroups += (cachedGroup.getSecond().isNegated() ? (ChatColor.RED + "-") : "") + ChatColor.WHITE + group.getName()
                                            + (cachedGroup.getFirst() == null || cachedGroup.getFirst().isEmpty() ? "" : ChatColor.WHITE + ":" + ChatColor.RED + cachedGroup.getFirst())
                                            + (cachedGroup.getSecond().willExpire()
                                            ? ChatColor.WHITE + ":" + ChatColor.YELLOW + Utils.getExpirationDateString(cachedGroup.getSecond().getExpirationDate()) : "");
                                    otherGroups += ", ";
                                }
                            }
                            if (otherGroups.endsWith(", "))
                                otherGroups = otherGroups.substring(0, otherGroups.length() - 2);

                            rows.add(otherGroups);
                        }
                    } else
                        rows.add("Player has no groups.");

                    List<Permission> playerPerms = permissionManager.getPlayerOwnPermissionsBase(uuid);
                    if (playerPerms != null && playerPerms.size() > 0)
                        for (Permission e : playerPerms) {
                            boolean s = !e.getServer().isEmpty();
                            boolean w = !e.getWorld().isEmpty();
                            boolean p = s || w || e.willExpire();
                            rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + (p ? " (" : "")
                                    + (e.getServer().isEmpty() ? "" : "Server:" + ChatColor.RED + e.getServer() + ChatColor.WHITE)
                                    + (e.getWorld().isEmpty() ? "" : (s ? " " : "") + "World:" + ChatColor.RED + e.getWorld() + ChatColor.WHITE)
                                    + (e.willExpire() ? ((s || w ? " " : "") + ChatColor.YELLOW + Utils.getExpirationDateString(e.getExpirationDate())) : "") + ChatColor.WHITE + (p ? ")" : ""));
                        }
                    else
                        rows.add("Player has no permissions.");

                    List<List<String>> list = Paginator.createList(rows, 19);
                    sendSender(invoker, sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());

                    if (page < list.size()) {
                        for (String s : list.get(page))
                            sendSender(invoker, sender, s);
                    } else
                        sendSender(invoker, sender, "Invalid page. Page too high. ");
                }
                return CommandResult.success;

            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        return null;
    }
}
