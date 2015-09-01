package com.github.cheesesoftware.PowerfulPerms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.impl.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PMR;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPerms;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public class PermissionManager implements Listener {

    private Plugin plugin;
    private HashMap<UUID, PermissionsPlayer> players = new HashMap<UUID, PermissionsPlayer>();
    private ConcurrentHashMap<UUID, CachedPlayer> cachedPlayers = new ConcurrentHashMap<UUID, CachedPlayer>();
    //private CopyOnWriteArrayList<UUID> connectingOnlinePlayers = new CopyOnWriteArrayList<UUID>();

    private HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    private SQL sql;

    private JedisPool pool;
    private JedisPubSub subscriber;

    public PermissionManager(Plugin plugin, SQL sql) {
	this.sql = sql;
	this.plugin = plugin;

	// Initialize Redis
	if (PowerfulPerms.redis_password == null || PowerfulPerms.redis_password.equals(""))
	    pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0);
	else
	    pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0, PowerfulPerms.redis_password);
	final Plugin tempPlugin = plugin;
	Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		Jedis jedis = null;
		try {
		    jedis = pool.getResource();
		    subscriber = (new JedisPubSub() {
			@Override
			public void onMessage(String channel, final String msg) {
			    Bukkit.getScheduler().runTaskAsynchronously(tempPlugin, new Runnable() {
				public void run() {
				    // Reload player or groups depending on msg
				    String[] split = msg.split(" ");
				    if (split.length == 2) {
					String first = split[0];
					String server = split[1];

					if (server.equals(Bukkit.getServerName()))
					    return;
					/*if(first.equals("[servername]") && split.length > 1) {
					    PowerfulPerms.serverName = msg.substring(12);
					    debug("Received servername \"" + PowerfulPerms.serverName + "\"");
					}
					else */if (first.equals("[groups]")) {
					    loadGroups();
					    Bukkit.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded all groups.");
					} else if (first.equals("[players]")) {
					    loadGroups();
					    Bukkit.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded all players. ");
					} else {
					    Player player = Bukkit.getPlayer(first);
					    if (player != null) {
						loadPlayer(player);
						Bukkit.getLogger().info(PowerfulPerms.consolePrefix + "Reloaded player \"" + first + "\".");
					    }
					}
				    }
				}
			    });
			}
		    });
		    jedis.subscribe(subscriber, "PowerfulPerms");
		} catch (Exception e) {
		    pool.returnBrokenResource(jedis);
		    Bukkit.getLogger().warning(
			    PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
		    return;
		}
		pool.returnResource(jedis);
	    }
	});
	// plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "PowerfulPerms", this);
	Bukkit.getPluginManager().registerEvents(this, plugin);

	try {
	    loadGroups();
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    private void debug(String msg) {
	if (PowerfulPerms.debug) {
	    Bukkit.getLogger().info("[DEBUG] " + msg);
	}
    }

    public void onDisable() {
	if (subscriber != null)
	    subscriber.unsubscribe();
	if (pool != null)
	    pool.destroy();
	players.clear();
	groups.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
	debug("PlayerQuitEvent " + e.getPlayer().getName());
	if (players.containsKey(e.getPlayer().getUniqueId())) {
	    players.remove(e.getPlayer().getUniqueId());
	}
	if (cachedPlayers.containsKey(e.getPlayer().getUniqueId()))
	    cachedPlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent e) {
	debug("AsyncPlayerPreLoginEvent " + e.getName());
	
	//try { Thread.sleep(4000); } catch (InterruptedException ex) { ex.printStackTrace(); }
	 
	if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
	    loadPlayer(e.getUniqueId(), e.getName(), true);
	}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent e) {
	debug("PlayerLoginEvent " + e.getPlayer().getName());
	// if (e.getResult() == PlayerLoginEvent.Result.ALLOWED) {

	// }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent e) {
	debug("PlayerJoinEvent " + e.getPlayer().getName());
	// Check again if
	if (cachedPlayers.containsKey(e.getPlayer().getUniqueId())) {
	    // Player is cached. Continue load it.
	    continueLoadPlayer(e.getPlayer());
	}
	else
	    debug("onPlayerJoin player isn't cached");
	
	/*else if (!players.containsKey(e.getPlayer().getUniqueId())) {
	    // MySQL connection is extremely slow so we let it load by itself when it finishes.
	    CachedPlayer temp = new CachedPlayer();
	    cachedPlayers.put(e.getPlayer().getUniqueId(), temp);
	}*/
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
	// String out = + e.getPlayer().getDisplayName() + getPlayerSuffix(e.getPlayer());
	// out = ChatColor.translateAlternateColorCodes('&', out);
	e.setFormat(ChatColor.translateAlternateColorCodes('&', getPlayerPrefix(e.getPlayer()) + "%1$s" + getPlayerSuffix(e.getPlayer()) + "%2$s"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
	Player p = event.getPlayer();
	debug("Player " + p.getName() + " changed world from " + event.getFrom().getName() + " to " + p.getWorld().getName());
	if (players.containsKey(p.getUniqueId())) {
	    PermissionsPlayer permissionsPlayer = players.get(p.getUniqueId());
	    permissionsPlayer.UpdatePermissionAttachment();
	}
    }
    
    /*public void notifyLoadServerName() {
	Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		try {
		    Jedis jedis = pool.getResource();
		    try {
			jedis.publish("PowerfulPerms", "[requestservername]");
			debug("Requested server name");
		    } catch (Exception e) {
			pool.returnBrokenResource(jedis);
		    }
		    pool.returnResource(jedis);
		} catch (Exception e) {
		    Bukkit.getLogger().warning(
			    PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
		}
	    }
	});
    }*/

    public void notifyReloadGroups() {
	Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		try {
		    Jedis jedis = pool.getResource();
		    try {
			jedis.publish("PowerfulPerms", "[groups]" + " " + Bukkit.getServerName());
		    } catch (Exception e) {
			pool.returnBrokenResource(jedis);
		    }
		    pool.returnResource(jedis);
		} catch (Exception e) {
		    Bukkit.getLogger().warning(
			    PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
		}
	    }
	});
    }

    public void notifyReloadPlayers() {
	Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		try {
		    Jedis jedis = pool.getResource();
		    try {
			jedis.publish("PowerfulPerms", "[players]" + " " + Bukkit.getServerName());
		    } catch (Exception e) {
			pool.returnBrokenResource(jedis);
		    }
		    pool.returnResource(jedis);

		} catch (Exception e) {
		    Bukkit.getLogger().warning(
			    PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
		}
	    }
	});
    }

    public void notifyReloadPlayer(final String playerName) {
	Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
	    @SuppressWarnings("deprecation")
	    public void run() {
		try {
		    Jedis jedis = pool.getResource();
		    try {
			jedis.publish("PowerfulPerms", playerName + " " + Bukkit.getServerName());
		    } catch (Exception e) {
			pool.returnBrokenResource(jedis);
		    }
		    pool.returnResource(jedis);
		} catch (Exception e) {
		    Bukkit.getLogger().warning(
			    PowerfulPerms.consolePrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
		}
	    }
	});
    }

    public void reloadPlayers() {
	for (Player p : Bukkit.getOnlinePlayers()) {
	    if (players.containsKey(p.getUniqueId())) {
		PermissionsPlayer gp = players.get(p.getUniqueId());
		gp.clearPermissions();
		players.remove(p.getUniqueId());
	    }
	    loadPlayer(p);
	}
    }

    public void reloadGroups() {
	groups.clear();
	loadGroups();
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     * 
     * @param p
     *            The player to get a PermissionsPlayer-object from.
     * @return PermissionsPlayer if player is online, null if player is offline.
     */
    public PermissionsPlayer getPermissionsPlayer(Player p) {
	return players.get(p.getUniqueId());
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     * 
     * @param name
     *            The name of the player to get a PermissionsPlayer-object from.
     * @return PermissionsPlayer if player is online, null if player is offline.
     */
    public PermissionsPlayer getPermissionsPlayer(String playerName) {
	Player p = Bukkit.getPlayer(playerName);
	if (p != null)
	    return players.get(p.getUniqueId());
	return null;
    }

    /**
     * Returns the PermissionsPlayer-object for the specified player, used for getting permissions information about the player. Player has to be online.
     * 
     * @param uuid
     *            The UUID of the player to get a PermissionsPlayer-object from.
     * @return PermissionsPlayer if player is online, null if player is offline.
     */
    public PermissionsPlayer getPermissionsPlayer(UUID uuid) {
	return players.get(uuid);
    }

    private void loadPlayer(Player player) {
	loadPlayer(player.getUniqueId(), player.getName(), false);
    }

    @SuppressWarnings("resource")
    private void loadPlayer(UUID uuid, String name, boolean login) {
	/**
	 * Loads player data from MySQL, removes old data
	 */
	try {
	    String groups_loaded = "";
	    String prefix_loaded = "";
	    String suffix_loaded = ": ";

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `uuid`=?");
	    s.setString(1, uuid.toString());
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		// The player exists in database.
		groups_loaded = result.getString("groups");
		prefix_loaded = result.getString("prefix");
		suffix_loaded = result.getString("suffix");

		// Check if name mismatch, update player name
		String playerName_loaded = result.getString("name");
		String playerName = name;
		if (!playerName_loaded.equals(playerName)) {
		    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `name`=? WHERE `uuid`=?;");
		    s.setString(1, name);
		    s.setString(2, uuid.toString());
		    s.execute();
		}
	    } else {
		// The player might exist in database but has no UUID yet.
		s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
		s.setString(1, name);
		s.execute();
		result = s.getResultSet();
		if (result.next()) {
		    // Player exists in database but has no UUID. Lets enter it.
		    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `uuid`=? WHERE `name`=?;");
		    s.setString(1, uuid.toString());
		    s.setString(2, name);
		    s.execute();
		    // UUID has been entered into player. Lets continue.
		    groups_loaded = result.getString("groups");
		    prefix_loaded = result.getString("prefix");
		    suffix_loaded = result.getString("suffix");

		    s.close();
		} else {
		    // Player does not exist in database. Create a new player.
		    s.close();

		    result = getPlayerData("[default]");
		    groups_loaded = result.getString("groups");
		    prefix_loaded = result.getString("prefix");
		    suffix_loaded = result.getString("suffix");

		    s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
		    s.setString(1, uuid.toString());
		    s.setString(2, name);
		    s.setString(3, groups_loaded);
		    s.setString(4, prefix_loaded);
		    s.setString(5, suffix_loaded);
		    s.execute();
		    s.close();
		}
	    }
	    s.close();

	    /*
	     * debug("begin sleep");
	     * 
	     * try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
	     * 
	     * debug("end sleep");
	     */

	    ArrayList<PowerfulPermission> perms = loadPlayerPermissions(uuid);

	    if (login) {
		/*if (cachedPlayers.containsKey(uuid)) {
		    final Player player = Bukkit.getServer().getPlayer(uuid);
		    if (player != null) {
			cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));
			Bukkit.getLogger().warning(PowerfulPerms.consolePrefix + "Your MySQL connection is running slow. Permission checks in player join event may not work as expected.");
			Bukkit.getScheduler().runTask(plugin, new Runnable() {
			    public void run() {
				continueLoadPlayer(player);
			    }
			});
		    }
		    return;
		}*/

		debug("Inserted into cachedPlayers allowing playerjoin to finish");
		cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));
		
	    } else {
		Player player = Bukkit.getServer().getPlayer(uuid);
		if (player != null) {
		    cachedPlayers.put(uuid, new CachedPlayer(groups_loaded, prefix_loaded, suffix_loaded, perms));
		    continueLoadPlayer(player);
		}

	    }
	} catch (SQLException ex) {
	    ex.printStackTrace();
	}
    }

    private void continueLoadPlayer(Player p) {
	debug("continueLoadPlayer " + p.getName());
	CachedPlayer cachedPlayer = cachedPlayers.get(p.getUniqueId());
	if (cachedPlayer == null) {
	    Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Could not continue load player. Cached player is null.");
	    return;
	}
	// Load player permissions.
	PermissionAttachment pa = p.addAttachment(plugin);

	if (players.containsKey(p.getUniqueId())) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    PermissionAttachment toRemove = gp.getPermissionAttachment();
	    if (toRemove != null)
		toRemove.remove();
	    players.remove(p.getUniqueId());
	}

	// Load player groups.
	HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(cachedPlayer.getGroups());
	HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
	for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
	    ArrayList<Group> groupList = new ArrayList<Group>();
	    for (Integer groupId : entry.getValue())
		groupList.add(groups.get(groupId));
	    playerGroups.put(entry.getKey(), groupList);
	}

	PermissionsPlayer permissionsPlayer = new PermissionsPlayer(p, playerGroups, cachedPlayer.getPermissions(), pa, cachedPlayer.getPrefix(), cachedPlayer.getSuffix());
	players.put(p.getUniqueId(), permissionsPlayer);
	cachedPlayers.remove(p.getUniqueId());
    }

    private void loadGroups() {
	/**
	 * Loads groups from MySQL, removes old group data. Will reload all players too.
	 */
	HashMap<Integer, String> tempParents = new HashMap<Integer, String>();
	try {
	    groups.clear();
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblGroups + "");
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

	Set<UUID> keysCopy = new HashSet<UUID>(players.keySet());
	for (UUID uuid : keysCopy) {
	    Player toReload = Bukkit.getPlayer(uuid);
	    if (toReload != null)
		loadPlayer(toReload);
	}
    }

    private ArrayList<PowerfulPermission> loadPlayerPermissions(UUID uuid) {
	PreparedStatement s;
	// boolean needsNameUpdate = false;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playeruuid`=?");
	    s.setString(1, uuid.toString());
	    s.execute();
	    ResultSet result = s.getResultSet();
	    ArrayList<PowerfulPermission> perms = new ArrayList<PowerfulPermission>();
	    while (result.next()) {
		PowerfulPermission tempPerm = new PowerfulPermission(result.getString("permission"), result.getString("world"), result.getString("server"));
		perms.add(tempPerm);

		// if (!p.getName().equals(result.getString("playername")))
		// needsNameUpdate = true;
	    }

	    // Update player names if UUID doesn't match. Allows permission indexing by player name.
	    /*
	     * if (needsNameUpdate) { s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPermissions + " SET `playername`=? WHERE `playeruuid`=?"); s.setString(1, p.getName());
	     * s.setString(2, p.getUniqueId().toString()); s.execute(); Bukkit.getLogger().info(PowerfulPerms.consolePrefix + "Player has changed name, updated UUID and name."); }
	     */

	    return perms;
	} catch (SQLException e) {
	    e.printStackTrace();
	    Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load player permissions.");
	}
	return null;
    }

    private ArrayList<PowerfulPermission> loadPlayerPermissions(String name) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playername`=?");
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
	    Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load player permissions.");
	}
	return null;
    }

    private ArrayList<PowerfulPermission> loadGroupPermissions(String groupName) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `groupname`=?");
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
	    Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Could not load group permissions.");
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

    private HashMap<String, List<Integer>> getPlayerGroupsRaw(String groupsString) {
	HashMap<String, List<Integer>> groups = new HashMap<String, List<Integer>>();
	if (groupsString.contains(";")) {
	    for (String s : groupsString.split(";")) {
		String[] split = s.split(":");
		if (split.length >= 2) {
		    List<Integer> input = groups.get(split[0]);
		    if (input == null)
			input = new ArrayList<Integer>();
		    input.add(Integer.parseInt(split[1]));
		    groups.put(split[0], input);
		} else {
		    List<Integer> input = groups.get("");
		    if (input == null)
			input = new ArrayList<Integer>();
		    input.add(Integer.parseInt(s));
		    groups.put("", input);
		}
	    }
	} else if (!groupsString.isEmpty()) {
	    ArrayList<Integer> tempList = new ArrayList<Integer>();
	    tempList.add(Integer.parseInt(groupsString));
	    groups.put("", tempList);
	}
	return groups;
    }

    public ResultSet getPlayerData(String playerName) {
	PreparedStatement s;
	try {
	    s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet rs = s.getResultSet();
	    if (rs.next())
		return rs;
	    else {
		s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?");
		s.setString(1, "");
		s.setString(2, playerName);
		s.setString(3, "1");
		s.setString(4, "");
		s.setString(5, "");
		s.execute();

		s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
		s.setString(1, playerName);
		s.execute();
		rs = s.getResultSet();
		if (rs.next())
		    return rs;
		Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Player didn't insert into database properly!");
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * Returns the primary group of an online player.
     */
    public Group getPlayerPrimaryGroup(Player p) {
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null)
	    return gp.getPrimaryGroup();
	return null;
    }

    /**
     * Returns the primary group of an offline player.
     */
    public Group getPlayerPrimaryGroup(String playerName) {
	Iterator<Group> it = getPlayerGroups(playerName).get("").iterator();
	return it.next(); // First group is primary group.
    }

    /**
     * Get the full list of groups a player has, if player isn't online it will be loaded from the database.
     */
    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
	Player p = Bukkit.getServer().getPlayer(playerName);
	if (p != null) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    if (gp != null)
		return gp.getServerGroups();
	}
	// Player is not online, load from MySQL
	try {
	    ResultSet result = getPlayerData(playerName);
	    HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(result.getString("groups"));
	    HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
	    for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
		ArrayList<Group> groupList = new ArrayList<Group>();
		for (Integer groupId : entry.getValue())
		    groupList.add(groups.get(groupId));
		playerGroups.put(entry.getKey(), groupList);
	    }
	    return playerGroups;
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * Gets a group from its name.
     * 
     * @param groupName
     *            The name of the group to get.
     */
    public Group getGroup(String groupName) {
	for (Map.Entry<Integer, Group> e : groups.entrySet())
	    if (e.getValue().getName().equalsIgnoreCase(groupName))
		return e.getValue();
	return null;
    }

    /**
     * Get all groups.
     * 
     * @return All groups.
     */
    public Collection<Group> getGroups() {
	return (Collection<Group>) this.groups.values();
    }

    /**
     * Gets a map containing all the permissions a player has, including derived permissions. If player is not online data will be loaded from DB and will not return world-specific or server-specific
     * permissions.
     * 
     * @param p
     *            The player to get permissions from.
     */
    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {

	Player p = Bukkit.getPlayer(playerName);
	if (p != null) {
	    PermissionsPlayer gp = players.get(p.getUniqueId());
	    if (gp != null)
		return gp.getAllPermissions();
	} else {
	    // Load from DB
	    try {
		ArrayList<PowerfulPermission> permissions = loadPlayerPermissions(playerName);

		ResultSet result = getPlayerData(playerName);
		HashMap<String, List<Integer>> playerGroupsRaw = getPlayerGroupsRaw(result.getString("groups"));
		HashMap<String, List<Group>> playerGroups = new HashMap<String, List<Group>>();
		for (Entry<String, List<Integer>> entry : playerGroupsRaw.entrySet()) {
		    ArrayList<Group> groupList = new ArrayList<Group>();
		    for (Integer groupId : entry.getValue())
			groupList.add(groups.get(groupId));
		    playerGroups.put(entry.getKey(), groupList);
		}

		Group group = playerGroups.get("").iterator().next();
		if (group != null) {
		    permissions.addAll(group.getPermissions());
		    return permissions;
		} else {
		    return permissions;
		}

	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	}
	return new ArrayList<PowerfulPermission>();
    }

    /**
     * Checks if player has permission. Works with offline players.
     * 
     * @param playerName
     *            Name of the player.
     * @param permission
     *            The permission string. Can check if permission is negated, "-some.permission"
     * @param server
     *            Check server-specific permission. Leave empty if global permission.
     * @param world
     *            Check world-specific permission. Leave empty if global permission.
     * @return
     */
    public boolean playerHasPermission(String playerName, String permission, String server, String world) {
	if (server.equalsIgnoreCase("ALL"))
	    server = "";

	if (world.equalsIgnoreCase("ALL"))
	    world = "";

	for (PowerfulPermission p : getPlayerPermissions(playerName)) {
	    if (p.getPermissionString().equals(permission)) {
		boolean isSameServer = false;
		boolean isSameWorld = false;

		if (p.getServer().isEmpty() || p.getServer().equalsIgnoreCase("ALL") || p.getServer().equals(server))
		    isSameServer = true;

		if (p.getWorld().isEmpty() || p.getWorld().equalsIgnoreCase("ALL") || p.getWorld().equals(world))
		    isSameWorld = true;
		if (isSameServer && isSameWorld)
		    return true;
	    }
	}
	return false;
    }

    /**
     * Checks if group has permission.
     * 
     * @param groupName
     *            Name of the group.
     * @param permission
     *            The permission string. Can check if permission is negated, "-some.permission"
     * @param server
     *            Check server-specific permission. Leave empty if global permission.
     * @param world
     *            Check world-specific permission. Leave empty if global permission.
     * @return
     */
    public boolean groupHasPermission(String groupName, String permission, String server, String world) {
	if (server.equalsIgnoreCase("ALL"))
	    server = "";

	if (world.equalsIgnoreCase("ALL"))
	    world = "";

	Group group = getGroup(groupName);
	if (group != null) {

	    for (PowerfulPermission p : group.getPermissions()) {
		if (p.getPermissionString().equals(permission)) {
		    boolean isSameServer = false;
		    boolean isSameWorld = false;

		    if (p.getServer().isEmpty() || p.getServer().equalsIgnoreCase("ALL") || p.getServer().equals(server))
			isSameServer = true;

		    if (p.getWorld().isEmpty() || p.getWorld().equalsIgnoreCase("ALL") || p.getWorld().equals(world))
			isSameWorld = true;
		    if (isSameServer && isSameWorld)
			return true;
		}
	    }
	}
	return false;
    }

    /**
     * Gets the prefix of a player. If the player doesn't have a prefix, return the top inherited group's prefix.
     * 
     * @param p
     *            The player to get prefix from.
     */
    public String getPlayerPrefix(Player p) {
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null) {
	    String prefix = gp.getPrefix();
	    return prefix;
	}
	Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Attempted to get prefix of a non-loaded player");
	return null;
    }

    /**
     * Gets the prefix of a player. Non-inherited.
     * 
     * @param p
     *            The player to get prefix from.
     */
    public String getPlayerPrefix(String playerName) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		return result.getString("prefix");
	    } else
		Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Attempted to get prefix of a player that doesn't exist.");
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return "";
    }

    /**
     * Gets the suffix of a player. If the player doesn't have a suffix, return the top inherited group's suffix.
     * 
     * @param p
     *            The player to get suffix from.
     */
    public String getPlayerSuffix(Player p) {
	PermissionsPlayer gp = players.get(p.getUniqueId());
	if (gp != null) {
	    String suffix = gp.getSuffix();
	    return suffix;
	}
	Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Attempted to get suffix of a non-loaded player");
	return null;
    }

    /**
     * Gets the suffix of a player. Non-inherited.
     * 
     * @param p
     *            The player to get suffix from.
     */
    public String getPlayerSuffix(String playerName) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		return result.getString("suffix");
	    } else
		Bukkit.getLogger().severe(PowerfulPerms.consolePrefix + "Attempted to get suffix of a player that doesn't exist.");
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return "";
    }

    /**
     * Gets the prefix of a group.
     * 
     * @param groupName
     *            The group to get prefix from.
     */
    public String getGroupPrefix(String groupName) {
	Group g = getGroup(groupName);
	if (g != null)
	    return g.getPrefix();
	return "";
    }

    /**
     * Gets the suffix of a group.
     * 
     * @param groupName
     *            The group to get suffix from.
     */
    public String getGroupSuffix(String groupName) {
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

    public PMR addPlayerPermission(String playerName, String permission) {
	return addPlayerPermission(playerName, permission, "", "");
    }

    public PMR addPlayerPermission(String playerName, String permission, String world, String server) {
	try {
	    if(playerName.equalsIgnoreCase("[default]"))
		return new PMR(false, "You can't add permissions to the default player. Add them to a group instead and add the group to the default player.");
	    
	    // Check if the same permission already exists.
	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPermissions + " WHERE `playername`=? AND `permission`=? AND `world`=? AND `server`=?");
	    s.setString(1, playerName);
	    s.setString(2, permission);
	    s.setString(3, world);
	    s.setString(4, server);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next()) {
		return new PMR(false, "Player already has the specified permission.");
	    }

	    UUID uuid = null;
	    // Get UUID from table players. Player has to exist.
	    s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    result = s.getResultSet();
	    if (result.next()) {
		uuid = UUID.fromString(result.getString("uuid"));
	    }

	    s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
	    if (uuid != null)
		s.setString(1, uuid.toString());
	    else
		return new PMR(false, "Could not add permission. Player doesn't exist.");
	    s.setString(2, playerName);
	    s.setString(3, "");
	    s.setString(4, permission);
	    s.setString(5, world);
	    s.setString(6, server);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);
	    notifyReloadPlayer(playerName);
	    return new PMR("Permission added to player.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR removePlayerPermission(String playerName, String permission) {
	return removePlayerPermission(playerName, permission, "", "");
    }

    public PMR removePlayerPermission(String playerName, String permission, String world, String server) {
	try {
	    boolean useWorld = false;
	    boolean useServer = false;

	    String statement = "DELETE FROM `" + PowerfulPerms.tblPermissions + "` WHERE `playername`=? AND `permission`=?";
	    if (!world.isEmpty() && !world.equalsIgnoreCase("ALL")) {
		statement += ", `world`=?";
		useWorld = true;
	    }
	    if (!server.isEmpty() && !server.equalsIgnoreCase("ALL")) {
		statement += ", `server`=?";
		useServer = true;
	    }
	    PreparedStatement s = sql.getConnection().prepareStatement(statement);

	    s.setString(1, playerName);
	    s.setString(2, permission);
	    if (useWorld)
		s.setString(3, world);
	    if (useServer)
		s.setString(4, server);
	    int amount = s.executeUpdate();
	    if (amount <= 0)
		return new PMR(false, "Player does not have the specified permission.");

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
	    return new PMR("Removed " + amount + " permissions from the player.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setPlayerPrefix(String playerName, String prefix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `prefix`=? WHERE `name`=?");
	    s.setString(1, prefix);
	    s.setString(2, playerName);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
	    return new PMR("Player prefix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setPlayerSuffix(String playerName, String suffix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `suffix`=? WHERE `name`=?");
	    s.setString(1, suffix);
	    s.setString(2, playerName);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
	    return new PMR("Player suffix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setPlayerPrimaryGroup(String playerName, String groupName) {
	Group group = getGroup(groupName);
	if (group == null)
	    return new PMR(false, "Group does not exist.");

	try {
	    String playerGroupString = "";

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next())
		playerGroupString = result.getString("groups");
	    else
		return new PMR(false, "Player does not exist.");

	    // Add group. Put it first.
	    HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);
	    List<Integer> groupList = playerGroups.get("");
	    if (groupList == null)
		groupList = new ArrayList<Integer>();

	    // Remove existing primary
	    Iterator<Integer> it = groupList.iterator();
	    if (it.hasNext()) {
		it.next();
		it.remove();
	    }

	    ArrayList<Integer> newList = new ArrayList<Integer>();
	    newList.add(group.getId());
	    newList.addAll(groupList);
	    playerGroups.put("", newList);

	    String playerGroupStringOutput = "";
	    for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
		for (Integer groupId : entry.getValue())
		    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
	    }

	    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `groups`=? WHERE `name`=?");
	    s.setString(1, playerGroupStringOutput);
	    s.setString(2, playerName);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
	    return new PMR("Player primary group set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}

    }

    public PMR removePlayerGroup(String playerName, String groupName) {
	return removePlayerGroup(playerName, groupName, "");
    }

    public PMR removePlayerGroup(String playerName, String groupName, String server) {
	if (server.equalsIgnoreCase("all"))
	    server = "";

	int groupId_new;
	Group group = getGroup(groupName);
	if (group != null)
	    groupId_new = group.getId();
	else
	    return new PMR(false, "Group does not exist.");
	try {
	    String playerGroupString = "";

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next())
		playerGroupString = result.getString("groups");
	    else
		return new PMR(false, "Player does not exist.");

	    boolean removed = false;
	    HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);

	    List<Integer> groupList = playerGroups.get(server);
	    if (groupList != null) {
		Iterator<Integer> it = groupList.iterator();
		while (it.hasNext()) {
		    int groupId = it.next();
		    if (groupId == groupId_new) {
			if (getPlayerPrimaryGroup(playerName).getId() == groupId && server.isEmpty())
			    return new PMR(false, "Can't remove player primary group.");
			it.remove();
			removed = true;
		    }
		}
	    }

	    if (removed)
		playerGroups.put(server, groupList);
	    else
		return new PMR(false, "Player does not have a specific group for the specified server.");

	    String playerGroupStringOutput = "";
	    for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
		for (Integer groupId : entry.getValue())
		    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
	    }

	    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `groups`=? WHERE `name`=?");
	    s.setString(1, playerGroupStringOutput);
	    s.setString(2, playerName);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
	    return new PMR("Player group removed.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}

    }

    public PMR addPlayerGroup(String playerName, String groupName) {
	return addPlayerGroup(playerName, groupName, "");
    }

    public PMR addPlayerGroup(String playerName, String groupName, String server) {
	if (server.equalsIgnoreCase("all"))
	    server = "";

	Group group = getGroup(groupName);
	if (group == null)
	    return new PMR(false, "Group does not exist.");

	try {
	    String playerGroupString = "";

	    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblPlayers + " WHERE `name`=?");
	    s.setString(1, playerName);
	    s.execute();
	    ResultSet result = s.getResultSet();
	    if (result.next())
		playerGroupString = result.getString("groups");
	    else
		return new PMR(false, "Player does not exist.");

	    // Add group. Put it first.
	    HashMap<String, List<Integer>> playerGroups = getPlayerGroupsRaw(playerGroupString);
	    List<Integer> groupList = playerGroups.get(server);
	    if (groupList == null)
		groupList = new ArrayList<Integer>();
	    if (groupList.contains(group.getId()))
		return new PMR(false, "Player already has this group.");
	    groupList.add(group.getId());
	    playerGroups.put(server, groupList);

	    String playerGroupStringOutput = "";
	    for (Entry<String, List<Integer>> entry : playerGroups.entrySet()) {
		for (Integer groupId : entry.getValue())
		    playerGroupStringOutput += entry.getKey() + ":" + groupId + ";";
	    }

	    s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblPlayers + " SET `groups`=? WHERE `name`=?");
	    s.setString(1, playerGroupStringOutput);
	    s.setString(2, playerName);
	    s.execute();

	    Player player = Bukkit.getPlayer(playerName);
	    if (player != null)
		loadPlayer(player);

	    notifyReloadPlayer(playerName);
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

    public PMR createGroup(String name) {
	Iterator<Entry<Integer, Group>> it = this.groups.entrySet().iterator();
	while (it.hasNext()) {
	    Entry<Integer, Group> e = it.next();
	    if (e.getValue().getName().equalsIgnoreCase(name)) {
		// Group already exists
		return new PMR(false, "Group already exists.");
	    }
	}

	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + PowerfulPerms.tblGroups + " SET `name`=?, `parents`=?, `prefix`=?, `suffix`=?");
	    s.setString(1, name);
	    s.setString(2, "");
	    s.setString(3, "");
	    s.setString(4, "");
	    s.execute();
	    // Reload groups
	    loadGroups();
	    notifyReloadGroups();
	    return new PMR("Created group.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "Could not create group: SQL error code: " + e.getErrorCode());
	}
    }

    public PMR deleteGroup(String groupName) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + PowerfulPerms.tblGroups + " WHERE `name`=?;");
	    s.setString(1, groupName);
	    s.execute();
	    // Reload groups
	    loadGroups();
	    notifyReloadGroups();
	    return new PMR("Deleted group.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "Could not delete group: SQL error code: " + e.getErrorCode());
	}
    }

    public PMR addGroupPermission(String groupName, String permission) {
	return addGroupPermission(groupName, permission, "", "");
    }

    public PMR addGroupPermission(String groupName, String permission, String world, String server) {
	Group group = getGroup(groupName);
	if (group != null) {
	    ArrayList<PowerfulPermission> groupPermissions = group.getOwnPermissions();
	    try {
		PowerfulPermission sp = new PowerfulPermission(permission, world, server);

		for (PowerfulPermission temp : groupPermissions) {
		    if (temp.getPermissionString().equals(permission) && temp.getServer().equals(server) && temp.getWorld().equals(world))
			return new PMR(false, "Group already has the specified permission.");
		}

		groupPermissions.add(sp);

		PreparedStatement s = sql.getConnection().prepareStatement(
			"INSERT INTO " + PowerfulPerms.tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
		s.setString(1, "");
		s.setString(2, "");
		s.setString(3, groupName);
		s.setString(4, permission);
		s.setString(5, world);
		s.setString(6, server);
		s.execute();

		// Reload groups
		loadGroups();
		notifyReloadGroups();
		return new PMR("Added permission to group.");
	    } catch (SQLException e) {
		e.printStackTrace();
		return new PMR(false, "SQL error code: " + e.getErrorCode());
	    }
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR removeGroupPermission(String groupName, String permission) {
	return removeGroupPermission(groupName, permission, "", "");
    }

    public PMR removeGroupPermission(String groupName, String permission, String world, String server) {
	// boolean allServers = server == null || server.isEmpty() || server.equals("ALL");
	Group group = getGroup(groupName);
	if (group != null) {
	    ArrayList<PowerfulPermission> removed = new ArrayList<PowerfulPermission>();
	    ArrayList<PowerfulPermission> groupPermissions = group.getOwnPermissions();
	    Iterator<PowerfulPermission> it = groupPermissions.iterator();
	    while (it.hasNext()) {
		PowerfulPermission current = it.next();
		if (current.getPermissionString().equalsIgnoreCase(permission)) {
		    if (world.equals(current.getWorld()) && server.equals(current.getServer())) {
			removed.add(current);
			it.remove();
		    }
		}
	    }

	    try {
		if (removed.size() <= 0)
		    return new PMR(false, "Group does not have the specified permission.");

		int amount = 0;
		for (PowerfulPermission current : removed) {
		    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + PowerfulPerms.tblPermissions + " WHERE `groupName`=? AND `permission`=? AND `world`=? AND `server`=?");
		    s.setString(1, groupName);
		    s.setString(2, current.getPermissionString());
		    s.setString(3, current.getWorld());
		    s.setString(4, current.getServer());
		    amount += s.executeUpdate();
		}

		// Reload groups
		loadGroups();
		notifyReloadGroups();
		return new PMR("Removed " + amount + " permissions from the group.");
	    } catch (SQLException e) {
		e.printStackTrace();
		return new PMR(false, "SQL error code: " + e.getErrorCode());
	    }
	} else
	    return new PMR(false, "Group does not exist.");
    }

    public PMR addGroupParent(String groupName, String parentGroupName) {
	Group group = getGroup(groupName);
	if (group != null) {
	    Group parentGroup = getGroup(parentGroupName);
	    if (parentGroup != null) {
		String currentParents = group.getRawOwnParents();
		currentParents += parentGroup.getId() + ";";
		try {
		    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblGroups + " SET `parents`=? WHERE `name`=?");
		    s.setString(1, currentParents);
		    s.setString(2, groupName);
		    s.execute();
		    // Reload groups
		    loadGroups();
		    notifyReloadGroups();
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

    public PMR removeGroupParent(String groupName, String parentGroupName) {
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
		    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblGroups + " SET `parents`=? WHERE `name`=?");
		    s.setString(1, currentParents);
		    s.setString(2, groupName);
		    s.execute();
		    // Reload groups
		    loadGroups();
		    notifyReloadGroups();
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
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblGroups + " SET `prefix`=? WHERE `name`=?");
	    s.setString(1, prefix);
	    s.setString(2, groupName);
	    s.execute();
	    // Reload groups
	    loadGroups();
	    notifyReloadGroups();
	    return new PMR("Group prefix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

    public PMR setGroupSuffix(String groupName, String suffix) {
	try {
	    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + PowerfulPerms.tblGroups + " SET `suffix`=? WHERE `name`=?");
	    s.setString(1, suffix);
	    s.setString(2, groupName);
	    s.execute();
	    // Reload groups
	    loadGroups();
	    notifyReloadGroups();
	    return new PMR("Group suffix set.");
	} catch (SQLException e) {
	    e.printStackTrace();
	    return new PMR(false, "SQL error code " + e.getErrorCode());
	}
    }

}