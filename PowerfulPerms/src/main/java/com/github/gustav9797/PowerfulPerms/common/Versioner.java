package com.github.gustav9797.PowerfulPerms.common;

public class Versioner {

    public static int getVersionNumber(String input) {
        String output = input.replace(".", "");
        try {
            int out = Integer.parseInt(output);
            if (out < 10)
                out *= 10;
            if (out < 100)
                out *= 10;
            return out;
        } catch (NumberFormatException e) {
        }
        return 0;
    }

}
