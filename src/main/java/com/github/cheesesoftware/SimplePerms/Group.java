package com.github.cheesesoftware.SimplePerms;

import java.util.ArrayList;
import java.util.HashMap;

public class Group {
    private int id;
    private String name;
    private ArrayList<SimplePermission> permissions = new ArrayList<SimplePermission>();
    private ArrayList<Group> parents;
    private String prefix;
    private String suffix;

    public Group(int id, String name, ArrayList<SimplePermission> permissions, String prefix, String suffix) {
	this.id = id;
	this.name = name;
	this.permissions = permissions;
	this.prefix = prefix;
	this.suffix = suffix;
    }

    public int getId() {
	return this.id;
    }

    public String getName() {
	return this.name;
    }

    public ArrayList<Group> getParents() {
	return this.parents;
    }

    public String getPrefix() {
	return prefix;
    }

    public String getSuffix() {
	return suffix;
    }

    public ArrayList<SimplePermission> getOwnPermissions() {
	return new ArrayList<SimplePermission>(permissions);
    }

    public ArrayList<SimplePermission> getPermissions() {
	ArrayList<SimplePermission> temp = new ArrayList<SimplePermission>(permissions);
	temp.addAll(permissions);
	for (Group parent : this.parents) {
	    temp.addAll(parent.getInheritedPermissions());
	}
	return temp;
    }

    public ArrayList<SimplePermission> getInheritedPermissions() {
	ArrayList<SimplePermission> temp = new ArrayList<SimplePermission>(permissions);
	for (Group parent : this.parents) {
	    temp.addAll(parent.getPermissions());
	}
	return temp;
    }

    /*
     * public String getRawPermissions() { String raw = getRawOwnPermissions(); for (Group g : parents) raw += g.getRawPermissions(); return raw; }
     * 
     * public String getRawOwnPermissions() { String raw = ""; for (Map.Entry<String, SimplePermission> e : permissions.entrySet()) { raw += e.getKey(); if(!e.getValue().getWorld().isEmpty() &&
     * !e.getValue().getWorld().equalsIgnoreCase("ALL")) raw += ":" + e.getValue().getWorld(); if(!e.getValue().getWorld().isEmpty() && !e.getValue().getWorld().equalsIgnoreCase("ALL")) raw += ":" +
     * e.getValue().getWorld(); raw += ";"; } return raw; }
     */

    public String getRawOwnParents() {
	String raw = "";
	for (Group g : parents)
	    raw += g.getId() + ";";
	return raw;
    }

    public void setParents(ArrayList<Group> parents) {
	this.parents = parents;
    }
}
