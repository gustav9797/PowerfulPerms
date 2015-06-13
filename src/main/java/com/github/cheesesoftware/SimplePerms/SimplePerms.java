package com.github.cheesesoftware.SimplePerms;

import java.sql.SQLException;
import java.util.ArrayList;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SimplePerms extends JavaPlugin implements Listener {

    private SQL sql;
    private PermissionManager permissionManager;
    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "SimplePerms" + ChatColor.WHITE + "] ";

    public void onEnable() {
	this.saveDefaultConfig();
	getServer().getPluginManager().registerEvents(this, this);
	this.sql = new SQL(getConfig().getString("host"), getConfig().getString("database"), getConfig().getInt("port"), getConfig().getString("username"), getConfig().getString("password"));

	try {
	    if (sql.getConnection() == null || sql.getConnection().isClosed()) {
		Bukkit.getLogger().severe(pluginPrefix + "Could not access the database, disabling..");
		this.setEnabled(false);
	    }
	} catch (SQLException e2) {
	    Bukkit.getLogger().severe(pluginPrefix + "Could not access the database, disabling..");
	    this.setEnabled(false);
	}

	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM groups LIMIT 1;").execute();
	} catch (SQLException e) {
	    String groupsTable = "CREATE TABLE `groups` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`name` varchar(255) NOT NULL,`parents` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`id`),UNIQUE KEY `id_UNIQUE` (`id`),UNIQUE KEY `name_UNIQUE` (`name`)) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(groupsTable).execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM players LIMIT 1;").execute();
	} catch (SQLException e) {
	    String playersTable = "CREATE TABLE `players` (`uuid` varchar(36) NOT NULL DEFAULT '',`name` varchar(32) NOT NULL,`group` int(10) unsigned NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`name`,`uuid`),UNIQUE KEY `uuid_UNIQUE` (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(playersTable).execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	try {
	    sql.getConnection().prepareStatement("SELECT 1 FROM permissions LIMIT 1;").execute();
	} catch (SQLException e) {
	    String permissionsTable = "CREATE TABLE `permissions` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`playeruuid` varchar(36) NOT NULL,`playername` varchar(45) NOT NULL,`groupname` varchar(255) NOT NULL,`permission` varchar(128) NOT NULL,`world` varchar(128) NOT NULL,`server` varchar(128) NOT NULL,PRIMARY KEY (`id`,`playeruuid`,`playername`,`groupname`),UNIQUE KEY `id_UNIQUE` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
	    try {
		sql.getConnection().prepareStatement(permissionsTable).execute();
	    } catch (SQLException e1) {
		e1.printStackTrace();
	    }
	}

	permissionManager = new PermissionManager(this, sql);
    }

    private void showCommandInfo(CommandSender sender) {
	String helpPrefix = "§b ";
	sender.sendMessage(ChatColor.RED + "~ " + ChatColor.BLUE + "SimplePerms" + ChatColor.BOLD + ChatColor.RED + " Reference ~");
	sender.sendMessage(helpPrefix + "/sip user <username>");
	sender.sendMessage(helpPrefix + "/sip user <username> setgroup <group>");
	sender.sendMessage(helpPrefix + "/sip user <username> removegroup");
	sender.sendMessage(helpPrefix + "/sip user <username> add/remove <permission> (world) (server)");
	sender.sendMessage(helpPrefix + "/sip user <username> prefix set/remove <prefix>");
	sender.sendMessage(helpPrefix + "/sip user <username> suffix set/remove <suffix>");
	sender.sendMessage(helpPrefix + "/sip group <group>");
	sender.sendMessage(helpPrefix + "/sip group <group> create/delete");
	sender.sendMessage(helpPrefix + "/sip group <group> add/remove <permission> (world) (server)");
	sender.sendMessage(helpPrefix + "/sip group <group> parents add/remove <parent>");
	sender.sendMessage(helpPrefix + "/sip group <group> prefix set/remove <prefix>");
	sender.sendMessage(helpPrefix + "/sip group <group> suffix set/remove <suffix>");
    }

    @EventHandler
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	if (cmd.getName().equalsIgnoreCase("sip") || cmd.getName().equalsIgnoreCase("simpleperms") && sender.hasPermission("simpleperms.admin")) {
	    if (args.length >= 1 && args[0].equalsIgnoreCase("user") && args.length >= 2) {
		String playerName = args[1];
		if (args.length >= 3) {
		    if (args[2].equalsIgnoreCase("setgroup")) {
			if (args.length >= 4) {
			    String group = args[3];
			    PMR result = permissionManager.setPlayerGroup(playerName, group);
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else
			    showCommandInfo(sender);
			return true;
		    } else if (args[2].equalsIgnoreCase("removegroup")) {
			PMR result = permissionManager.setPlayerGroup(playerName, 1);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			PMR result = permissionManager.AddPlayerPermission(playerName, permission, world, server);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			PMR result = permissionManager.RemovePlayerPermission(playerName, permission, world, server);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			    sender.sendMessage(pluginPrefix + result.getResponse());

			} else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
			    PMR result = permissionManager.setPlayerPrefix(playerName, "");
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else {
			    Player p = Bukkit.getPlayer(playerName);
			    if (p != null) {
				String prefixReceived = permissionManager.getPlayerPrefix(p);
				sender.sendMessage(pluginPrefix + "Prefix for player(inherited) " + playerName + ": " + (prefixReceived.equals("") ? ChatColor.RED + "none" : prefixReceived));
			    } else
				sender.sendMessage(pluginPrefix + "Prefix for player(offline,non-inherited) " + playerName + ": " + permissionManager.getPlayerPrefix(playerName));
			}
			return true;
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
			    sender.sendMessage(pluginPrefix + result.getResponse());

			} else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
			    PMR result = permissionManager.setPlayerSuffix(playerName, "");
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else {
			    Player p = Bukkit.getPlayer(playerName);
			    if (p != null) {
				String suffixReceived = permissionManager.getPlayerSuffix(p);
				sender.sendMessage(pluginPrefix + "Suffix for player(inherited) " + playerName + ": " + (suffixReceived.equals("") ? ChatColor.RED + "none" : suffixReceived));
			    } else
				sender.sendMessage(pluginPrefix + "Suffix for player(offline,non-inherited) " + playerName + ": " + permissionManager.getPlayerSuffix(playerName));
			}
			return true;
		    } else
			showCommandInfo(sender);
		} else {
		    // List player permissions
		    sender.sendMessage(pluginPrefix + "Listing permissions for player " + playerName + ".");
		    Group playerGroup = permissionManager.getPlayerGroup(playerName);
		    sender.sendMessage(pluginPrefix + "Group: " + (playerGroup != null ? playerGroup.getName() : "Player has no group."));
		    // String prefix = permissionManager.getPlayerPrefix(playerName);
		    // sender.sendMessage(pluginPrefix+"Prefix: " + (prefix.isEmpty() ? "Player has no prefix." : prefix));
		    // boolean isOnline = false;
		    // if (Bukkit.getPlayer(playerName) != null)
		    // isOnline = true;
		    ArrayList<SimplePermission> playerPerms = permissionManager.getPlayerPermissions(playerName);
		    if (playerPerms.size() > 0)
			for (SimplePermission e : playerPerms) {
			    sender.sendMessage(pluginPrefix + "  " + e.getPermissionString() + " (Server:" + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer())
				    + " World: " + (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
			}
		    else
			sender.sendMessage(pluginPrefix + "Player has no permissions.");
		    return true;
		}
	    } else if (args.length >= 1 && args[0].equalsIgnoreCase("group") && args.length >= 2) {
		String groupName = args[1];
		if (args.length >= 3) {
		    if (args[2].equalsIgnoreCase("create")) {
			PMR result = permissionManager.CreateGroup(groupName);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
		    } else if (args[2].equalsIgnoreCase("delete")) {
			PMR result = permissionManager.DeleteGroup(groupName);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			PMR result = permissionManager.AddGroupPermission(groupName, permission, world, server);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			PMR result = permissionManager.RemoveGroupPermission(groupName, permission, world, server);
			sender.sendMessage(pluginPrefix + result.getResponse());
			return true;
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
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
			    PMR result = permissionManager.setGroupPrefix(groupName, "");
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else {
			    String prefix = permissionManager.getGroupPrefix(groupName);
			    sender.sendMessage(pluginPrefix + "Prefix for group " + groupName + ": " + (prefix.equals("") ? ChatColor.RED + "none" : prefix));
			}
			return true;
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
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
			    PMR result = permissionManager.setGroupSuffix(groupName, "");
			    sender.sendMessage(pluginPrefix + result.getResponse());
			} else {
			    String suffix = permissionManager.getGroupSuffix(groupName);
			    sender.sendMessage(pluginPrefix + "Suffix for group " + groupName + ": " + (suffix.equals("") ? ChatColor.RED + "none" : suffix));
			}
			return true;
		    } else if (args[2].equalsIgnoreCase("parents")) {
			if (args.length >= 5 && args[3].equalsIgnoreCase("add")) {
			    String parent = args[4];
			    PMR result = permissionManager.AddGroupParent(groupName, parent);
			    sender.sendMessage(pluginPrefix + result.getResponse());
			    return true;
			} else if (args.length >= 5 && args[3].equalsIgnoreCase("remove")) {
			    String parent = args[4];
			    PMR result = permissionManager.RemoveGroupParent(groupName, parent);
			    sender.sendMessage(pluginPrefix + result.getResponse());
			    return true;
			} else {
			    // List parents
			    Group group = permissionManager.getGroup(groupName);
			    if (group != null) {
				sender.sendMessage(pluginPrefix + "Listing parents for group " + groupName + ":");
				if (group.getParents() != null && group.getParents().size() > 0) {
				    for (Group g : group.getParents())
					sender.sendMessage(pluginPrefix + "  " + g.getName());
				} else
				    sender.sendMessage(pluginPrefix + "Group has no parents.");

			    } else
				sender.sendMessage(pluginPrefix + "Group doesn't exist.");
			    return true;
			}
		    } else
			showCommandInfo(sender);
		} else {
		    // List group permissions
		    Group group = permissionManager.getGroup(groupName);
		    if (group != null) {
			sender.sendMessage(pluginPrefix + "Listing permissions for group " + groupName + ":");

			ArrayList<SimplePermission> permissions = group.getPermissions();
			if (permissions.size() > 0) {
			    for (SimplePermission e : permissions)
				sender.sendMessage(pluginPrefix + "  " + e.getPermissionString() + " (Server:" + (e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer())
					+ " World:" + (e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
			} else
			    sender.sendMessage(pluginPrefix + "Group has no permissions.");

		    } else
			sender.sendMessage(pluginPrefix + "Group doesn't exist.");
		    return true;
		}
	    } else
		showCommandInfo(sender);
	    return true;
	}
	return false;
    }

    public static SimplePerms getPlugin() {
	return (SimplePerms) Bukkit.getPluginManager().getPlugin("SimplePerms");
    }

    public SQL getSQL() {
	return this.sql;
    }
}
