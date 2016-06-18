package com.github.cheesesoftware.PowerfulPerms.common;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    
    public static List<String> toList(String s, String seperator) {
        List<String> l = new ArrayList<>();
        String ls = "";
        for (int i = 0; i < (s.length() - seperator.length()) + 1; i++) {
            if (s.substring(i, i + seperator.length()).equalsIgnoreCase(seperator)) {
                l.add(ls);
                ls = "";
                i = i + seperator.length() - 1;
            } else {
                ls += s.substring(i, i + 1);
            }
        }
        if (ls.length() > 0) {
            l.add(ls);
        }
        return l;
    }
    
}
