package com.github.cheesesoftware.SimplePerms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class PermissionManager implements Listener, PluginMessageListener {

    private Plugin plugin;
    private HashMap<UUID, PermissionsPlayer> players = new HashMap<UUID, PermissionsPlayer>();
    private HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    private SQL sql;

    public PermissionManager(Plugin plugin, SQL sql) {
	this.sql = sql;
	this.plugin = plugin;

	plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
	plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
	Bukkit.getPluginManager().registerEvents(this, plugin);

	try {
	    LoadGroups();
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void OnPlayerQuit(PlayerQuitEvent e) {
	if (players.containsKey(e.getPlayer().getUniqueId())) {
	    PermissionsPlayer gp = players.get(e.getPlayer().getUniqueId());
	    gp.clearPermissions();
	    players.remove(e.getPlayer().getUniqueId());
	} else
	    Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Could not remove leaving player.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void OnPlayerJoin(PlayerJoinEvent e) {
	LoadPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerChat(AsyncPlayerChatEvent e) {
	e.setFormat(getPlayerPrefix(e.getPlayer()) + e.getPlayer().getDisplayName() + getPlayerSuffix(e.getPlayer()) + e.getMessage());
    }

    public void reloadPlayers() {
	for (Player p : Bukkit.getOnlinePlayers()) {
	    if (players.containsKey(p.getUniqueId())) {
		PermissionsPlayer gp = players.get(p.getUniqueId());
		gp.clearPermissions();
		players.remove(p.getUniqueId());
	    }
	    LoadPlayer(p);
	}
    }

    public void reloadGroups() {
	groups.clear();
	LoadGroups();
    }

    public PermissionsPlayer getPermissionsPlayer(Player p) {
	return players.get(p.getUniqueId());
    }

    public PermissionsPlayer getPermissionsPlayer(UUID uuid) {
	return players.get(uuid);
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
	if (!channel.equals("BungeeCord")) {
	    return;
	}

	ByteArrayDataInput in = ByteStreams.newDataInput(message);
	String subChannel = in.readUTF();
	if (subChannel.equals("PermissionManager")) {
	    Bukkit.getLogger().info("Subchannel equals");
	    short len = in.readShort();
	    byte[] msgbytes = new byte[len];
	    in.readFully(msgbytes);

	    DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
	    String somedata;
	    try {
		somedata = msgin.readUTF();
		if (somedata.equals("ReloadPlayer")) {
		    String playerName = msgin.readUTF();
		    Player p = Bukkit.getPlayer(playerName);
		    if (p != null) {
			LoadPlayer(p);
			// p.sendMessage("Your rank has been changed. Please relog when convenient.");
			Bukkit.getLogger().info(SimplePerms.pluginPrefix + "Reloaded permissions for player " + playerName);
		    } // else
		      // Bukkit.getLogger().info("Received player reload packet for offline player " + playerName);
		} else if (somedata.equals("ReloadGroups")) {
		    LoadGroups();
		    Bukkit.getLogger().info(SimplePerms.pluginPrefix + "Reloaded permissions for groups");
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public void setPrefix(Player p) {
	String prefix = getPlayerPrefix(p);
	// prefix = prefix.substring(prefix.length() - 2, prefix.length());
	String name = prefix + " " + p.getDisplayName();
	if (name.length() >= 16) {
	    name = name.substring(0, 15);
	}
	p.setPlayerListName(name);
    }

    private void NotifyReloadPlayer(String playerName) {
	if (Bukkit.getOnlinePlayers().size() > 0) {
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("Forward"); // So BungeeCord knows to forward it
	    out.writeUTF("ONLINE"); // Which server to send to, "ALL" is all and
				    // "ONLINE" is all servers online
	    out.writeUTF("PermissionManager"); // The channel name to check if
					       // this your data

	    ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
	    DataOutputStream msgout = new DataOutputStream(msgbytes);
	    try {
		msgout.writeUTF("ReloadPlayer");
		msgout.writeUTF(playerName);
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    out.writeShort(msgbytes.toByteArray().length);
	    out.write(msgbytes.toByteArray());
	    ((PluginMessageRecipient) Bukkit.getServer().getOnlinePlayers().toArray()[0]).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	    Bukkit.getLogger().info(SimplePerms.pluginPrefix + "Sent packet to reload permissions for player " + playerName);
	} else
	    Bukkit.getLogger().info(SimplePerms.pluginPrefix + "Could not send player reload message to other servers, there is no player online!");
    }

    private void NotifyReloadGroups() {
	if (Bukkit.getOnlinePlayers().size() > 0) {
	    ByteArrayDataOutput out = ByteStreams.newDataOutput();
	    out.writeUTF("Forward"); // So BungeeCord knows to forward it
	    out.writeUTF("ONLINE"); // Which server to send to, "ALL" is all and
				    // "ONLINE" is all servers online
	    out.writeUTF("PermissionManager"); // The channel name to check if
					       // this your data

	    ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
	    DataOutputStream msgout = new DataOutputStream(msgbytes);
	    try {
		msgout.writeUTF("ReloadGroups");
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	    out.writeShort(msgbytes.toByteArray().length);
	    out.write(msgbytes.toByteArray());
	    ((PluginMessageRecipient) Bukkit.getOnlinePlayers().toArray()[0]).sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	    Bukkit.getLogger().info(SimplePerms.pluginPrefix + "Sent packet to reload group permissions");
	} else
	    Bukkit.getLogger().info("Could not send groups reload message to other servers, there is no player online!");
    }

    @SuppressWarnings("resource")
    private void LoadPlayer(Player p) {
	/**
	 * Loads player data from MySQL, removes old data
	 */
	try {
	    if (SimplePerms.getPlugin().getSQL().getConnection().isClosed()) {
		sql.openConnection();
		LoadPlayer(p);
		return;
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	try {
	    int group_loaded = 1;
	    String permissions_loaded = "";
	    String prefix_loaded = "";
	    String suffix_loaded = ": "; // Default suffix

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `uuid`=?");
	    s.setString(1, p.getUniqueId().toString());
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		// The player exists in database.
		group_loaded = result.getInt("group");
		permissions_loaded = result.getString("permissions");
		prefix_loaded = result.getString("prefix");
		suffix_loaded = result.getString("suffix");

		// Check if name mismatch, update player name
		String playerName_loaded = result.getString("name");
		String playerName = p.getName();
		if (!playerName_loaded.equals(playerName)) {
		    s = sql.getConnection().prepareStatement("UPDATE Players SET `name`=? WHERE `uuid`=?;");
		    s.setString(1, p.getName());
		    s.setString(2, p.getUniqueId().toString());
		    s.execute();
		}
	    } else {
		// The player might exist in database but has no UUID yet.
		s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
		s.setString(1, p.getName());
		s.execute();
		result = s.getResultSet();
		if (result.next()) {
		    // Player exists in database but has no UUID. Lets enter it.
		    s = sql.getConnection().prepareStatement("UPDATE Players SET `uuid`=? WHERE `name`=?;");
		    s.setString(1, p.getUniqueId().toString());
		    s.setString(2, p.getName());
		    s.execute();
		    // UUID has been entered into player. Lets continue.
		    group_loaded = result.getInt("group");
		    permissions_loaded = result.getString("permissions");
		    prefix_loaded = result.getString("prefix");

		    s.close();
		} else {
		    // Player does not exist in database. Create a new player.
		    s.close();
		    s = sql.getConnection().prepareStatement("INSERT INTO Players SET `uuid`=?, `name`=?, `group`=?, `permissions`=?, `prefix`=?, `suffix`=?;");
		    s.setString(1, p.getUniqueId().toString());
		    s.setString(2, p.getName());
		    s.setInt(3, 1);
		    s.setString(4, "");
		    s.setString(5, "");
		    s.setString(6, "");
		    s.execute();
		    s.close();
		}
	    }
	    s.close();

	    // Load player permissions.
	    PermissionAttachment pa = getPlayerPermissions(p, permissions_loaded);
	    Map<String, String> betterPerms = getPermissionsMap(permissions_loaded);

	    if (players.containsKey(p.getUniqueId())) {
		PermissionsPlayer gp = players.get(p.getUniqueId());
		PermissionAttachment toRemove = gp.getPermissionAttachment();
		if (toRemove != null)
		    toRemove.remove();
	    }
	    Group playerGroup = groups.get(group_loaded);
	    players.put(p.getUniqueId(), new PermissionsPlayer(p, playerGroup, betterPerms, pa, prefix_loaded, suffix_loaded));

	} catch (SQLException ex) {
	    ex.printStackTrace();
	}
	setPrefix(p);
    }

    private void LoadGroups() {
	/**
	 * Loads groups from MySQL, removes old group data. Will reload all players too.
	 */
	HashMap<Integer, String> tempParents = new HashMap<Integer, String>();
	try {
	    groups.clear();
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM Groups");
	    s.execute();
	    ResultSet result = s.getResultSet();
	    while (result.next()) {
		int groupId = result.getInt("id");
		String name = result.getString("name");
		String permissionsString = result.getString("permissions");
		String parents = result.getString("parents");
		String prefix = result.getString("prefix");
		String suffix = result.getString("suffix");

		tempParents.put(groupId, parents);
		Group group = new Group(groupId, name, getPermissionsMap(permissionsString), prefix, suffix);
		groups.put(groupId, group);
	    }
	} catch (SQLException ex) {
	    ex.printStackTrace();
	}

	Iterator<Entry<Integer, String>> it = tempParents.entrySet().iterator();
	while (it.hasNext()) {
	    Entry<Integer, String> e = it.next();
	    // Bukkit.getLogger().info("Adding parents to " +
	    // groups.get(e.getKey()).getName());
	    ArrayList<Group> finalGroups = new ArrayList<Group>();
	    ArrayList<String> rawParents = getGroupParents(e.getValue());
	    for (String s : rawParents) {
		for (Group testGroup : groups.values()) {
		    // Bukkit.getLogger().info("Comparing " + s + " with " +
		    // testGroup.getId());
		    if (!s.isEmpty() && Integer.parseInt(s) == testGroup.getId()) {
			finalGroups.add(testGroup);
			// Bukkit.getLogger().info("Added parent " +
			// testGroup.getName() + " to " +
			// groups.get(e.getKey()).getName());
			break;
		    }
		}
	    }
	    groups.get(e.getKey()).setParents(finalGroups);
	}

	for (UUID uuid : players.keySet()) {
	    Player toReload = Bukkit.getPlayer(uuid);
	    if (toReload != null)
		LoadPlayer(toReload);
	}
    }

    private PermissionAttachment getPlayerPermissions(Player p, String permissionsString) {
	PermissionAttachment pa = p.addAttachment(plugin);
	if (!permissionsString.isEmpty()) {
	    String[] rawPerms = permissionsString.split(";");
	    for (String tempRawPerm : rawPerms) {
		String[] temp = tempRawPerm.split(":");
		pa.setPermission(temp[0], (temp.length >= 2 ? ((temp[1].equals(Bukkit.getServerName()) || temp[1].isEmpty() || temp[1].equals("ALL")) ? true : false) : false));
		// Bukkit.getLogger().info("Loaded permission " + temp[0] +
		// " for player " + p.getName());
	    }
	}
	return pa;
    }

    private HashMap<String, String> getPermissionsMap(String permissionsString) {
	HashMap<String, String> permissions = new HashMap<String, String>();
	if (!permissionsString.isEmpty()) {
	    String[] rawPerms = permissionsString.split(";");
	    for (String tempRawPerm : rawPerms) {
		String[] temp = tempRawPerm.split(":");
		permissions.put(temp[0], (temp.length >= 2 ? temp[1] : ""));
	    }
	}
	return permissions;
    }

    private ArrayList<String> getGroupParents(String parentsString) {
	ArrayList<String> parents = new ArrayList<String>();
	if (parentsString.contains(";")) {
	    for (String s : parentsString.split(";")) {
		parents.add(s);
	    }
	} else
	    parents.add(parentsString);
	return parents;
    }

    private ResultSet getPlayerData(String playerName) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet rs = s.getResultSet();
	    if (rs.next())
		return rs;
	    else {
		s = sql.getConnection().prepareStatement("INSERT INTO Players SET `uuid`=?, `name`=?, `group`=?, `permissions`=?, `prefix`=?, `suffix`=?");
		s.setString(1, "");
		s.setString(2, playerName);
		s.setInt(3, 1);
		s.setString(4, "");
		s.setString(5, "");
		s.setString(6, "");
		s.execute();

		s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
		s.setString(1, playerName);
		s.execute();
		rs = s.getResultSet();
		if (rs.next())
		    return rs;
		Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Player didn't insert into database properly!");
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return null;
    }

    public Group getPlayerGroup(Player p) {
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null)
	    return gp.getGroup();
	return null;
    }

    public Group getPlayerGroup(String playerName) {
	Player p = Bukkit.getServer().getPlayer(playerName);
	if (p != null) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    if (gp != null)
		return gp.getGroup();
	}
	// Player is not online, load from MySQL
	try {
	    ResultSet result = getPlayerData(playerName);
	    int groupId = result.getInt("group");
	    return groups.get(groupId);
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return null;
    }

    public Group getGroup(String groupName) {
	/**
	 * Gets a group from its name.
	 * 
	 * @param groupName
	 *            The name of the group to get.
	 */
	for (Map.Entry<Integer, Group> e : groups.entrySet())
	    if (e.getValue().getName().equals(groupName))
		return e.getValue();
	return null;
    }

    public Group getGroup(int groupId) {
	/**
	 * Gets a group from its group-id.
	 * 
	 * @param groupId
	 *            The id of the group to get.
	 */
	return groups.get(groupId);
    }

    public Map<String, String> getPlayerPermissions(String playerName) {
	/**
	 * Gets a map containing all the permissions a player has, including its group's permissions and its group's parent groups' permissions. If player is not online data will be loaded from DB.
	 * 
	 * @param p
	 *            The player to get permissions from.
	 */
	Player p = Bukkit.getPlayer(playerName);
	if (p != null) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    if (gp != null)
		return gp.getPermissions();
	} else {
	    // Load from DB
	    try {
		ResultSet result = getPlayerData(playerName);
		int groupId = result.getInt("group");
		String ownPerms = result.getString("permissions");
		Group group = groups.get(groupId);
		if (group != null) {
		    Map<String, String> temp = new HashMap<String, String>();
		    temp.putAll(group.getPermissions());
		    temp.putAll(getPermissionsMap(ownPerms));
		    return temp;
		} else
		    Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Attempted to get permissions of a non-loaded player (Group is null. Group ID:" + groupId + ")");

	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	}
	return new HashMap<String, String>();
    }

    public String getPlayerPrefix(Player p) {
	/**
	 * Gets the prefix of a player. If the player doesn't have a prefix, return the top inherited group's prefix.
	 * 
	 * @param p
	 *            The player to get prefix from.
	 */
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null) {
	    String prefix = gp.getPrefix();
	    return prefix;
	}
	Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Attempted to get prefix of a non-loaded player");
	return null;
    }

    public String getPlayerPrefix(String playerName) {
	/**
	 * Gets the prefix of a player. If the player doesn't have a prefix, return the top inherited group's prefix.
	 * 
	 * @param p
	 *            The player to get prefix from.
	 */
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		return result.getString("prefix");
	    } else
		Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Attempted to get prefix of a player that doesn't exist.");
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return "";
    }

    public String getPlayerSuffix(Player p) {
	/**
	 * Gets the prefix of a player. If the player doesn't have a prefix, return the top inherited group's prefix.
	 * 
	 * @param p
	 *            The player to get prefix from.
	 */
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null) {
	    String suffix = gp.getSuffix();
	    return suffix;
	}
	Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Attempted to get suffix of a non-loaded player");
	return null;
    }

    public String getPlayerSuffix(String playerName) {
	/**
	 * Gets the prefix of a player. If the player doesn't have a prefix, return the top inherited group's prefix.
	 * 
	 * @param p
	 *            The player to get prefix from.
	 */
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		return result.getString("suffix");
	    } else
		Bukkit.getLogger().severe(SimplePerms.pluginPrefix + "Attempted to get suffix of a player that doesn't exist.");
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return "";
    }

    public String getGroupPrefix(String groupName) {
	/**
	 * Gets the prefix of a group.
	 * 
	 * @param groupName
	 *            The group to get prefix from.
	 */
	Group g = getGroup(groupName);
	if (g != null)
	    return g.getPrefix();
	return "";
    }

    public String getGroupSuffix(String groupName) {
	/**
	 * Gets the suffix of a group.
	 * 
	 * @param groupName
	 *            The group to get suffix from.
	 */
	Group g = getGroup(groupName);
	if (g != null)
	    return g.getSuffix();
	return "";
    }

    // -------------------------------------------------------------------//
    // //
    // ------------PLAYER PERMISSION MODIFYING FUNCTIONS BELOW------------//
    // //
    // -------------------------------------------------------------------//

    public PMR AddPlayerPermission(String playerName, String permission) {
	return AddPlayerPermission(playerName, permission, "");
    }

    public PMR AddPlayerPermission(Player player, String permission) {
	return AddPlayerPermission(player.getName(), permission, "");
    }

    public PMR AddPlayerPermission(Player player, String permission, String server) {
	return AddPlayerPermission(player.getName(), permission, server);
    }

    public PMR AddPlayerPermission(String playerName, String permission, String server) {
	boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
	ResultSet rs = getPlayerData(playerName);
	String rawPermissions;
	try {
	    rawPermissions = rs.getString("permissions");
	    rawPermissions += permission + (allServers ? "" : (":" + server)) + ";";
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Players SET `permissions`=? WHERE `name`=?");
	    s.setString(1, rawPermissions);
	    s.setString(2, playerName);
	    s.execute();
	    Player p = Bukkit.getPlayer(playerName);
	    // If player is online, reload his permissions
	    if (p != null)
		LoadPlayer(p);
	    NotifyReloadPlayer(playerName);
	    return new PMR("Permission added to player.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR RemovePlayerPermission(String playerName, String permission) {
	return RemovePlayerPermission(playerName, permission, "");
    }

    public PMR RemovePlayerPermission(Player player, String permission) {
	return RemovePlayerPermission(player.getName(), permission, "");
    }

    public PMR RemovePlayerPermission(Player player, String permission, String server) {
	return RemovePlayerPermission(player.getName(), permission, server);
    }

    public PMR RemovePlayerPermission(String playerName, String permission, String server) {
	boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
	ResultSet rs = getPlayerData(playerName);
	String rawPermissions;
	try {
	    rawPermissions = rs.getString("permissions");
	    String toRemove = permission + (allServers ? "" : (":" + server)) + ";";
	    if (!rawPermissions.contains(toRemove))
		return new PMR(false, "Player doesn't have the specified permission.");
	    rawPermissions = rawPermissions.replace(toRemove, "");
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Players SET `permissions`=? WHERE `name`=?");
	    s.setString(1, rawPermissions);
	    s.setString(2, playerName);
	    s.execute();
	    Player p = Bukkit.getPlayer(playerName);
	    // If player is online, reload his permissions
	    if (p != null)
		LoadPlayer(p);
	    NotifyReloadPlayer(playerName);
	    return new PMR("Removed permission from player.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public void setPlayerPrefix(Player p, String prefix) {
	setPlayerPrefix(p.getName(), prefix);
    }

    public PMR setPlayerPrefix(String playerName, String prefix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Players SET `prefix`=? WHERE `name`=?");
	    s.setString(1, prefix);
	    s.setString(2, playerName);
	    s.execute();
	    Player p = Bukkit.getPlayer(playerName);
	    // If player is online, reload his permissions
	    if (p != null) {
		LoadPlayer(p);
	    }
	    NotifyReloadPlayer(playerName);
	    return new PMR("Player prefix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public void setPlayerSuffix(Player p, String suffix) {
	setPlayerSuffix(p.getName(), suffix);
    }

    public PMR setPlayerSuffix(String playerName, String suffix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Players SET `suffix`=? WHERE `name`=?");
	    s.setString(1, suffix);
	    s.setString(2, playerName);
	    s.execute();
	    Player p = Bukkit.getPlayer(playerName);
	    // If player is online, reload his permissions
	    if (p != null) {
		LoadPlayer(p);
	    }
	    NotifyReloadPlayer(playerName);
	    return new PMR("Player suffix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setPlayerGroup(Player p, int groupId) {
	return setPlayerGroup(p.getName(), groupId);
    }

    public PMR setPlayerGroup(String playerName, int groupId) {
	return setPlayerGroup(playerName, "", groupId);
    }

    public PMR setPlayerGroup(Player p, String groupName) {
	return setPlayerGroup(p.getName(), groupName);
    }

    public PMR setPlayerGroup(String playerName, String groupName) {
	return setPlayerGroup(playerName, groupName, -1);
    }

    private PMR setPlayerGroup(String playerName, String groupName, int groupId) {
	int groupId_new;
	if (groupName.isEmpty())
	    groupId_new = groupId;
	else {
	    Group group = getGroup(groupName);
	    if (group != null)
		groupId_new = group.getId();
	    else
		return new PMR(false, "Group does not exist.");
	}
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Players SET `group`=? WHERE `name`=?");
	    s.setInt(1, groupId_new);
	    s.setString(2, playerName);
	    s.execute();
	    Player p = Bukkit.getPlayer(playerName);
	    // If player is online, reload his permissions
	    if (p != null)
		LoadPlayer(p);
	    NotifyReloadPlayer(playerName);
	    setPrefix(p);
	    return new PMR("Player group set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}

    }

    // -------------------------------------------------------------------//
    // //
    // ------------GROUP PERMISSION MODIFYING FUNCTIONS BELOW-------------//
    // //
    // -------------------------------------------------------------------//

    public PMR CreateGroup(String name) {
	Iterator<Entry<Integer, Group>> it = this.groups.entrySet().iterator();
	while (it.hasNext()) {
	    Entry<Integer, Group> e = it.next();
	    if (e.getValue().getName().equals(name)) {
		// Group already exists
		return new PMR(false, "Group already exists.");
	    }
	}

	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO Groups SET `name`=?, `permissions`=?, `parents`=?, `prefix`=?, `suffix`=?");
	    s.setString(1, name);
	    s.setString(2, "");
	    s.setString(3, "");
	    s.setString(4, "");
	    s.setString(5, "");
	    s.execute();
	    // Reload groups
	    LoadGroups();
	    NotifyReloadGroups();
	    return new PMR("Created group.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "Could not create group: SQL error code: " + e.getErrorCode());
	}
    }

    public PMR DeleteGroup(String groupName) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM Groups WHERE `name`=?;");
	    s.setString(1, groupName);
	    s.execute();
	    // Reload groups
	    LoadGroups();
	    NotifyReloadGroups();
	    return new PMR("Deleted group.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "Could not delete group: SQL error code: " + e.getErrorCode());
	}
    }

    public PMR AddGroupPermission(String groupName, String permission) {
	return AddGroupPermission(groupName, permission, "");
    }

    public PMR AddGroupPermission(String groupName, String permission, String server) {
	boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
	Group group = getGroup(groupName);
	if (group != null) {
	    String groupPermissions = group.getRawOwnPermissions();
	    try {
		groupPermissions += permission + (allServers ? "" : (":" + server)) + ";";
		PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `permissions`=? WHERE `name`=?");
		s.setString(1, groupPermissions);
		s.setString(2, groupName);
		s.execute();
		// Reload groups
		LoadGroups();
		NotifyReloadGroups();
		return new PMR("Added permission to group.");
	    } catch (SQLException e) {
		e.printStackTrace();
		return new PMR(false, "SQL error code: " + e.getErrorCode());
	    }
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR RemoveGroupPermission(String groupName, String permission) {
	return RemoveGroupPermission(groupName, permission, "");
    }

    public PMR RemoveGroupPermission(String groupName, String permission, String server) {
	boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
	Group group = getGroup(groupName);
	if (group != null) {
	    String groupPermissions = group.getRawOwnPermissions();
	    try {
		String toRemove = permission + (allServers ? "" : (":" + server)) + ";";
		if (!groupPermissions.contains(toRemove))
		    return new PMR(false, "Group does not have the specified permission.");
		groupPermissions = groupPermissions.replace(toRemove, "");
		PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `permissions`=? WHERE `name`=?");
		s.setString(1, groupPermissions);
		s.setString(2, groupName);
		s.execute();
		// Reload groups
		LoadGroups();
		NotifyReloadGroups();
		return new PMR("Removed permission from group.");
	    } catch (SQLException e) {
		e.printStackTrace();
		return new PMR(false, "SQL error code: " + e.getErrorCode());
	    }
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR AddGroupParent(String groupName, String parentGroupName) {
	Group group = getGroup(groupName);
	if (group != null) {
	    Group parentGroup = getGroup(parentGroupName);
	    if (parentGroup != null) {
		String currentParents = group.getRawOwnParents();
		currentParents += parentGroup.getId() + ";";
		try {
		    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `parents`=? WHERE `name`=?");
		    s.setString(1, currentParents);
		    s.setString(2, groupName);
		    s.execute();
		    // Reload groups
		    LoadGroups();
		    NotifyReloadGroups();
		    return new PMR("Added parent to group.");
		} catch (SQLException e) {
		    e.printStackTrace();
		    return new PMR(false, "SQL error code " + e.getErrorCode());
		}
	    } else
		return new PMR(false, "Parent group does not exist.");
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR RemoveGroupParent(String groupName, String parentGroupName) {
	Group group = getGroup(groupName);
	if (group != null) {
	    Group parentGroup = getGroup(parentGroupName);
	    if (parentGroup != null) {
		String currentParents = group.getRawOwnParents();
		String toRemove = parentGroup.getId() + ";";
		if (!currentParents.contains(toRemove))
		    return new PMR(false, "Group does not have that parent.");
		currentParents = currentParents.replaceFirst(parentGroup.getId() + ";", "");
		try {
		    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `parents`=? WHERE `name`=?");
		    s.setString(1, currentParents);
		    s.setString(2, groupName);
		    s.execute();
		    // Reload groups
		    LoadGroups();
		    NotifyReloadGroups();
		    return new PMR("Removed parent from group.");
		} catch (SQLException e) {
		    e.printStackTrace();
		    return new PMR(false, "SQL error code: " + e.getErrorCode());
		}
	    } else
		return new PMR(false, "Parent group does not exist.");
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR setGroupPrefix(String groupName, String prefix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `prefix`=? WHERE `name`=?");
	    s.setString(1, prefix);
	    s.setString(2, groupName);
	    s.execute();
	    // Reload groups
	    LoadGroups();
	    NotifyReloadGroups();
	    return new PMR("Set group prefix.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setGroupSuffix(String groupName, String suffix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE Groups SET `suffix`=? WHERE `name`=?");
	    s.setString(1, suffix);
	    s.setString(2, groupName);
	    s.execute();
	    // Reload groups
	    LoadGroups();
	    NotifyReloadGroups();
	    return new PMR("Set group suffix.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }
}