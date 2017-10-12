package com.github.gustav9797.PowerfulPerms.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;

public class Util {

    public static LinkedHashMap<String, List<CachedGroup>> getPlayerGroups_old(String raw) {
        LinkedHashMap<String, List<CachedGroup>> tempGroups = new LinkedHashMap<>();
        for (String s : raw.split(";")) {
            // Each group entry
            String[] split = s.split(":");
            if (split.length >= 2) {
                String server = split[0];

                // If list null, initialize list
                List<CachedGroup> input = tempGroups.get(server);
                if (input == null)
                    input = new ArrayList<>();

                boolean negated = split[1].startsWith("-");
                if (negated)
                    split[1] = split[1].substring(1);

                int groupId = Integer.parseInt(split[1]);

                input.add(new CachedGroup(-1, groupId, negated, null));
                tempGroups.put(server, input);
            } else {
                if (!s.isEmpty()) {
                    // If list null, initialize list
                    List<CachedGroup> input = tempGroups.get("");
                    if (input == null)
                        input = new ArrayList<>();

                    input.add(new CachedGroup(-1, Integer.parseInt(s), false, null));
                    tempGroups.put("", input);
                }
            }
        }
        return tempGroups;
    }

    public static HashMap<String, String> getPrefixSuffix_old(String input) {
        HashMap<String, String> output = new HashMap<>();
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

    public static ArrayList<String> getGroupParents_old(String parentsString) {
        ArrayList<String> parents = new ArrayList<>();
        if (parentsString.contains(";")) {
            for (String s : parentsString.split(";")) {
                parents.add(s);
            }
        } else
            parents.add(parentsString);
        return parents;
    }
}
