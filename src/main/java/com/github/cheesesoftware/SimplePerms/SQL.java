package com.github.cheesesoftware.SimplePerms;

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
	if (!(conn == null)) {
	    try {
		if (!conn.isClosed()) {

		} else {
		    openConnection();
		}
	    } catch (SQLException e) {
		e.printStackTrace();
	    }
	} else {
	    openConnection();
	}
    }

    public Connection getConnection() {
	try {
	    if (conn.equals(null)) {
		openConnection();
		return getConnection();
	    }
	    if (conn.isClosed()) {
		openConnection();
		return getConnection();
	    }
	} catch (SQLException e) {
	    e.printStackTrace();
	}
	return conn;
    }

    public void openConnection() {
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    conn = DriverManager.getConnection(connectionString, username, password);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (SQLException e) {
	    e.printStackTrace();
	}
    }

}
