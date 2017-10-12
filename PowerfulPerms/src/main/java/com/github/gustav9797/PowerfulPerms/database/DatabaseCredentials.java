package com.github.gustav9797.PowerfulPerms.database;

public class DatabaseCredentials {

    private String host = "";
    private String database = "";
    private int port;
    private String username = "";
    private String password = "";

    public DatabaseCredentials(String host, String database, int port, String username, String password) {
        this.host = host;
        this.database = database;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public String getDatabase() {
        return database;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
