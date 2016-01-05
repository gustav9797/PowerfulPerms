package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Group {
    private int id;
    private String name;
    private ArrayList<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private ArrayList<Group> parents;

    // private String prefixRaw;
    // private String suffixRaw;
    private HashMap<String, String> serverPrefix = new HashMap<String, String>();
    private HashMap<String, String> serverSuffix = new HashMap<String, String>();

    public Group(int id, String name, ArrayList<PowerfulPermission> permissions, String prefixRaw, String suffixRaw) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
        // this.prefixRaw = prefixRaw;
        // this.suffixRaw = suffixRaw;
        this.serverPrefix = getPrefixSuffix(prefixRaw);
        this.serverSuffix = getPrefixSuffix(suffixRaw);
    }

    public static HashMap<String, String> getPrefixSuffix(String input) {
        HashMap<String, String> output = new HashMap<String, String>();
        String[] splitted = input.split(";;;;;;;;");
        for (String one : splitted) {
            String[] server_prefixSuffix = one.split("::::::::");
            if (server_prefixSuffix.length >= 2) {
                String server = server_prefixSuffix[0];
                String prefixSuffix = server_prefixSuffix[1];
                output.put(server, prefixSuffix);
            } else if (server_prefixSuffix.length >= 1)
                output.put("", server_prefixSuffix[0]);
        }
        return output;
    }

    public static String encodePrefixSuffix(HashMap<String, String> input) {
        String output = "";
        for (Entry<String, String> entry : input.entrySet()) {
            output += entry.getKey() + "::::::::" + entry.getValue() + ";;;;;;;;";
        }
        return output;
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

    public String getPrefix(String server) {
        String prefix = serverPrefix.get(server);
        if (prefix != null)
            return prefix;
        return serverPrefix.get("");
    }

    public String getSuffix(String server) {
        String suffix = serverSuffix.get(server);
        if (suffix != null)
            return suffix;
        return serverSuffix.get("");
    }

    public HashMap<String, String> getServerPrefix() {
        return this.serverPrefix;
    }

    public HashMap<String, String> getServerSuffix() {
        return this.serverSuffix;
    }

    public ArrayList<PowerfulPermission> getOwnPermissions() {
        return new ArrayList<PowerfulPermission>(permissions);
    }

    public ArrayList<PowerfulPermission> getPermissions() {
        ArrayList<PowerfulPermission> temp = new ArrayList<PowerfulPermission>();
        for (Group parent : this.parents) {
            temp.addAll(parent.getPermissions());
        }
        temp.addAll(permissions);
        return temp;
    }

    /*
     * public ArrayList<PowerfulPermission> getInheritedPermissions() { ArrayList<PowerfulPermission> temp = new ArrayList<PowerfulPermission>(permissions); for (Group parent : this.parents) {
     * temp.addAll(parent.getPermissions()); } return temp; }
     */

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
