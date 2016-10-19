package com.github.cheesesoftware.PowerfulPerms.database;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

public class MongoDBDatabase extends Database {

    private PowerfulPermsPlugin plugin;
    private MongoClient mongo;
    private String database;
    private DB db;

    private DBCollection groupParents;
    private DBCollection groupPermissions;
    private DBCollection groupPrefixes;
    private DBCollection groups;
    private DBCollection groupSuffixes;
    private DBCollection playerGroups;
    private DBCollection playerPermissions;
    private DBCollection players;

    public MongoDBDatabase(IScheduler scheduler, DatabaseCredentials cred, PowerfulPermsPlugin plugin, String tablePrefix) {
        super(scheduler, tablePrefix);
        this.plugin = plugin;
        this.database = cred.getDatabase();

        try {
            MongoCredential credential = MongoCredential.createCredential(cred.getUsername(), cred.getDatabase(), cred.getPassword().toCharArray());
            ServerAddress address;
            address = new ServerAddress(cred.getHost(), cred.getPort());
            mongo = new MongoClient(address, Arrays.asList(credential));
            db = mongo.getDB(database);
            groupParents = db.getCollection(tblGroupParents);
            groupPermissions = db.getCollection(tblGroupPermissions);
            groupPrefixes = db.getCollection(tblGroupPrefixes);
            groups = db.getCollection(tblGroups);
            groupSuffixes = db.getCollection(tblGroupSuffixes);
            playerGroups = db.getCollection(tblPlayerGroups);
            playerPermissions = db.getCollection(tblPlayerPermissions);
            players = db.getCollection(tblPlayers);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not connect to the database!");
            return;
        }
    }

    @Override
    public void applyPatches() {

    }

    @Override
    public boolean ping() {
        DBObject ping = new BasicDBObject("ping", "1");
        try {
            mongo.getDB("dbname").command(ping);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tableExists(String table) {
        return true;
    }

    @Override
    public void createTable(String tableName) {

    }

    @Override
    public void renameTable(String tableName, String newTableName) {

    }

    @Override
    public boolean insertGroup(int id, String group, String ladder, int rank) {
        BasicDBObject doc = new BasicDBObject("name", "MongoDB").append("type", "database").append("count", 1).append("info", new BasicDBObject("x", 203).append("y", 102));
        WriteResult result = groups.insert(doc);
        return false;
    }

    @Override
    public boolean insertPlayer(UUID uuid, String name, String prefix, String suffix) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getPlayer(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getPlayers(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getPlayersCaseInsensitive(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setPlayerName(UUID uuid, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setPlayerUUID(String name, UUID uuid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getGroupPermissions(int groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getGroupPermissions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getPlayerPermissions(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean playerHasPermission(UUID uuid, String permission, String world, String server, Date expires) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean insertPlayerPermission(UUID uuid, String permission, String world, String server, Date expires) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean insertGroupPermission(int groupId, String permission, String world, String server, Date expires) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult deletePlayerPermission(UUID uuid, String permission, String world, String server, Date expires) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult deletePlayerPermissions(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult deleteGroupPermission(int groupId, String permission, String world, String server, Date expires) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult deleteGroupPermissions(int groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setPlayerPrefix(UUID uuid, String prefix) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setPlayerSuffix(UUID uuid, String suffix) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean insertPlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deletePlayerGroup(UUID uuid, int groupId, String server, boolean negated, Date expires) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getPlayerGroups(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteGroup(int groupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean insertGroupParent(int groupId, int parentGroupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupParent(int groupId, int parentGroupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupParents(int groupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getGroupParents(int groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getGroupParents() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean insertGroupPrefix(int groupId, String prefix, String server) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupPrefix(int groupId, String prefix, String server) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupPrefixes(int groupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getGroupPrefixes(int groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getGroupPrefixes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean insertGroupSuffix(int groupId, String suffix, String server) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupSuffix(int groupId, String suffix, String server) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteGroupSuffixes(int groupId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public DBResult getGroupSuffixes(int groupId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBResult getGroupSuffixes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setGroupLadder(int groupId, String ladder) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setGroupRank(int groupId, int rank) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setGroupName(int groupId, String name) {
        // TODO Auto-generated method stub
        return false;
    }

}
