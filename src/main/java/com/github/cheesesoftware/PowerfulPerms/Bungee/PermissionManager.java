package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import org.apache.commons.pool2.impl.*;

import com.github.cheesesoftware.PowerfulPerms.Group;
import com.github.cheesesoftware.PowerfulPerms.PowerfulPermission;
import com.github.cheesesoftware.PowerfulPerms.SQL;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class PermissionManager {

    private HashMap<Integer, Group> groups = new HashMap<Integer, Group>();
    private SQL sql;

    private JedisPool pool;
    private JedisPubSub subscriber;

    public PermissionManager(SQL sql, Plugin plugin) {
        this.sql = sql;

        // Initialize Redis
        if (PowerfulPerms.redis_password == null || PowerfulPerms.redis_password.isEmpty())
            pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0);
        else
            pool = new JedisPool(new GenericObjectPoolConfig(), PowerfulPerms.redis_ip, PowerfulPerms.redis_port, 0, PowerfulPerms.redis_password);
        final Plugin tempPlugin = plugin;
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @SuppressWarnings("deprecation")
            public void run() {
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    subscriber = (new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, final String msg) {
                            tempPlugin.getProxy().getScheduler().runAsync(tempPlugin, new Runnable() {
                                public void run() {
                                    // Reload player or groups depending on msg
                                    String[] split = msg.split(" ");
                                    if (split.length == 2) {
                                        String first = split[0];

                                        if (first.equals("[groups]")) {
                                            loadGroups();
                                            tempPlugin.getLogger().info(PowerfulPerms.pluginPrefix + "Reloaded all groups.");
                                        }
                                        /*
                                         * else if(first.equals("[requestservername]")) { ProxyServer.getInstance().getServers().get("lol").getAddress(). }
                                         */
                                    }
                                }
                            });
                        }
                    });
                    jedis.subscribe(subscriber, "PowerfulPerms");
                } catch (Exception e) {
                    pool.returnBrokenResource(jedis);
                    tempPlugin.getLogger().warning(
                            PowerfulPerms.pluginPrefix + "Unable to connect to Redis server. Check your credentials in the config file. If you don't use Redis, this message is perfectly fine.");
                    return;
                }
                pool.returnResource(jedis);
            }
        });

        try {
            loadGroups();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onDisable() {
        if (subscriber != null)
            subscriber.unsubscribe();
        if (pool != null)
            pool.destroy();
        groups.clear();
    }

    public void reloadGroups() {
        groups.clear();
        loadGroups();
    }

    private void loadGroups() {
        /**
         * Loads groups from MySQL, removes old group data. Will reload all players too.
         */
        HashMap<Integer, String> tempParents = new HashMap<Integer, String>();
        try {
            groups.clear();
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + PowerfulPerms.tblGroups);
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
            ProxyServer.getInstance().getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load player permissions.");
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
            ProxyServer.getInstance().getLogger().severe(PowerfulPerms.pluginPrefix + "Could not load group permissions.");
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
                ProxyServer.getInstance().getLogger().severe(PowerfulPerms.pluginPrefix + "Player didn't insert into database properly!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Group getPlayerPrimaryGroup(String playerName) {
        Iterator<Group> it = getPlayerGroups(playerName).get("").iterator();
        return it.next(); // First group is primary group.
    }

    public HashMap<String, List<Group>> getPlayerGroups(String playerName) {
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
     * Gets a map containing all the permissions a player has, including its group's permissions and its group's parent groups' permissions. If player is not online data will be loaded from DB.
     * 
     * @param p
     *            The player to get permissions from.
     */
    public ArrayList<PowerfulPermission> getPlayerPermissions(String playerName) {
        ArrayList<PowerfulPermission> permissions = loadPlayerPermissions(playerName);

        Group group = getPlayerPrimaryGroup(playerName);
        if (group != null) {
            permissions.addAll(group.getPermissions());
            return permissions;
        } else
            return permissions;
    }

    public boolean getPlayerHasPermission(ProxiedPlayer player, String permission) {
        ArrayList<PowerfulPermission> perms = getPlayerPermissions(player.getName());
        for (PowerfulPermission toCheck : perms) {
            if (permission.equalsIgnoreCase(toCheck.getPermissionString())) {
                if (toCheck.getServer().equalsIgnoreCase("all") || toCheck.getServer().isEmpty() || player.getServer().getInfo().getName().equalsIgnoreCase(toCheck.getServer())) {
                    if (toCheck.getWorld().equalsIgnoreCase(("all")) || toCheck.getWorld().isEmpty())
                        return true;
                }
            }
        }
        return false;
    }

}