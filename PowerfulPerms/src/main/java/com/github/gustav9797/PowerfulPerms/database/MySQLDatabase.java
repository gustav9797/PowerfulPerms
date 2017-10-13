package com.github.gustav9797.PowerfulPerms.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.TreeMap;
import java.util.UUID;

import com.github.gustav9797.PowerfulPerms.common.PermissionManagerBase;
import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.DBDocument;
import com.github.gustav9797.PowerfulPermsAPI.IScheduler;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.base.Charsets;

public class MySQLDatabase extends Database {

    private PowerfulPermsPlugin plugin;
    private SQL sql;
    private Map<String, String> tables = new HashMap<>();

    public MySQLDatabase(IScheduler scheduler, DatabaseCredentials cred, PowerfulPermsPlugin plugin, String tablePrefix) {
        super(scheduler, tablePrefix);
        this.plugin = plugin;

        this.sql = new SQL(cred.getHost(), cred.getDatabase(), cred.getPort(), cred.getUsername(), cred.getPassword());
        try {
            if (sql.getConnection() == null || sql.getConnection().isClosed()) {
                plugin.getLogger().severe("Could not connect to the database!");
                return;
            }
        } catch (SQLException e2) {
            e2.printStackTrace();
            plugin.getLogger().severe("Could not connect to the database!");
            return;
        }

        tables.put(tblGroupParents, "CREATE TABLE `" + tblGroupParents + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `groupid` int(10) unsigned NOT NULL,\r\n"
                + "  `parentgroupid` int(10) unsigned NOT NULL,\r\n" + "  PRIMARY KEY (`id`),\r\n" + "  UNIQUE KEY `id_UNIQUE` (`id`)\r\n" + ") ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;");

        tables.put(tblGroupPermissions,
                "CREATE TABLE `" + tblGroupPermissions + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `groupid` int(10) unsigned NOT NULL,\r\n"
                        + "  `permission` varchar(128) NOT NULL,\r\n" + "  `world` varchar(128) NOT NULL,\r\n" + "  `server` varchar(128) NOT NULL,\r\n" + "  `expires` datetime DEFAULT NULL,\r\n"
                        + "  PRIMARY KEY (`id`)\r\n" + ") ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblGroupPrefixes,
                "CREATE TABLE `" + tblGroupPrefixes + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `groupid` int(10) unsigned NOT NULL,\r\n"
                        + "  `prefix` text NOT NULL,\r\n" + "  `server` text NOT NULL,\r\n" + "  PRIMARY KEY (`id`),\r\n" + "  UNIQUE KEY `id_UNIQUE` (`id`)\r\n"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblGroups,
                "CREATE TABLE `" + tblGroups + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `name` varchar(255) NOT NULL,\r\n" + "  `ladder` varchar(64) NOT NULL,\r\n"
                        + "  `rank` int(11) NOT NULL,\r\n" + "  PRIMARY KEY (`id`),\r\n" + "  UNIQUE KEY `id_UNIQUE` (`id`),\r\n" + "  UNIQUE KEY `name_UNIQUE` (`name`)\r\n"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblGroupSuffixes,
                "CREATE TABLE `" + tblGroupSuffixes + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `groupid` int(10) unsigned NOT NULL,\r\n"
                        + "  `suffix` text NOT NULL,\r\n" + "  `server` text NOT NULL,\r\n" + "  PRIMARY KEY (`id`),\r\n" + "  UNIQUE KEY `id_UNIQUE` (`id`)\r\n"
                        + ") ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblPlayerGroups,
                "CREATE TABLE `" + tblPlayerGroups + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `playeruuid` varchar(36) NOT NULL,\r\n"
                        + "  `groupid` int(10) unsigned NOT NULL,\r\n" + "  `server` text NOT NULL,\r\n" + "  `negated` tinyint(1) NOT NULL DEFAULT '0',\r\n" + "  `expires` datetime DEFAULT NULL,\r\n"
                        + "  PRIMARY KEY (`id`),\r\n" + "  UNIQUE KEY `id_UNIQUE` (`id`)\r\n" + ") ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblPlayerPermissions,
                "CREATE TABLE `" + tblPlayerPermissions + "` (\r\n" + "  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,\r\n" + "  `playeruuid` varchar(36) NOT NULL,\r\n"
                        + "  `permission` varchar(128) NOT NULL,\r\n" + "  `world` varchar(128) NOT NULL,\r\n" + "  `server` varchar(128) NOT NULL,\r\n" + "  `expires` datetime DEFAULT NULL,\r\n"
                        + "  PRIMARY KEY (`id`)\r\n" + ") ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;\r\n");

        tables.put(tblPlayers, "CREATE TABLE `" + tblPlayers + "` (\r\n" + "  `uuid` varchar(36) NOT NULL DEFAULT '',\r\n" + "  `name` varchar(32) NOT NULL,\r\n" + "  `prefix` text NOT NULL,\r\n"
                + "  `suffix` text NOT NULL,\r\n" + "  PRIMARY KEY (`uuid`,`name`),\r\n" + "  UNIQUE KEY `uuid_UNIQUE` (`uuid`)\r\n" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;\r\n");
    }

    private DBResult fromResultSet(ResultSet r) throws SQLException {
        ArrayList<DBDocument> rows = new ArrayList<>();
        ResultSetMetaData md = r.getMetaData();
        int columns = md.getColumnCount();
        while (r.next()) {
            Map<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), r.getObject(i));
            }
            rows.add(new DBDocument(row));
        }
        return new DBResult(rows);
    }

    private String getExpirationDateString(Date expires) {
        if (expires == null)
            return null;
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = dateFormat.format(expires);
        if (format == null || format.equals("null"))
            return null;
        return format;
    }

    @Override
    public void applyPatches() {
        if (plugin.getOldVersion() < 233) {
            // Set [default] UUID
            final PowerfulPermsPlugin pl = plugin;
            setPlayerUUID("[default]", java.util.UUID.nameUUIDFromBytes(("[default]").getBytes(Charsets.UTF_8)));
            pl.getLogger().info("Applied database patch #1: Inserted UUID for player [default].");
        }

        if (plugin.getOldVersion() < 240) {
            // Add "ladder" and "rank" columns to groups table
            try {

                PreparedStatement s = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `" + tblGroups + "` LIKE 'ladder';");
                ResultSet result = s.executeQuery();
                if (!result.next()) {
                    s.close();
                    s = sql.getConnection().prepareStatement("ALTER TABLE `" + tblGroups + "` ADD COLUMN `ladder` VARCHAR(64) NOT NULL AFTER `suffix`,ADD COLUMN `rank` INT NOT NULL AFTER `ladder`");
                    s.execute();
                    s.close();
                    plugin.getLogger().info("Applied database patch #2: Added columns 'ladder' and 'rank' to groups table.");
                } else {
                    plugin.getLogger().info("Skipping database patch #2.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (plugin.getOldVersion() < 400 && !tableExists(tblGroupParents) && !tableExists(
            tblGroupPermissions) && !tableExists(tblGroupPrefixes)
                && !tableExists(tblGroupSuffixes) && !tableExists(tblPlayerGroups) && !tableExists(
            tblPlayerPermissions)) {
            plugin.getLogger().warning("PowerfulPerms has detected that you are upgrading from a version earlier than " + plugin.getVersion() + " and that your database isn't up to date.");
            plugin.getLogger().warning("PowerfulPerms " + plugin.getVersion() + " will NOT work with a database from version 2.X.X or 3.X.X.");
            plugin.getLogger().info("Updating your database...");

            String tblPermsTemp = prefix + "permissions";
            String tblGroupsTemp = prefix + "groups";
            String tblPlayersTemp = prefix + "players";
            if (!tableExists(tblPermsTemp) | !tableExists(tblGroupsTemp) || !tableExists(tblPlayersTemp)) {
                plugin.getLogger().severe("The required tables do not exist.");
                plugin.getLogger().severe("Server will continue booting in 10 seconds.");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                renameTable(tblPermsTemp, tblPermsTemp + "_old");
                tblPermsTemp = tblPermsTemp + "_old";

                renameTable(tblGroupsTemp, tblGroupsTemp + "_old");
                tblGroupsTemp = tblGroupsTemp + "_old";

                renameTable(tblPlayersTemp, tblPlayersTemp + "_old");
                tblPlayersTemp = tblPlayersTemp + "_old";

                for (String table : tables.keySet())
                    createTable(table);

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayersTemp);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    DBResult result = fromResultSet(r);
                    while (result.hasNext()) {
                        DBDocument current = result.next();
                        final UUID uuid = UUID.fromString(current.getString("uuid"));
                        String name = current.getString("name");
                        String groupsRaw = current.getString("groups");
                        String prefix = current.getString("prefix");
                        String suffix = current.getString("suffix");
                        this.insertPlayer(uuid, name, prefix, suffix);
                        plugin.getLogger().info("Inserted player " + uuid.toString());
                        LinkedHashMap<String, List<CachedGroup>> tempGroups = Util.getPlayerGroups_old(groupsRaw);
                        for (Entry<String, List<CachedGroup>> e : tempGroups.entrySet()) {
                            String server = e.getKey();
                            for (final CachedGroup cachedGroup : e.getValue()) {
                                this.insertPlayerGroup(uuid, cachedGroup.getGroupId(), server, cachedGroup.isNegated(), null);
                                plugin.getLogger().info("Inserted player group " + cachedGroup.getGroupId() + " for player " + uuid.toString());
                            }
                        }

                    }
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                Map<String, Integer> groupIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupsTemp);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    DBResult result = fromResultSet(r);
                    while (result.hasNext()) {
                        DBDocument current = result.next();
                        final int id = current.getInt("id");
                        String name = current.getString("name");
                        String parentsRaw = current.getString("parents");
                        String prefixRaw = current.getString("prefix");
                        String suffixRaw = current.getString("suffix");
                        String ladder = current.getString("ladder");
                        int rank = current.getInt("rank");
                        groupIds.put(name, id);

                        this.insertGroup(id, name, ladder, rank);
                        plugin.getLogger().info("Inserted group " + id);

                        HashMap<String, String> prefixes = Util.getPrefixSuffix_old(prefixRaw);
                        for (final Entry<String, String> e : prefixes.entrySet()) {
                            this.insertGroupPrefix(id, e.getValue(), e.getKey());
                            plugin.getLogger().info("Inserted group prefix " + e.getKey() + ":" + e.getValue() + " for group " + id);
                        }

                        HashMap<String, String> suffixes = Util.getPrefixSuffix_old(suffixRaw);
                        for (final Entry<String, String> e : suffixes.entrySet()) {
                            this.insertGroupSuffix(id, e.getValue(), e.getKey());
                            plugin.getLogger().info("Inserted group suffix " + e.getKey() + ":" + e.getValue() + " for group " + id);
                        }

                        ArrayList<String> parents = Util.getGroupParents_old(parentsRaw);
                        for (String parentName : parents) {
                            try {
                                final int parentId = Integer.parseInt(parentName);
                                this.insertGroupParent(id, parentId);
                                plugin.getLogger().info("Inserted group parent " + parentId + " for group " + id);
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Couldn't add group parent " + parentName + " to group " + id);
                            }
                        }

                    }
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermsTemp);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    DBResult result = fromResultSet(r);
                    while (result.hasNext()) {
                        DBDocument current = result.next();

                        String uuidTemp = current.getString("playeruuid");
                        UUID tempUUID = null;
                        try {
                            if (uuidTemp != null && !uuidTemp.isEmpty()) {
                                tempUUID = UUID.fromString(uuidTemp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        final UUID uuid = tempUUID;
                        final String playername = current.getString("playername");
                        final String groupname = current.getString("groupname");
                        final String permission = current.getString("permission");
                        final String world = current.getString("world");
                        final String server = current.getString("server");

                        if (groupname != null && !groupname.isEmpty()) {
                            Integer groupId = groupIds.get(groupname);
                            if (groupId != null) {
                                this.insertGroupPermission(groupId, permission, world, server, null);
                                plugin.getLogger().info("Inserted permission " + permission + " for group " + groupname);
                            } else
                                plugin.getLogger().warning("Couldn't add group permission " + permission + " to group " + groupname);
                        } else if (uuid != null || playername != null) {
                            if (uuid == null) {
                                UUID resultUUID = ((PermissionManagerBase) plugin.getPermissionManager()).getConvertUUIDBase(playername);
                                try {
                                    if (resultUUID != null) {
                                        insertPlayerPermission(resultUUID, permission, world, server, null);
                                        plugin.getLogger().info("Inserted permission " + permission + " for player " + playername);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                this.insertPlayerPermission(uuid, permission, world, server, null);
                                plugin.getLogger().info("Inserted permission " + permission + " for player " + playername);
                            }
                        }
                    }
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                plugin.getLogger().info("Done.");
                plugin.getLogger().info("Your database has been upgraded to PowerfulPerms 4.0.0.");
                plugin.getLogger().info("The old tables are still there but renamed. Keep them until you are sure everything works fine!");

            }
        }
    }

    @Override
    public boolean ping() {
        try {
            Connection connection = sql.getConnection();
            if (connection != null && connection.isValid(10))
                return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tableExists(final String table) {
        boolean exists = false;
        try {

            DatabaseMetaData dbm = sql.getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, table, null);
            if (tables.next())
                exists = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exists;
    }

    @Override
    public void createTable(String tableName) {
        String table = tables.get(tableName);
        try {
            PreparedStatement s = sql.getConnection().prepareStatement(table);
            s.execute();
            s.close();
            plugin.getLogger().info("Created table " + tableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renameTable(String tableName, String newTableName) {
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("RENAME TABLE `" + tableName + "` TO `" + newTableName + "`");
            s.execute();
            s.close();
            plugin.getLogger().info("Renamed table " + tableName + " to " + newTableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean insertGroup(final int id, final String group, final String ladder, final int rank) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroups + " SET " + (id != -1 ? "`id`=?, " : "") + "`name`=?, `ladder`=?, `rank`=?");
            if (id != -1) {
                s.setInt(1, id);
                s.setString(2, group);
                s.setString(3, ladder);
                s.setInt(4, rank);
            } else {
                s.setString(1, group);
                s.setString(2, ladder);
                s.setInt(3, rank);
            }
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean insertPlayer(final UUID uuid, final String name, final String prefix, final String suffix) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblPlayers + " SET `uuid`=?, `name`=?, `prefix`=?, `suffix`=?;");
            s.setString(1, uuid.toString());
            s.setString(2, name);
            s.setString(3, prefix);
            s.setString(4, suffix);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getPlayer(final UUID uuid) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `uuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }

        return result;
    }

    @Override
    public DBResult deletePlayer(final UUID uuid) {
        boolean success = true;
        int amount = 0;

        plugin.getLogger().info("Deleting player " + uuid + "...");
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM `" + tblPlayers + "` WHERE `uuid`=?");
            s.setString(1, uuid.toString());
            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        plugin.getLogger().info("Deleting player permissions...");
        this.deletePlayerPermissions(uuid);
        plugin.getLogger().info("Deleting player groups...");
        this.deletePlayerGroups(uuid);
        return new DBResult(success, amount);
    }

    @Override
    public DBResult getPlayersInGroup(int groupId, int limit, int offset) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayerGroups + " INNER JOIN " + tblPlayers + " ON " + tblPlayers + ".uuid=" + tblPlayerGroups
                    + ".playeruuid WHERE `groupid`=? AND `negated`=? LIMIT ? OFFSET ?");
            s.setInt(1, groupId);
            s.setInt(2, 0);
            s.setInt(3, limit);
            s.setInt(4, offset);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getPlayers(final String name) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE BINARY `name`=?");
            s.setString(1, name);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getPlayersCaseInsensitive(final String name) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
            s.setString(1, name);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean setPlayerName(final UUID uuid, final String name) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `name`=? WHERE `uuid`=?;");
            s.setString(1, name);
            s.setString(2, uuid.toString());
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean setPlayerUUID(final String name, final UUID uuid) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `uuid`=? WHERE `name`=?;");
            s.setString(1, uuid.toString());
            s.setString(2, name);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getGroups() {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroups);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getGroupPermissions(final int groupId) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupPermissions + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getGroupPermissions() {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupPermissions);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getPlayerPermissions(final UUID uuid) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayerPermissions + " WHERE `playeruuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean playerHasPermission(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        boolean success = false;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement(
                    "SELECT * FROM " + tblPlayerPermissions + " WHERE `playeruuid`=? AND `permission`=? AND `world`=? AND `server`=? AND " + (expires == null ? "`expires` is NULL" : "`expires`=?"));
            s.setString(1, uuid.toString());
            s.setString(2, permission);
            s.setString(3, world);
            s.setString(4, server);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            s.execute();
            ResultSet result = s.getResultSet();
            if (result.next()) {
                success = true;
            }
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    @Override
    public boolean insertPlayerPermission(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection()
                    .prepareStatement("INSERT INTO " + tblPlayerPermissions + " SET `playeruuid`=?, `permission`=?, `world`=?, `server`=?" + (expires != null ? ", `expires`=?" : ""));
            s.setString(1, uuid.toString());
            s.setString(2, permission);
            s.setString(3, world);
            s.setString(4, server);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult deletePlayerPermission(final UUID uuid, final String permission, final String world, final String server, final Date expires) {
        boolean success = true;
        int amount = 0;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement(
                    "DELETE FROM `" + tblPlayerPermissions + "` WHERE `playeruuid`=? AND `permission`=? AND `server`=? AND `world`=? AND " + (expires == null ? "`expires` is NULL" : "`expires`=?"));

            s.setString(1, uuid.toString());
            s.setString(2, permission);
            s.setString(3, server);
            s.setString(4, world);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));

            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return new DBResult(success, amount);
    }

    @Override
    public DBResult deletePlayerPermissions(final UUID uuid) {
        boolean success = true;
        int amount = 0;
        try {
            String statement = "DELETE FROM `" + tblPlayerPermissions + "` WHERE `playeruuid`=?";
            PreparedStatement s = sql.getConnection().prepareStatement(statement);

            s.setString(1, uuid.toString());
            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return new DBResult(success, amount);
    }

    @Override
    public boolean insertGroupPermission(final int groupId, final String permission, final String world, final String server, final Date expires) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection()
                    .prepareStatement("INSERT INTO " + tblGroupPermissions + " SET `groupid`=?, `permission`=?, `world`=?, `server`=?" + (expires != null ? ", `expires`=?" : ""));
            s.setInt(1, groupId);
            s.setString(2, permission);
            s.setString(3, world);
            s.setString(4, server);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult deleteGroupPermission(final int groupId, final String permission, final String world, final String server, final Date expires) {
        boolean success = true;
        int amount = 0;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement(
                    "DELETE FROM " + tblGroupPermissions + " WHERE `groupid`=? AND `permission`=? AND `world`=? AND `server`=? AND " + (expires == null ? "`expires` is NULL" : "`expires`=?"));
            s.setInt(1, groupId);
            s.setString(2, permission);
            s.setString(3, world);
            s.setString(4, server);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return new DBResult(success, amount);
    }

    @Override
    public DBResult deleteGroupPermissions(final int groupId) {
        boolean success = true;
        int amount = 0;

        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupPermissions + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return new DBResult(success, amount);
    }

    @Override
    public boolean setPlayerPrefix(final UUID uuid, final String prefix) {
        boolean success = true;

        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `prefix`=? WHERE `uuid`=?");
            s.setString(1, prefix);
            s.setString(2, uuid.toString());
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean setPlayerSuffix(final UUID uuid, final String suffix) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `suffix`=? WHERE `uuid`=?");
            s.setString(1, suffix);
            s.setString(2, uuid.toString());
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean insertPlayerGroup(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection()
                    .prepareStatement("INSERT INTO " + tblPlayerGroups + " SET `playeruuid`=?, `groupid`=?, `server`=?, `negated`=?" + (expires != null ? ", `expires`=?" : ""));
            s.setString(1, uuid.toString());
            s.setInt(2, groupId);
            s.setString(3, server);
            s.setBoolean(4, negated);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deletePlayerGroup(final UUID uuid, final int groupId, final String server, final boolean negated, final Date expires) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement(
                    "DELETE FROM " + tblPlayerGroups + " WHERE `playeruuid`=? AND `groupid`=? AND `server`=? AND `negated`=? AND " + (expires == null ? "`expires` is NULL" : "`expires`=?"));
            s.setString(1, uuid.toString());
            s.setInt(2, groupId);
            s.setString(3, server);
            s.setBoolean(4, negated);
            if (expires != null)
                s.setString(5, getExpirationDateString(expires));
            plugin.debug(s.toString());
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deletePlayerGroups(final UUID uuid) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPlayerGroups + " WHERE `playeruuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getPlayerGroups(final UUID uuid) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayerGroups + " WHERE `playeruuid`=?");
            s.setString(1, uuid.toString());
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean deleteGroup(final int groupId) {
        boolean success = true;
        int amount = 0;

        plugin.getLogger().info("Deleting group " + groupId + "...");
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroups + " WHERE `id`=?;");
            s.setInt(1, groupId);
            amount = s.executeUpdate();
            if (amount <= 0)
                success = false;
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }

        plugin.getLogger().info("Deleting group permissions...");
        deleteGroupPermissions(groupId);
        plugin.getLogger().info("Deleting group parents...");
        deleteGroupParents(groupId);
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupParents + " WHERE `parentgroupid`=?");
            s.setInt(1, groupId);
            amount = s.executeUpdate();
            s.close();
            plugin.getLogger().info("Deleted " + amount + " parent references.");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().info("Could not delete parent references.");
        }
        plugin.getLogger().info("Deleting group prefixes...");
        deleteGroupPrefixes(groupId);
        plugin.getLogger().info("Deleting group suffixes...");
        deleteGroupSuffixes(groupId);
        plugin.getLogger().info("Deleting group references...");
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPlayerGroups + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            amount = s.executeUpdate();
            s.close();
            plugin.getLogger().info("Deleted " + amount + " group references.");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().info("Could not delete group references.");
        }
        plugin.getLogger().info("Done.");
        return success;
    }

    @Override
    public boolean insertGroupParent(final int groupId, final int parentGroupId) {
        boolean success = true;

        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroupParents + " SET `groupid`=?, `parentgroupid`=?");
            s.setInt(1, groupId);
            s.setInt(2, parentGroupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupParent(final int groupId, final int parentGroupId) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupParents + " WHERE `groupid`=? AND `parentgroupid`=?");
            s.setInt(1, groupId);
            s.setInt(2, parentGroupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupParents(final int groupId) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupParents + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getGroupParents(final int groupId) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupParents + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getGroupParents() {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupParents);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean insertGroupPrefix(final int groupId, final String prefix, final String server) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroupPrefixes + " SET `groupid`=?, `prefix`=?, `server`=?");
            s.setInt(1, groupId);
            s.setString(2, prefix);
            s.setString(3, server);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupPrefix(final int groupId, final String prefix, final String server) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupPrefixes + " WHERE `groupid`=? AND `prefix`=? AND `server`=?");
            s.setInt(1, groupId);
            s.setString(2, prefix);
            s.setString(3, server);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupPrefixes(final int groupId) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupPrefixes + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getGroupPrefixes(final int groupId) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupPrefixes + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getGroupPrefixes() {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupPrefixes);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean insertGroupSuffix(final int groupId, final String suffix, final String server) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroupSuffixes + " SET `groupid`=?, `suffix`=?, `server`=?");
            s.setInt(1, groupId);
            s.setString(2, suffix);
            s.setString(3, server);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupSuffix(final int groupId, final String suffix, final String server) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupSuffixes + " WHERE `groupid`=? AND `suffix`=? AND `server`=?");
            s.setInt(1, groupId);
            s.setString(2, suffix);
            s.setString(3, server);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean deleteGroupSuffixes(final int groupId) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroupSuffixes + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public DBResult getGroupSuffixes(final int groupId) {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupSuffixes + " WHERE `groupid`=?");
            s.setInt(1, groupId);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public DBResult getGroupSuffixes() {
        DBResult result;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroupSuffixes);
            s.execute();
            ResultSet r = s.getResultSet();
            result = fromResultSet(r);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            result = new DBResult(false);
        }
        return result;
    }

    @Override
    public boolean setGroupLadder(final int groupId, final String ladder) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `ladder`=? WHERE `id`=?");
            s.setString(1, ladder);
            s.setInt(2, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean setGroupRank(final int groupId, final int rank) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `rank`=? WHERE `id`=?");
            s.setInt(1, rank);
            s.setInt(2, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    @Override
    public boolean setGroupName(final int groupId, final String name) {
        boolean success = true;
        try {
            PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `name`=? WHERE `id`=?");
            s.setString(1, name);
            s.setInt(2, groupId);
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

}
