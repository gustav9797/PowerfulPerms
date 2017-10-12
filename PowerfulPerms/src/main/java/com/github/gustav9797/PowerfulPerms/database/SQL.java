package com.github.gustav9797.PowerfulPerms.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQL {

    private Connection conn;

    private String username = "";
    private String password = "";

    private String connectionString = "";

    public SQL(String host, String database, int port, String username, String password) {
	
	this.username = username;
	this.password = password;

	this.connectionString = "jdbc:mysql://" + host + ":" + port + "/" + database;

	conn = openConnection();
    }

    public synchronized Connection getConnection() {
	try {
	    if (conn != null && (conn.isClosed() || !conn.isValid(1))) {
		try {
		    conn.close();
		} catch (SQLException e) {
		}
		conn = null;
	    }

	    if (conn == null) {
		conn = openConnection();
	    }
	    return conn;
	} catch (SQLException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    private synchronized Connection openConnection() {
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    return DriverManager.getConnection(connectionString, username, password);
	} catch (ClassNotFoundException | SQLException e) {
	    e.printStackTrace();
	}
        return null;
    }

}
