package com.github.cheesesoftware.PowerfulPerms.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPerms.SQL;
import com.github.cheesesoftware.PowerfulPerms.common.IScheduler;

public class MySQLDatabase extends Database {

    private SQL sql;

    public MySQLDatabase(IScheduler scheduler, SQL sql) {
        super(scheduler);
        this.sql = sql;
    }

    private DBResult fromResultSet(ResultSet r) throws SQLException {
        ArrayList<DBDocument> rows = new ArrayList<DBDocument>();
        ResultSetMetaData md = r.getMetaData();
        int columns = md.getColumnCount();
        while (r.next()) {
            Map<String, Object> row = new HashMap<String, Object>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), r.getObject(i));
            }
            rows.add(new DBDocument(row));
        }
        return new DBResult(rows);
    }

    @Override
    public void tableExists(final String table, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean exists = false;
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SHOW TABLES LIKE " + table);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    if (r.next())
                        exists = true;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                done.setResult(new DBResult(exists));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void createGroupsTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String groupsTable = "CREATE TABLE `"
                        + tblGroups
                        + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`name` varchar(255) NOT NULL,`parents` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`id`),UNIQUE KEY `id_UNIQUE` (`id`),UNIQUE KEY `name_UNIQUE` (`name`)) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8";
                try {
                    sql.getConnection().prepareStatement(groupsTable).execute();

                    // Insert one group "Guest"
                    sql.getConnection().prepareStatement("INSERT INTO `" + tblGroups + "` (`id`, `name`, `parents`, `prefix`, `suffix`) VALUES ('1', 'Guest', '', '[Guest]', ': ');").execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void createPlayersTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String playersTable = "CREATE TABLE `"
                        + tblPlayers
                        + "` (`uuid` varchar(36) NOT NULL DEFAULT '',`name` varchar(32) NOT NULL,`groups` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`name`,`uuid`),UNIQUE KEY `uuid_UNIQUE` (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
                try {
                    sql.getConnection().prepareStatement(playersTable).execute();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                    success = false;
                }

                // Insert [default] if not exists
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM players WHERE `name`=?");
                    s.setString(1, "[default]");
                    s.execute();
                    ResultSet result = s.getResultSet();
                    if (!result.next()) {
                        // Default player doesn't exist. Create it.
                        sql.getConnection().prepareStatement("INSERT INTO `" + tblPlayers + "` (`name`, `groups`, `prefix`, `suffix`) VALUES ('[default]', '1', '', '');").execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void createPermissionsTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String permissionsTable = "CREATE TABLE `"
                        + tblPermissions
                        + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`playeruuid` varchar(36) NOT NULL,`playername` varchar(45) NOT NULL,`groupname` varchar(255) NOT NULL,`permission` varchar(128) NOT NULL,`world` varchar(128) NOT NULL,`server` varchar(128) NOT NULL,PRIMARY KEY (`id`,`playeruuid`,`playername`,`groupname`),UNIQUE KEY `id_UNIQUE` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
                try {
                    sql.getConnection().prepareStatement(permissionsTable).execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void insertGroup(final String group, final String parents, final String prefix, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroups + " SET `name`=?, `parents`=?, `prefix`=?, `suffix`=?");
                    s.setString(1, group);
                    s.setString(2, parents);
                    s.setString(3, prefix);
                    s.setString(4, suffix);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void insertPlayer(final UUID uuid, final String name, final String groups, final String prefix, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.setString(3, groups);
                    s.setString(4, prefix);
                    s.setString(5, suffix);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getPlayer(final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `uuid`=?");
                    s.setString(1, uuid.toString());
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getPlayers(final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
                    s.setString(1, name);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setPlayerName(final UUID uuid, final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `name`=? WHERE `uuid`=?;");
                    s.setString(1, name);
                    s.setString(2, uuid.toString());
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setPlayerUUID(final String name, final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `uuid`=? WHERE `name`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getGroups(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroups);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getGroupPermissions(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `groupname`=?");
                    s.setString(1, group);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getPlayerPermissions(final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playeruuid`=?");
                    s.setString(1, uuid.toString());
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void getPlayerPermissions(final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playername`=?");
                    s.setString(1, name);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);

                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void playerHasPermission(final String name, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = false;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playername`=? AND `permission`=? AND `world`=? AND `server`=?");
                    s.setString(1, name);
                    s.setString(2, permission);
                    s.setString(3, world);
                    s.setString(4, server);
                    s.execute();
                    ResultSet result = s.getResultSet();
                    if (result.next()) {
                        success = true;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void insertPermission(final UUID uuid, final String name, final String group, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement(
                            "INSERT INTO " + tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
                    if (uuid != null)
                        s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.setString(3, group);
                    s.setString(4, permission);
                    s.setString(5, world);
                    s.setString(6, server);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void deletePlayerPermission(final String name, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    boolean useWorld = false;
                    boolean useServer = false;

                    String statement = "DELETE FROM `" + tblPermissions + "` WHERE `playername`=? AND `permission`=?";
                    if (!world.isEmpty() && !world.equalsIgnoreCase("ALL")) {
                        statement += ", `world`=?";
                        useWorld = true;
                    }
                    if (!server.isEmpty() && !server.equalsIgnoreCase("ALL")) {
                        statement += ", `server`=?";
                        useServer = true;
                    }
                    PreparedStatement s = sql.getConnection().prepareStatement(statement);

                    s.setString(1, name);
                    s.setString(2, permission);
                    if (useWorld)
                        s.setString(3, world);
                    if (useServer)
                        s.setString(4, server);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void deletePlayerPermissions(final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    String statement = "DELETE FROM `" + tblPermissions + "` WHERE `playername`=?";
                    PreparedStatement s = sql.getConnection().prepareStatement(statement);

                    s.setString(1, name);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void deleteGroupPermission(final String group, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=? AND `permission`=? AND `world`=? AND `server`=?");
                    s.setString(1, group);
                    s.setString(2, permission);
                    s.setString(3, world);
                    s.setString(4, server);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void deleteGroupPermissions(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=?");
                    s.setString(1, group);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setPlayerPrefix(final String name, final String prefix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `prefix`=? WHERE `name`=?");
                    s.setString(1, prefix);
                    s.setString(2, name);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setPlayerSuffix(final String name, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `suffix`=? WHERE `name`=?");
                    s.setString(1, suffix);
                    s.setString(2, name);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setPlayerGroups(final String name, final String groups, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `groups`=? WHERE `name`=?");
                    s.setString(1, groups);
                    s.setString(2, name);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void deleteGroup(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroups + " WHERE `name`=?;");
                    s.setString(1, group);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setGroupParents(final String group, final String parents, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `parents`=? WHERE `name`=?");
                    s.setString(1, parents);
                    s.setString(2, group);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setGroupPrefix(final String group, final String prefix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `prefix`=? WHERE `name`=?");
                    s.setString(1, prefix);
                    s.setString(2, group);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

    @Override
    public void setGroupSuffix(final String group, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `suffix`=? WHERE `name`=?");
                    s.setString(1, suffix);
                    s.setString(2, group);
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done);
            }
        });
    }

}
