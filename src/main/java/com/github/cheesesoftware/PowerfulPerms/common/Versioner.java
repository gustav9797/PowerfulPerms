package com.github.cheesesoftware.PowerfulPerms.common;

public class Versioner {

    public static int getVersionNumber(String input) {
        String output = input.replace(".", "");
        try {
            int out = Integer.parseInt(output);
            return out;
        } catch (NumberFormatException e) {
        }
        return 0;
    }

}
