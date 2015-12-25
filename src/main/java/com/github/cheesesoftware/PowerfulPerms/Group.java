package com.github.cheesesoftware.PowerfulPerms;

import java.util.ArrayList;

public class Group {
    private int id;
    private String name;
    private ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private ArrayList<Group> parents;
    private String prefix;
    private String suffix;

    public Group(int id, String name, ArrayList<PowerfulPermission> permissions, String prefix, String suffix) {
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

    public ArrayList<PowerfulPermission> getOwnPermissions() {
        return new ArrayList<PowerfulPermission>(permissions);
    }

    public ArrayList<PowerfulPermission> getPermissions() {
        ArrayList<PowerfulPermission> temp = new ArrayList<PowerfulPermission>();
        for (Group parent : this.parents) {
            temp.addAll(parent.getInheritedPermissions());
        }
        temp.addAll(permissions);
        return temp;
    }

    public ArrayList<PowerfulPermission> getInheritedPermissions() {
        ArrayList<PowerfulPermission> temp = new ArrayList<PowerfulPermission>(permissions);
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
