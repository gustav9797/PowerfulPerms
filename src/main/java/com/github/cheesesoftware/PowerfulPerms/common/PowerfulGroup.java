package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;

public class PowerfulGroup implements Group {
    private int id;
    private String name;
    private List<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();
    private List<Group> parents;
    private String ladder;
    private int rank;

    private HashMap<String, String> serverPrefix = new HashMap<String, String>();
    private HashMap<String, String> serverSuffix = new HashMap<String, String>();

    public PowerfulGroup(int id, String name, List<PowerfulPermission> permissions, String prefixRaw, String suffixRaw, String ladder, int rank) {
        this.id = id;
        this.name = name;
        this.permissions = permissions;
        this.serverPrefix = getPrefixSuffix(prefixRaw);
        this.serverSuffix = getPrefixSuffix(suffixRaw);
        this.ladder = ladder;
        this.rank = rank;
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

    public static String encodeParents(List<Group> parents) {
        String raw = "";
        for (Group g : parents)
            raw += g.getId() + ";";
        return raw;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<Group> getParents() {
        return this.parents;
    }

    @Override
    public String getPrefix(String server) {
        String prefix = serverPrefix.get(server);
        if (prefix != null)
            return prefix;
        return serverPrefix.get("");
    }

    @Override
    public String getSuffix(String server) {
        String suffix = serverSuffix.get(server);
        if (suffix != null)
            return suffix;
        return serverSuffix.get("");
    }

    @Override
    public HashMap<String, String> getPrefixes() {
        return this.serverPrefix;
    }

    @Override
    public HashMap<String, String> getSuffixes() {
        return this.serverSuffix;
    }

    @Override
    public ArrayList<Permission> getOwnPermissions() {
        return new ArrayList<Permission>(permissions);
    }

    @Override
    public ArrayList<Permission> getPermissions() {
        ArrayList<Permission> temp = new ArrayList<Permission>();
        for (Group parent : this.parents) {
            temp.addAll(parent.getPermissions());
        }
        temp.addAll(permissions);
        return temp;
    }

    @Override
    public String getLadder() {
        return this.ladder;
    }

    @Override
    public int getRank() {
        return this.rank;
    }

    @Override
    public void setParents(List<Group> parents) {
        this.parents = parents;
    }

}
