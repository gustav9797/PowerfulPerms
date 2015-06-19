package com.github.cheesesoftware.PowerfulPerms;

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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

public class PermissionManager implements Listener, PluginMessageListener {

    private Plugin plugin;
    private HashMap<UUID, PermissionsPlayer> players = new HashMap<UUID, PermissionsPlayer>();
    private HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    private SQL sql;

    public PermissionManager(Plugin plugin, SQL sql) {
	this.sql = sql;
	this.plugin = plugin;

	plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "PowerfulPerms", this);
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
	    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Could not remove leaving player.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void OnPlayerJoin(PlayerJoinEvent e) {
	LoadPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerChat(AsyncPlayerChatEvent e) {
	e.setFormat(getPlayerPrefix(e.getPlayer()) + e.getPlayer().getDisplayName() + getPlayerSuffix(e.getPlayer()) + e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
	Player p = event.getPlayer();
	if (players.containsKey(p.getUniqueId())) {
	    PermissionsPlayer permissionsPlayer = players.get(p.getUniqueId());
	    permissionsPlayer.UpdatePermissionAttachment();
	}
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
	if (!channel.equals("PowerfulPerms")) {
	    return;
	}

	if (message.length == 1 && message[0] == 0) {
	    LoadGroups();
	    Bukkit.getLogger().info(PowerfulPerms.pluginPrefix + "Reloaded permissions for groups");
	} else {
	    ByteArrayDataInput in = ByteStreams.newDataInput(message);
	    String playerName = in.readUTF();
	    Player p = Bukkit.getPlayer(playerName);
	    if (p != null) {
		LoadPlayer(p);
		Bukkit.getLogger().info(PowerfulPerms.pluginPrefix + "Reloaded permissions for player " + playerName);
	    }
	}
    }

    @SuppressWarnings("resource")
    private void LoadPlayer(Player p) {
	/**
	 * Loads player data from MySQL, removes old data
	 */
	try {
	    String groups_loaded = (getDefaultGroup() != null ? getDefaultGroup().getId() + "" : "1");
	    String prefix_loaded = "";
	    String suffix_loaded = ": "; // Default suffix

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `uuid`=?");
	    s.setString(1, p.getUniqueId().toString());
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		// The player exists in database.
		groups_loaded = result.getString("groups");
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
		    groups_loaded = result.getString("groups");
		    prefix_loaded = result.getString("prefix");

		    s.close();
		} else {
		    // Player does not exist in database. Create a new player.
		    s.close();
		    s = sql.getConnection().prepareStatement("INSERT INTO Players SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
		    s.setString(1, p.getUniqueId().toString());
		    s.setString(2, p.getName());
		    s.setString(3, groups_loaded);
		    s.setString(4, "");
		    s.setString(5, "");
		    s.execute();
		    s.close();
		}
	    }
	    s.close();

	    // Load player permissions.
	    PermissionAttachment pa = p.addAttachment(plugin);
	    ArrayList<PowerfulPermission> perms = loadPlayerPermissions(p);

	    if (players.containsKey(p.getUniqueId())) {
		PermissionsPlayer gp = players.get(p.getUniqueId());
		PermissionAttachment toRemove = gp.getPermissionAttachment();
		if (toRemove != null)
		    toRemove.remove();
	    }

	    // Get player groups. Check if player has any server specific group. If not, set to one general.
	    int playerGroupID = 1;
	    HashMap<String, Integer> playerGroups = getPlayerGroupsRaw(groups_loaded);
	    if (playerGroups.containsKey(Bukkit.getServerName())) {
		playerGroupID = playerGroups.get(Bukkit.getServerName());
	    } else {
		for (Entry<String, Integer> entry : playerGroups.entrySet()) {
		    if (entry.getKey().isEmpty()) {
			playerGroupID = entry.getValue();
			break;
		    }
		}
	    }

	    HashMap<String, Group> playerGroupsGroup = new HashMap<String, Group>();
	    for (Entry<String, Integer> entry : playerGroups.entrySet()) {
		playerGroupsGroup.put(entry.getKey(), this.groups.get(entry.getValue()));
	    }

	    Group playerGroup = groups.get(playerGroupID);
	    if (playerGroup == null) {
		playerGroup = getDefaultGroup();
		Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load player groups, setting default group.");
		if (playerGroup == null)
		    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Default group doesn't exist, this must be created.");
	    }

	    PermissionsPlayer permissionsPlayer = new PermissionsPlayer(p, playerGroup, playerGroupsGroup, perms, pa, prefix_loaded, suffix_loaded);
	    players.put(p.getUniqueId(), permissionsPlayer);
	    permissionsPlayer.UpdatePermissionAttachment();

	} catch (SQLException ex) {
	    ex.printStackTrace();
	}
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
		String parents = result.getString("parents");
		String prefix = result.getString("prefix");
		String suffix = result.getString("suffix");

		tempParents.put(groupId, parents);
		Group group = new Group(groupId, name, loadGroupPermissions(name), prefix, suffix);
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

    private ArrayList<PowerfulPermission> loadPlayerPermissions(Player p) {
	PreparedStatement s;
	boolean needsNameUpdate = false;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM permissions WHERE `playeruuid`=?");
	    s.setString(1, p.getUniqueId().toString());
	    s.execute();
	    ResultSet result = s.getResultSet();
	    ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
	    while (result.next()) {
		PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
		perms.add(tempPerm);

		if (!p.getName().equals(result.getString("playername")))
		    needsNameUpdate = true;
	    }

	    // Update player names if UUID doesn't match. Allows permission indexing by player name.
	    if (needsNameUpdate) {
		s = sql.getConnection().prepareStatement("UPDATE permissions SET `playername`=? WHERE `playeruuid`=?");
		s.setString(1, p.getName());
		s.setString(2, p.getUniqueId().toString());
		s.execute();
		Bukkit.getLogger().info(PowerfulPerms.pluginPrefix + "Player has changed name, updated UUID and name.");
	    }

	    return perms;
	} catch (SQLException e) {
	    e.printStackTrace();
	    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load player permissions.");
	}
	return null;
    }

    private ArrayList<PowerfulPermission> loadPlayerPermissions(String name) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM permissions WHERE `playername`=?");
	    s.setString(1, name);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
	    while (result.next()) {
		PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
		perms.add(tempPerm);
	    }
	    return perms;
	} catch (SQLException e) {
	    e.printStackTrace();
	    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load player permissions.");
	}
	return null;
    }

    private ArrayList<PowerfulPermission> loadGroupPermissions(String groupName) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM permissions WHERE `groupname`=?");
	    s.setString(1, groupName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
	    while (result.next()) {
		PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
		perms.add(tempPerm);
	    }
	    return perms;
	} catch (SQLException e) {
	    e.printStackTrace();
	    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load group permissions.");
	}
	return null;
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

    private HashMap<String, Integer> getPlayerGroupsRaw(String groupsString) {
	HashMap<String, Integer> groups = new HashMap<String, Integer>();
	if (groupsString.contains(";")) {
	    for (String s : groupsString.split(";")) {
		String[] split = s.split(":");
		if (split.length >= 2)
		    groups.put(split[0], Integer.parseInt(split[1]));
		else
		    groups.put("", Integer.parseInt(s));
	    }
	} else if (!groupsString.isEmpty())
	    groups.put("", Integer.parseInt(groupsString));
	return groups;
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
		s = sql.getConnection().prepareStatement("INSERT INTO Players SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?");
		s.setString(1, "");
		s.setString(2, playerName);
		s.setString(3, "1");
		s.setString(4, "");
		s.setString(5, "");
		s.execute();

		s = sql.getConnection().prepareStatement("SELECT * FROM Players WHERE `name`=?");
		s.setString(1, playerName);
		s.execute();
		rs = s.getResultSet();
		if (rs.next())
		    return rs;
		Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Player didn't insert into database properly!");
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
	return getPlayerGroups(playerName).get("");
    }

    public HashMap<String, Group> getPlayerGroups(String playerName) {
	Player p = Bukkit.getServer().getPlayer(playerName);
	if (p != null) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    if (gp != null)
		return gp.getGroups();
	}
	// Player is not online, load from MySQL
	try {
	    ResultSet result = getPlayerData(playerName);
	    HashMap<String, Integer> playerGroups = getPlayerGroupsRaw(result.getString("groups"));
	    HashMap<String, Group> playerGroupsGroup = new HashMap<String, Group>();
	    for (Entry<String, Integer> entry : playerGroups.entrySet()) {
		playerGroupsGroup.put(entry.getKey(), this.groups.get(entry.getValue()));
	    }
	    return playerGroupsGroup;
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
	    if (e.getValue().getName().equalsIgnoreCase(groupName))
		return e.getValue();
	return null;
    }

    public Group getDefaultGroup() {
	return getGroup(PowerfulPerms.defaultGroup);
    }

    @Deprecated
    public Group getGroup(int groupId) {
	/**
	 * Gets a group from its group-id.
	 * 
	 * @param groupId
	 *            The id of the group to get.
	 */
	return groups.get(groupId);
    }

    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {
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
		ArrayList<PowerfulPermission> permissions = loadPlayerPermissions(playerName);

		ResultSet result = getPlayerData(playerName);
		int groupId = result.getInt("group");
		Group group = groups.get(groupId);
		if (group != null) {
		    permissions.addAll(group.getPermissions());
		    return permissions;
		} else {
		    Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Attempted to get permissions of a non-loaded player (Group is null. Group ID:" + groupId + ")");
		    return permissions;
		}

	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	}
	return new ArrayList<PowerfulPermission>();
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
	Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Attempted to get prefix of a non-loaded player");
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
		Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Attempted to get prefix of a player that doesn't exist.");
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
	Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Attempted to get suffix of a non-loaded player");
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
		Bukkit.getLogger().severe(PowerfulPerms.pluginPrefix + "Attempted to get suffix of a player that doesn't exist.");
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

}