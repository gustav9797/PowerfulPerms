package com.github.cheesesoftware.PowerfulPerms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PermissionCommand implements CommandExecutor {

    private PermissionManager permissionManager;

    public PermissionCommand(PermissionManager permissionManager) {
	this.permissionManager = permissionManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	if (args.length >= 1 && args[0].equalsIgnoreCase("user") && args.length >= 2) {
	    String playerName = args[1];
	    if (args.length >= 3) {
		if (args[2].equalsIgnoreCase("setprimarygroup") && args.length >= 4) {
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
		} else
		    showCommandInfo(sender);
	    } else {
		// List player permissions
		sendSender(sender, "Listing permissions for player " + playerName + ".");
		ResultSet result = permissionManager.getPlayerData(playerName);
		try {
		    sendSender(sender, "UUID: " + result.getString("uuid"));
		} catch (SQLException e1) {
		    e1.printStackTrace();
		}
		HashMap<String, List<Group>> groups = permissionManager.getPlayerGroups(playerName);
		Group primary = permissionManager.getPlayerPrimaryGroup(playerName);
		sendSender(sender, "Primary Group: " + (primary != null ? primary.getName() : "Player has no group."));

		String otherGroups = PowerfulPerms.pluginPrefix + "Groups: ";
		if (groups.size() > 0) {
		    Iterator<Entry<String, List<Group>>> it = groups.entrySet().iterator();
		    while (it.hasNext()) {
			Entry<String, List<Group>> current = it.next();
			Iterator<Group> itt = current.getValue().iterator();
			while (itt.hasNext()) {
			    Group group = itt.next();
			    otherGroups += (current.getKey().isEmpty() ? "ALL" : current.getKey()) + ":" + group.getName();
			    if (it.hasNext() || itt.hasNext())
				otherGroups += ", ";
			}
		    }
		}
		sendSender(sender, otherGroups);
		// String prefix = permissionManager.getPlayerPrefix(playerName);
		// sender.sendMessage(pluginPrefix+"Prefix: " + (prefix.isEmpty() ? "Player has no prefix." : prefix));
		// boolean isOnline = false;
		// if (Bukkit.getPlayer(playerName) != null)
		// isOnline = true;
		ArrayList<PowerfulPermission> playerPerms = permissionManager.getPlayerPermissions(playerName);
		if (playerPerms.size() > 0)
		    for (PowerfulPermission e : playerPerms) {
			sendSender(sender, e.getPermissionString() + " (Server:" + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
				+ (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
		    }
		else
		    sendSender(sender, "Player has no permissions.");
	    }
	} else if (args.length >= 1 && args[0].equalsIgnoreCase("group") && args.length >= 2) {
	    String groupName = args[1];
	    if (args.length >= 3) {
		if (args[2].equalsIgnoreCase("create")) {
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
		} else
		    showCommandInfo(sender);
	    } else {
		// List group permissions
		Group group = permissionManager.getGroup(groupName);
		if (group != null) {
		    sendSender(sender, "Listing permissions for group " + groupName + ":");
		    ArrayList<PowerfulPermission> permissions = group.getPermissions();
		    if (permissions.size() > 0) {
			for (PowerfulPermission e : permissions)
			    sendSender(sender, e.getPermissionString() + " (Server:" + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
				    + (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
		    } else
			sendSender(sender, "Group has no permissions.");

		} else
		    sendSender(sender, "Group doesn't exist.");
	    }
	} else if (args.length >= 1 && args[0].equalsIgnoreCase("groups")) {
	    List<Group> groups = permissionManager.getGroups();
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

    private void sendSender(CommandSender sender, String message) {
	sender.sendMessage(PowerfulPerms.pluginPrefix + message);
    }

    private void showCommandInfo(CommandSender sender) {
	String helpPrefix = "§b ";
	sender.sendMessage(ChatColor.RED + "~ " + ChatColor.BLUE + "PowerfulPerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
	sender.sendMessage(helpPrefix + "/pp user <username>");
	sender.sendMessage(helpPrefix + "/pp user <username> setprimarygroup <group>");
	sender.sendMessage(helpPrefix + "/pp user <username> addgroup <group> (server)");
	sender.sendMessage(helpPrefix + "/pp user <username> removegroup <group> (server)");
	sender.sendMessage(helpPrefix + "/pp user <username> add/remove <permission> (world) (server)");
	sender.sendMessage(helpPrefix + "/pp user <username> prefix set/remove <prefix>");
	sender.sendMessage(helpPrefix + "/pp user <username> suffix set/remove <suffix>");
	sender.sendMessage(helpPrefix + "/pp groups");
	sender.sendMessage(helpPrefix + "/pp group <group>");
	sender.sendMessage(helpPrefix + "/pp group <group> create/delete");
	sender.sendMessage(helpPrefix + "/pp group <group> add/remove <permission> (world) (server)");
	sender.sendMessage(helpPrefix + "/pp group <group> parents add/remove <parent>");
	sender.sendMessage(helpPrefix + "/pp group <group> prefix set/remove <prefix>");
	sender.sendMessage(helpPrefix + "/pp group <group> suffix set/remove <suffix>");
	sender.sendMessage(helpPrefix + "/pp reload  |  /pp globalreload");
	sender.sendMessage(helpPrefix + "PowerfulPerms version " + PowerfulPerms.getPlugin().getDescription().getVersion() + " by gustav9797");
    }

}
